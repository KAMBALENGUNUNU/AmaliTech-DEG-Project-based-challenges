package com.finsafe.gateway.model;

import com.finsafe.gateway.dto.PaymentResponse;

/**
 * Represents a record of an idempotency request.
 */
public class IdempotencyRecord {
    private final String requestHash;
    private PaymentResponse response;
    private Exception error;
    private final long timestamp;

    public IdempotencyRecord(String requestHash) {
        this.requestHash = requestHash;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRequestHash() { return requestHash; }
    public PaymentResponse getResponse() { return response; }
    public Exception getError() { return error; }
    public long getTimestamp() { return timestamp; }

    public void completeWith(PaymentResponse response) {
        this.response = response;
    }

    public void failWith(Exception error) {
        this.error = error;
    }
}