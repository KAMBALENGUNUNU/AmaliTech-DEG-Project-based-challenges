package com.critmon.watchdog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides the shared ScheduledExecutorService used to manage
 * per-monitor countdown timers.
 *
 * Uses a thread pool of 10 so up to 10 timers can fire concurrently
 * without queuing behind each other.
 */
@Configuration
public class SchedulerConfig {

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(10);
    }
}