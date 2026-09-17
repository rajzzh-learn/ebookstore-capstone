package com.capstone.ebookstore.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

public class CategoryDto {

    @Data
    public static class CategoryRequest {
        @NotBlank
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;
    }

    @Data
    @Builder
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String description;
    }
}
