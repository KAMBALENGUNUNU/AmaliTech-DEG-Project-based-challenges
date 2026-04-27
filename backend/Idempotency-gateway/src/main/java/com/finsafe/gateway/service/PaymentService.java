package com.finsafe.gateway.service;

import com.finsafe.gateway.dto.PaymentRequest;
import com.finsafe.gateway.dto.PaymentResponse;
import org.springframework.stereotype.Service;

/**
 * Simulates an external payment provider with processing latency.
 */
@Service
public class PaymentService {

    /**
     * Simulates payment processing with a mandatory 2-second delay.
     */
    public PaymentResponse execute(PaymentRequest request) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new PaymentResponse("Charged " + request.amount() + " " + request.currency());
    }
}