package com.finsafe.gateway.model;

import com.finsafe.gateway.dto.PaymentResponse;

/**
 * Wrapper to indicate if a payment response was retrieved from cache.
 */
public record IdempotencyResult(PaymentResponse response, boolean isCacheHit) {
}