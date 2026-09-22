package com.capstone.ebookstore.service;

import com.capstone.ebookstore.dto.CartDto;
import com.capstone.ebookstore.dto.OrderDto;
import com.capstone.ebookstore.entity.*;
import com.capstone.ebookstore.exception.BadRequestException;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int GIFT_POINTS_PER_UNIT_SPENT = 1;  // 1 point per $ spent
    private static final BigDecimal GIFT_POINT_VALUE = new BigDecimal("0.01"); // $0.01 per point

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;

    @Transactional
    public OrderDto.OrderResponse placeOrder(String email, OrderDto.PlaceOrderRequest request) {
        User user = getUser(email);
        Cart cart = cartService.getRawCart(user.getId());
        if (cart == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        Order.PaymentMethod method = Order.PaymentMethod.valueOf(request.getPaymentMethod());

        BigDecimal subtotal = cart.getTotalAmount();
        BigDecimal discount = BigDecimal.ZERO;
        int pointsRedeemed = 0;

        // --- Bug #3 fix: recalculate pointsRedeemed AFTER capping discount at subtotal ---
        if (request.isUseGiftPoints() && request.getGiftPointsToRedeem() > 0) {
            int maxRedeemable = Math.min(request.getGiftPointsToRedeem(), user.getGiftPoints());
            discount = GIFT_POINT_VALUE.multiply(BigDecimal.valueOf(maxRedeemable));
            if (discount.compareTo(subtotal) > 0) {
                // Cap discount and recalculate only the points actually consumed
                discount = subtotal;
                pointsRedeemed = subtotal.divide(GIFT_POINT_VALUE, 0, RoundingMode.DOWN).intValue();
            } else {
                pointsRedeemed = maxRedeemable;
            }
        }
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        // --- Bug #2 fix: track earned points on the order so cancellation can reverse them ---
        int earned = total.setScale(0, RoundingMode.DOWN).intValue() * GIFT_POINTS_PER_UNIT_SPENT;

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(user)
                .deliveryAddress(address)
                .subtotal(subtotal)
                .giftPointsRedeemed(pointsRedeemed)
                .giftPointsEarned(earned)
                .discountAmount(discount)
                .totalAmount(total)
                .paymentMethod(method)
                .status(Order.OrderStatus.CONFIRMED)
                .placedAt(Instant.now())
                .build();

        // --- Bug #1 fix: decrement stock for each product; validate no oversell at checkout ---
        for (CartItem ci : cart.getItems()) {
            Product product = ci.getProduct();
            int newStock = product.getStockQuantity() - ci.getQuantity();
            if (newStock < 0) {
                throw new BadRequestException(
                        "Insufficient stock for '" + product.getTitle() + "'. " +
                        "Available: " + product.getStockQuantity() +
                        ", Requested: " + ci.getQuantity());
            }
            product.setStockQuantity(newStock);

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getUnitPrice())
                    .build();
            order.getItems().add(oi);
        }

        // Deduct redeemed points and credit earned ones (1 point per $ of order total)
        user.setGiftPoints(user.getGiftPoints() - pointsRedeemed + earned);
        userRepository.save(user);

        // Clear the cart
        cart.getItems().clear();

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.OrderResponse> getOrderHistory(String email) {
        User user = getUser(email);
        return orderRepository.findByUserIdOrderByPlacedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderResponse getOrder(String email, Long orderId) {
        User user = getUser(email);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional
    public OrderDto.OrderResponse cancelOrder(String email, Long orderId) {
        User user = getUser(email);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.isCancellable()) {
            throw new BadRequestException(
                    "Order cannot be cancelled. The 48-hour cancellation window has passed.");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());

        // --- Bug #2 fix: refund redeemed points AND reverse earned points on cancellation ---
        int pointAdjustment = order.getGiftPointsRedeemed() - order.getGiftPointsEarned();
        if (pointAdjustment != 0) {
            user.setGiftPoints(Math.max(0, user.getGiftPoints() + pointAdjustment));
            userRepository.save(user);
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public CartDto.CartResponse buyAgain(String email, Long orderId) {
        User user = getUser(email);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        for (OrderItem oi : order.getItems()) {
            CartDto.CartItemRequest req = new CartDto.CartItemRequest();
            req.setProductId(oi.getProduct().getId());
            req.setQuantity(oi.getQuantity());
            cartService.addItem(email, req);
        }
        return cartService.getOrCreateCart(email);
    }

    // ---- helpers ----

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private OrderDto.OrderResponse toResponse(Order o) {
        List<OrderDto.OrderItemResponse> items = o.getItems().stream()
                .map(i -> OrderDto.OrderItemResponse.builder()
                        .id(i.getId())
                        .product(productService.toResponse(i.getProduct()))
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getUnitPrice().multiply(
                                BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        return OrderDto.OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus().name())
                .items(items)
                .deliveryAddress(buildAddressResponse(o.getDeliveryAddress()))
                .subtotal(o.getSubtotal())
                .giftPointsRedeemed(o.getGiftPointsRedeemed())
                .giftPointsEarned(o.getGiftPointsEarned())
                .discountAmount(o.getDiscountAmount())
                .totalAmount(o.getTotalAmount())
                .paymentMethod(o.getPaymentMethod().name())
                .placedAt(o.getPlacedAt())
                .cancelledAt(o.getCancelledAt())
                .cancellableUntil(o.getCancellableUntil())
                .build();
    }

    private com.capstone.ebookstore.dto.AddressDto.AddressResponse buildAddressResponse(Address a) {
        return com.capstone.ebookstore.dto.AddressDto.AddressResponse.builder()
                .id(a.getId()).fullName(a.getFullName()).street(a.getStreet())
                .city(a.getCity()).state(a.getState()).postalCode(a.getPostalCode())
                .country(a.getCountry()).phoneNumber(a.getPhoneNumber())
                .build();
    }
}
