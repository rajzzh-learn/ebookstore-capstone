package com.capstone.ebookstore.controller;

import com.capstone.ebookstore.dto.CartDto;
import com.capstone.ebookstore.dto.OrderDto;
import com.capstone.ebookstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDto.OrderResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getOrderHistory(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<OrderDto.OrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderDto.PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userDetails.getUsername(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.OrderResponse> get(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(userDetails.getUsername(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDto.OrderResponse> cancel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(userDetails.getUsername(), id));
    }

    @PostMapping("/{id}/buy-again")
    public ResponseEntity<CartDto.CartResponse> buyAgain(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.buyAgain(userDetails.getUsername(), id));
    }
}
