package com.capstone.ebookstore.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

public class BrandDto {

    @Data
    public static class BrandRequest {
        @NotBlank
        @Size(max = 100)
        private String name;

        private String logoUrl;
    }

    @Data
    @Builder
    public static class BrandResponse {
        private Long id;
        private String name;
        private String logoUrl;
    }
}
