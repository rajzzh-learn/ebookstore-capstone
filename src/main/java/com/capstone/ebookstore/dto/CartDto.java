package com.capstone.ebookstore.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class CartDto {

    @Data
    public static class CartItemRequest {
        @NotNull  private Long productId;
        @Min(1)   private int quantity;
    }

    @Data
    public static class CartItemUpdateRequest {
        @Min(1) private int quantity;
    }

    @Data
    @Builder
    public static class CartItemResponse {
        private Long id;
        private ProductDto.ProductResponse product;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    public static class CartResponse {
        private Long id;
        private List<CartItemResponse> items;
        private BigDecimal totalAmount;
    }
}
