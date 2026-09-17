package com.capstone.ebookstore.controller;

import com.capstone.ebookstore.dto.AuthDto;
import com.capstone.ebookstore.entity.Order;
import com.capstone.ebookstore.entity.User;
import com.capstone.ebookstore.exception.BadRequestException;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.OrderRepository;
import com.capstone.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final BigDecimal GIFT_POINT_VALUE = new BigDecimal("0.01");

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<AuthDto.PaymentConfirmation> processPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AuthDto.PaymentRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findByIdAndUserId(request.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order is not in CONFIRMED state for payment");
        }

        // Mark order as PROCESSING after payment
        order.setStatus(Order.OrderStatus.PROCESSING);
        orderRepository.save(order);

        return ResponseEntity.ok(AuthDto.PaymentConfirmation.builder()
                .paymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .amountCharged(order.getTotalAmount())
                .giftPointsEarned(order.getTotalAmount().intValue())
                .paymentMethod(order.getPaymentMethod().name())
                .confirmedAt(Instant.now())
                .message("Payment successful! Your order " + order.getOrderNumber() +
                         " has been confirmed. Estimated delivery in " +
                         order.getItems().stream()
                             .mapToInt(i -> i.getProduct().getEstimatedDeliveryDays()).max()
                             .orElse(5) + " days.")
                .build());
    }

    @GetMapping("/gift-points")
    public ResponseEntity<AuthDto.GiftPointsResponse> getGiftPoints(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(AuthDto.GiftPointsResponse.builder()
                .userId(user.getId())
                .balance(user.getGiftPoints())
                .monetaryValue(GIFT_POINT_VALUE.multiply(BigDecimal.valueOf(user.getGiftPoints())))
                .build());
    }
}
