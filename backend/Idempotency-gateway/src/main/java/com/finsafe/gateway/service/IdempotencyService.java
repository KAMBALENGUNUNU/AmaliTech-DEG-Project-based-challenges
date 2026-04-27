package com.finsafe.gateway.service;

import com.finsafe.gateway.dto.PaymentRequest;
import com.finsafe.gateway.dto.PaymentResponse;
import com.finsafe.gateway.exception.IdempotencyConflictException;
import com.finsafe.gateway.model.IdempotencyRecord;
import com.finsafe.gateway.model.IdempotencyResult;
import com.finsafe.gateway.repository.InMemoryIdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates idempotency logic with enterprise-grade resilience.
 */
@Service
public class IdempotencyService {
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final long WAIT_TIMEOUT_SECONDS = 5;

    private final InMemoryIdempotencyRepository repository;
    private final PaymentService paymentService;

    public IdempotencyService(InMemoryIdempotencyRepository repository, PaymentService paymentService) {
        this.repository = repository;
        this.paymentService = paymentService;
    }

    public IdempotencyResult processPayment(String key, PaymentRequest request) {
        String requestHash = calculateHash(request);
        IdempotencyRecord newRecord = new IdempotencyRecord(requestHash);

        IdempotencyRecord existingRecord = repository.putIfAbsent(key, newRecord);

        if (existingRecord != null) {
            return handleExistingRecord(key, requestHash, existingRecord);
        }

        return executeNewPayment(key, request, newRecord);
    }

    private IdempotencyResult handleExistingRecord(String key, String requestHash, IdempotencyRecord existingRecord) {
        if (!existingRecord.getRequestHash().equals(requestHash)) {
            logger.warn("Payload mismatch for idempotency key: {}", key);
            throw new IdempotencyConflictException("Idempotency key already used for a different request body.");
        }

        try {
            logger.info("Duplicate request for key: {}. Waiting for in-flight processing...", key);
            boolean completed = existingRecord.awaitCompletion(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                logger.error("Timeout waiting for in-flight request: {}", key);
                throw new RuntimeException("Gateway timeout waiting for payment processing.");
            }

            if (existingRecord.getError() != null) {
                logger.warn("Replaying failure for key: {}", key);
                throw new RuntimeException("Previous attempt failed: " + existingRecord.getError().getMessage());
            }

            logger.info("Cache hit for key: {}", key);
            return new IdempotencyResult(existingRecord.getResponse(), true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operational error during request synchronization");
        }
    }

    private IdempotencyResult executeNewPayment(String key, PaymentRequest request, IdempotencyRecord record) {
        try {
            logger.info("Processing first-time request for key: {}", key);
            PaymentResponse response = paymentService.execute(request);
            record.completeWith(response);
            return new IdempotencyResult(response, false);
        } catch (Exception e) {
            logger.error("Payment processing failed for key: {}. Cleaning up record.", key, e);
            record.failWith(e);
            repository.remove(key);
            throw e;
        }
    }

    private String calculateHash(PaymentRequest request) {
        try {
            String raw = request.amount() + "|" + request.currency();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}