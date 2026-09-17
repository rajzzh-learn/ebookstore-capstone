package com.capstone.ebookstore.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

public class AuthDto {

    @Data
    public static class RegisterRequest {
        @NotBlank @Size(max = 100)  private String firstName;
        @NotBlank @Size(max = 100)  private String lastName;
        @NotBlank @Email @Size(max = 255) private String email;
        @NotBlank @Size(min = 8, max = 100) private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email private String email;
        @NotBlank        private String password;
    }

    @Data
    @Builder
    public static class AuthResponse {
        private String token;
        private String tokenType;
        private Long userId;
        private String email;
    }

    @Data
    @Builder
    public static class UserProfileResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private int giftPoints;
    }

    @Data
    @Builder
    public static class GiftPointsResponse {
        private Long userId;
        private int balance;
        private BigDecimal monetaryValue;
    }

    @Data
    @Builder
    public static class PaymentRequest {
        private Long orderId;
        private String paymentMethod;
        private boolean useGiftPoints;
        private int giftPointsToRedeem;
    }

    @Data
    @Builder
    public static class PaymentConfirmation {
        private String paymentId;
        private Long orderId;
        private String orderNumber;
        private BigDecimal amountCharged;
        private int giftPointsEarned;
        private String paymentMethod;
        private Instant confirmedAt;
        private String message;
    }
}
