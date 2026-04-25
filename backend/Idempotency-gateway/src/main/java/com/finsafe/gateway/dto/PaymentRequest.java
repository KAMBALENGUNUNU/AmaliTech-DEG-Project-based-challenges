package com.finsafe.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object for payment requests.
 * Uses a record for immutability and automatic equals/hashCode.
 */
public record PaymentRequest(
        @Positive(message = "Amount must be positive") int amount,
        @NotBlank(message = "Currency is required") String currency) {
}