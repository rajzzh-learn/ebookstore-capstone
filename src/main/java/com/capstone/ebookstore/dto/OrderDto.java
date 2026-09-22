package com.capstone.ebookstore.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDto {

    @Data
    @Builder
    public static class PlaceOrderRequest {
        private Long addressId;
        private String paymentMethod;
        private boolean useGiftPoints;
        private int giftPointsToRedeem;
    }

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private ProductDto.ProductResponse product;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    public static class OrderResponse {
        private Long id;
        private String orderNumber;
        private String status;
        private List<OrderItemResponse> items;
        private AddressDto.AddressResponse deliveryAddress;
        private BigDecimal subtotal;
        private int giftPointsRedeemed;
        private int giftPointsEarned;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private String paymentMethod;
        private Instant placedAt;
        private Instant cancelledAt;
        private Instant cancellableUntil;
    }
}
