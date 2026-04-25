package com.finsafe.gateway.repository;

import com.finsafe.gateway.model.IdempotencyRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory repository for idempotency records.
 */
@Repository
public class InMemoryIdempotencyRepository {
    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    /**
     * Atomically adds a record only if the key is not already present.
     * @return the existing record if key exists, or null if the new record was inserted.
     */
    public IdempotencyRecord putIfAbsent(String key, IdempotencyRecord record) {
        return store.putIfAbsent(key, record);
    }

    /**
     * Removes a record by key.
     */
    public void remove(String key) {
        store.remove(key);
    }

    /**
     * Removes all records older than the specified timestamp threshold.
     */
    public void removeOlderThan(long thresholdMillis) {
        store.entrySet().removeIf(entry -> entry.getValue().getTimestamp() < thresholdMillis);
    }
}