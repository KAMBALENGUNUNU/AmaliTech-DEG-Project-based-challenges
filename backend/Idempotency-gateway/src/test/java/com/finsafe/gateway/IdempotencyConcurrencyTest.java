package com.finsafe.gateway;

import com.finsafe.gateway.dto.PaymentRequest;
import com.finsafe.gateway.dto.PaymentResponse;
import com.finsafe.gateway.model.IdempotencyResult;
import com.finsafe.gateway.service.IdempotencyService;
import com.finsafe.gateway.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class IdempotencyConcurrencyTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @MockBean
    private PaymentService paymentService;

    @Test
    void testConcurrentRequests_SameKey_ExactlyOneProcessing() throws InterruptedException, ExecutionException {
        String key = "concurrent-test-key";
        PaymentRequest request = new PaymentRequest(100, "GHS");

        when(paymentService.execute(any())).thenAnswer(invocation -> {
            Thread.sleep(1000);
            return new PaymentResponse("Charged 100 GHS");
        });

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<IdempotencyResult>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> idempotencyService.processPayment(key, request));
        }

        List<Future<IdempotencyResult>> futures = executor.invokeAll(tasks);

        int cacheHits = 0;
        for (Future<IdempotencyResult> future : futures) {
            IdempotencyResult result = future.get();
            assertEquals("Charged 100 GHS", result.response().status());
            if (result.isCacheHit()) cacheHits++;
        }

        // Exactly 9 out of 10 should be cache hits; 1 was the original
        assertEquals(9, cacheHits);
        verify(paymentService, times(1)).execute(any());
        executor.shutdown();
    }

    @Test
    void testConcurrentRequests_SameKey_FailureHandling() throws InterruptedException {
        String key = "failure-test-key";
        PaymentRequest request = new PaymentRequest(200, "GHS");

        when(paymentService.execute(any())).thenThrow(new RuntimeException("Payment Provider Down"));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<String> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    idempotencyService.processPayment(key, request);
                    results.add("Success");
                } catch (Exception e) {
                    results.add(e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(threadCount, results.size());
        assertTrue(results.stream().allMatch(r -> r != null && r.contains("Payment Provider Down")));
        verify(paymentService, times(1)).execute(any());

        executor.shutdown();
    }
}