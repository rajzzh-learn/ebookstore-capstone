package com.capstone.ebookstore.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

public class ProductDto {

    @Data
    public static class ProductRequest {
        @NotBlank
        @Size(max = 500)
        private String title;

        @NotBlank
        @Size(max = 255)
        private String author;

        private String description;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal price;

        @Min(0)
        private int stockQuantity;

        private String imageUrl;

        @Min(1)
        private int estimatedDeliveryDays = 5;

        @NotNull
        private Long categoryId;

        @NotNull
        private Long brandId;
    }

    @Data
    @Builder
    public static class ProductResponse {
        private Long id;
        private String title;
        private String author;
        private String description;
        private BigDecimal price;
        private int stockQuantity;
        private String imageUrl;
        private int estimatedDeliveryDays;
        private CategoryDto.CategoryResponse category;
        private BrandDto.BrandResponse brand;
    }

    @Data
    @Builder
    public static class ProductPage {
        private java.util.List<ProductResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
