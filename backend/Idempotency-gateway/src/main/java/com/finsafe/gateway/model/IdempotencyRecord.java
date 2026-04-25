package com.finsafe.gateway.model;

import com.finsafe.gateway.dto.PaymentResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents a record of an idempotency request.
 * Contains the hash of the request payload, the saved response,
 * and a CountDownLatch for managing "in-flight" requests.
 */
public class IdempotencyRecord {
    private final String requestHash;
    private PaymentResponse response;
    private Exception error;
    private final CountDownLatch latch;
    private final long timestamp;

    public IdempotencyRecord(String requestHash) {
        this.requestHash = requestHash;
        this.latch = new CountDownLatch(1);
        this.timestamp = System.currentTimeMillis();
    }

    public String getRequestHash() { return requestHash; }
    public PaymentResponse getResponse() { return response; }
    public Exception getError() { return error; }
    public long getTimestamp() { return timestamp; }

    /**
     * Completes the record with a response and releases all waiting threads.
     */
    public void completeWith(PaymentResponse response) {
        this.response = response;
        this.latch.countDown();
    }

    /**
     * Fails the record with an error and releases all waiting threads.
     */
    public void failWith(Exception error) {
        this.error = error;
        this.latch.countDown();
    }

    /**
     * Blocks the current thread until the record is completed or timeout is reached.
     * @return true if completed in time, false if timed out.
     */
    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        return this.latch.await(timeout, unit);
    }
}