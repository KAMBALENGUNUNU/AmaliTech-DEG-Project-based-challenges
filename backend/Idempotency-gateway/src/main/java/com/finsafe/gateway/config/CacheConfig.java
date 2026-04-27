package com.finsafe.gateway.config;

import com.finsafe.gateway.repository.InMemoryIdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for background tasks and caching.
 * Implements the "Developer's Choice" requirement: TTL Eviction.
 */
@Configuration
@EnableScheduling
public class CacheConfig {
    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    private final InMemoryIdempotencyRepository repository;

    public CacheConfig(InMemoryIdempotencyRepository repository) {
        this.repository = repository;
    }

    /**
     * Runs every hour. Evicts records older than 24 hours.
     */
    @Scheduled(fixedRate = 3600000)
    public void evictOldRecords() {
        long threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        repository.removeOlderThan(threshold);
        logger.info("Executed TTL eviction sweep on idempotency cache.");
    }
}