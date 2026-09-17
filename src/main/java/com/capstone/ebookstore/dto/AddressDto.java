package com.capstone.ebookstore.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

public class AddressDto {

    @Data
    public static class AddressRequest {
        @NotBlank @Size(max = 200) private String fullName;
        @NotBlank @Size(max = 500) private String street;
        @NotBlank @Size(max = 100) private String city;
        @NotBlank @Size(max = 100) private String state;
        @NotBlank @Size(max = 20)  private String postalCode;
        @NotBlank @Size(max = 100) private String country;
        @Size(max = 20)            private String phoneNumber;
    }

    @Data
    @Builder
    public static class AddressResponse {
        private Long id;
        private String fullName;
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phoneNumber;
    }
}
