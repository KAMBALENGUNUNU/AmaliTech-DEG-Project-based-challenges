package com.critmon.watchdog.dto;

import com.critmon.watchdog.model.Monitor;
import com.critmon.watchdog.model.MonitorStatus;

import java.time.Instant;

/**
 * Response DTO for monitor state. Derived from the Monitor domain model.
 */
public record MonitorResponse(
        String id,
        int timeoutSeconds,
        String alertEmail,
        MonitorStatus status,
        Instant registeredAt,
        Instant lastHeartbeatAt,
        Instant lastAlertFiredAt
) {
    public static MonitorResponse from(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getTimeoutSeconds(),
                monitor.getAlertEmail(),
                monitor.getStatus(),
                monitor.getRegisteredAt(),
                monitor.getLastHeartbeatAt(),
                monitor.getLastAlertFiredAt()
        );
    }
}