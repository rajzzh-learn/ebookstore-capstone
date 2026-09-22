package com.capstone.ebookstore.service;

import com.capstone.ebookstore.dto.OrderDto;
import com.capstone.ebookstore.entity.*;
import com.capstone.ebookstore.exception.BadRequestException;
import com.capstone.ebookstore.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the three critical bug fixes in OrderService:
 *
 *  Bug #1 – Product stock is never decremented when an order is placed
 *  Bug #2 – Earned gift points are not reversed on order cancellation
 *  Bug #3 – Excess gift points stolen when discount is capped at subtotal
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;
    @Mock private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    // ── shared test fixtures ────────────────────────────────────────────────

    private User user;
    private Address address;
    private Product product;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("buyer@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .password("hashed")
                .giftPoints(0)
                .build();

        address = Address.builder()
                .id(1L)
                .user(user)
                .fullName("Jane Doe")
                .street("123 Main St")
                .city("Springfield")
                .state("IL")
                .postalCode("62701")
                .country("US")
                .build();

        product = Product.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert Martin")
                .price(new BigDecimal("29.99"))
                .stockQuantity(10)
                .estimatedDeliveryDays(5)
                .category(Category.builder().id(1L).name("Tech").build())
                .brand(Brand.builder().id(1L).name("Publisher").build())
                .build();

        cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();
        cartItem.setCart(cart);
    }

    /** Helper: stub the common repositories for a successful placeOrder call */
    private void stubForPlaceOrder() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(cartService.getRawCart(1L)).thenReturn(cart);
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.getItems().forEach(i -> i.setId(99L)); // simulate DB id assignment
            return o;
        });
        when(productService.toResponse(any(Product.class))).thenReturn(null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug #1 — Stock decrement
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Bug #1 — Product stock is decremented on order placement")
    class StockDecrementTests {

        @Test
        @DisplayName("Stock decreases by the ordered quantity after a successful order")
        void stockDecrementedAfterOrder() {
            stubForPlaceOrder();
            int initialStock = product.getStockQuantity(); // 10
            int orderedQty   = cartItem.getQuantity();     // 2

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(false)
                    .giftPointsToRedeem(0)
                    .build();

            orderService.placeOrder("buyer@example.com", request);

            assertThat(product.getStockQuantity())
                    .as("Stock should be decremented by the ordered quantity")
                    .isEqualTo(initialStock - orderedQty); // 10 - 2 = 8
        }

        @Test
        @DisplayName("Order is rejected when cart quantity exceeds available stock")
        void oversellIsRejected() {
            // Cart wants 5 units but only 2 remain in stock
            cartItem.setQuantity(5);
            product.setStockQuantity(2);

            when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
            when(cartService.getRawCart(1L)).thenReturn(cart);
            when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(false)
                    .giftPointsToRedeem(0)
                    .build();

            assertThatThrownBy(() -> orderService.placeOrder("buyer@example.com", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Insufficient stock")
                    .hasMessageContaining("Clean Code");

            // Stock must not be changed when the order is rejected
            assertThat(product.getStockQuantity()).isEqualTo(2);
            // Order must never be saved
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Stock reaches exactly zero when all remaining units are ordered")
        void stockReachesZeroExactly() {
            cartItem.setQuantity(10); // buy all remaining stock
            product.setStockQuantity(10);

            stubForPlaceOrder();

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(false)
                    .giftPointsToRedeem(0)
                    .build();

            orderService.placeOrder("buyer@example.com", request);

            assertThat(product.getStockQuantity())
                    .as("Stock should be exactly 0 after buying all remaining units")
                    .isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug #2 — Earned gift points reversed on cancellation
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Bug #2 — Earned gift points are reversed on order cancellation")
    class GiftPointsEarnedReversalTests {

        private Order buildCancellableOrder(int pointsRedeemed, int pointsEarned) {
            return Order.builder()
                    .id(10L)
                    .orderNumber("ORD-TEST0001")
                    .user(user)
                    .deliveryAddress(address)
                    .subtotal(new BigDecimal("59.98"))
                    .giftPointsRedeemed(pointsRedeemed)
                    .giftPointsEarned(pointsEarned)
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("59.98"))
                    .paymentMethod(Order.PaymentMethod.CREDIT_CARD)
                    .status(Order.OrderStatus.CONFIRMED)
                    .placedAt(Instant.now())
                    .items(new ArrayList<>())
                    .build();
        }

        @Test
        @DisplayName("Cancelling an order without redemption deducts the earned points")
        void cancelOrderRevertsEarnedPoints() {
            // User had 0 points, earned 59 points from a $59.98 order
            user.setGiftPoints(59);
            Order order = buildCancellableOrder(0, 59);

            when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
            when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            orderService.cancelOrder("buyer@example.com", 10L);

            // Net adjustment: +0 redeemed - 59 earned = -59
            assertThat(user.getGiftPoints())
                    .as("Earned points should be reversed on cancellation (59 - 59 = 0)")
                    .isZero();
        }

        @Test
        @DisplayName("Cancelling an order with redemption nets the two correctly")
        void cancelOrderNetsRedeemedAndEarnedPoints() {
            // User redeemed 200 points and earned 45 points on a $45 order
            // After order: balance was X - 200 + 45
            // After cancel: should be restored by reversing: +200 redeemed - 45 earned = net +155
            user.setGiftPoints(345); // some balance after the order was placed
            Order order = buildCancellableOrder(200, 45);

            when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
            when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            orderService.cancelOrder("buyer@example.com", 10L);

            // Net adjustment: +200 redeemed - 45 earned = +155
            assertThat(user.getGiftPoints())
                    .as("Net point adjustment on cancel: +200 refunded - 45 reversed = +155")
                    .isEqualTo(345 + 200 - 45); // 500
        }

        @Test
        @DisplayName("Gift points earned are stored on the order at placement time")
        void earnedPointsStoredOnOrder() {
            // $59.98 order → floor($59.98) = 59 points earned
            when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
            when(cartService.getRawCart(1L)).thenReturn(cart);
            when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.getItems().forEach(i -> i.setId(99L));
                return o;
            });
            when(productService.toResponse(any(Product.class))).thenReturn(null);

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(false)
                    .giftPointsToRedeem(0)
                    .build();

            orderService.placeOrder("buyer@example.com", request);

            verify(orderRepository).save(argThat(o ->
                    o.getGiftPointsEarned() == 59 // floor(29.99 * 2) = floor(59.98) = 59
            ));
        }

        @Test
        @DisplayName("Point balance never goes below zero on cancellation")
        void pointBalanceNeverNegative() {
            // Edge case: user's balance is 5 but earned 59 points and redeemed 0
            user.setGiftPoints(5); // somehow balance is lower than expected
            Order order = buildCancellableOrder(0, 59);

            when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
            when(orderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            orderService.cancelOrder("buyer@example.com", 10L);

            assertThat(user.getGiftPoints())
                    .as("Gift point balance must never go below 0")
                    .isGreaterThanOrEqualTo(0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug #3 — Excess points not stolen when discount is capped
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Bug #3 — pointsRedeemed recalculated after discount capped at subtotal")
    class GiftPointsCapTests {

        @Test
        @DisplayName("User retains excess points when redemption value exceeds order total")
        void excessPointsNotStolen() {
            // Cart: 2 × $29.99 = $59.98
            // User has 10_000 points = $100 value, tries to redeem all 10_000
            // Discount should be capped at $59.98
            // Points actually consumed = $59.98 / $0.01 = 5998 points
            // Remaining points = 10_000 - 5998 = 4002  (NOT 0)
            user.setGiftPoints(10_000);

            stubForPlaceOrder();

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(true)
                    .giftPointsToRedeem(10_000)
                    .build();

            orderService.placeOrder("buyer@example.com", request);

            // Subtotal = $59.98 → pointsRedeemed = 5998, earned = 0 (total = $0.00)
            // Final balance = 10_000 - 5998 + 0 = 4002
            assertThat(user.getGiftPoints())
                    .as("User should retain the excess points not consumed by the capped discount")
                    .isEqualTo(4002);
        }

        @Test
        @DisplayName("Order total is $0 when points fully cover the subtotal")
        void orderTotalIsZeroWhenPointsCoverSubtotal() {
            user.setGiftPoints(10_000);

            when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
            when(cartService.getRawCart(1L)).thenReturn(cart);
            when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.getItems().forEach(i -> i.setId(99L));
                return o;
            });
            when(productService.toResponse(any(Product.class))).thenReturn(null);

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(true)
                    .giftPointsToRedeem(10_000)
                    .build();

            orderService.placeOrder("buyer@example.com", request);

            verify(orderRepository).save(argThat(o ->
                    o.getTotalAmount().compareTo(BigDecimal.ZERO) == 0
            ));
        }

        @Test
        @DisplayName("No excess points stolen when redemption is exactly equal to subtotal")
        void exactRedemptionNoExcessPointsStolen() {
            // Cart subtotal = $59.98 → exactly 5998 points = $59.98
            user.setGiftPoints(5998);

            stubForPlaceOrder();

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(true)
                    .giftPointsToRedeem(5998)
                    .build();

            orderService.placeOrder("buyer@example.com", request);

            // 5998 - 5998 + 0 = 0
            assertThat(user.getGiftPoints())
                    .as("All requested points are consumed when value exactly equals subtotal")
                    .isZero();
        }

        @Test
        @DisplayName("Partial redemption below subtotal works correctly — no capping needed")
        void partialRedemptionBelowSubtotal() {
            // Cart = $59.98, user redeems 1000 points = $10.00
            // Total = $49.98, earned = floor($49.98) = 49 points
            // Balance = 5000 - 1000 + 49 = 4049
            user.setGiftPoints(5000);

            stubForPlaceOrder();

            OrderDto.PlaceOrderRequest request = OrderDto.PlaceOrderRequest.builder()
                    .addressId(1L)
                    .paymentMethod("CREDIT_CARD")
                    .useGiftPoints(true)
                    .giftPointsToRedeem(1000)
                    .build();

            orderService.placeOrder("buyer@example.com", request);

            assertThat(user.getGiftPoints())
                    .as("Balance should be: 5000 - 1000 redeemed + 49 earned = 4049")
                    .isEqualTo(4049);
        }
    }
}
