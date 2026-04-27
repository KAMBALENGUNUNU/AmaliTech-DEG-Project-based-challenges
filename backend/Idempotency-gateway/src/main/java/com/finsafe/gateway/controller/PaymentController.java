package com.finsafe.gateway.controller;

import com.finsafe.gateway.dto.PaymentRequest;
import com.finsafe.gateway.dto.PaymentResponse;
import com.finsafe.gateway.model.IdempotencyResult;
import com.finsafe.gateway.service.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API Controller for payment processing.
 */
@RestController
public class PaymentController {

    private final IdempotencyService idempotencyService;

    public PaymentController(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    /**
     * Processes a payment with idempotency guarantees.
     * Returns 201 Created for new payments, 200 OK for replayed (cached) responses.
     */
    @PostMapping("/process-payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        IdempotencyResult result = idempotencyService.processPayment(idempotencyKey, request);

        HttpHeaders headers = new HttpHeaders();
        if (result.isCacheHit()) {
            headers.add("X-Cache-Hit", "true");
            return new ResponseEntity<>(result.response(), headers, HttpStatus.OK);
        }

        return new ResponseEntity<>(result.response(), headers, HttpStatus.CREATED);
    }
}