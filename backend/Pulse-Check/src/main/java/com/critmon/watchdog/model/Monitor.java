package com.critmon.watchdog.model;

import java.time.Instant;

/**
 * Represents a registered device monitor.
 * Holds all state for a single device including its configuration,
 * current status, and timing metadata.
 */
public class Monitor {
    private final String id;
    private final int timeoutSeconds;
    private final String alertEmail;
    private MonitorStatus status;
    private final Instant registeredAt;
    private Instant lastHeartbeatAt;
    private Instant lastAlertFiredAt;

    public Monitor(String id, int timeoutSeconds, String alertEmail) {
        this.id = id;
        this.timeoutSeconds = timeoutSeconds;
        this.alertEmail = alertEmail;
        this.status = MonitorStatus.ACTIVE;
        this.registeredAt = Instant.now();
        this.lastHeartbeatAt = Instant.now();
    }

    public String getId() { return id; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public String getAlertEmail() { return alertEmail; }
    public MonitorStatus getStatus() { return status; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public Instant getLastAlertFiredAt() { return lastAlertFiredAt; }

    public void setStatus(MonitorStatus status) {
        this.status = status;
    }

    public void recordHeartbeat() {
        this.lastHeartbeatAt = Instant.now();
        this.status = MonitorStatus.ACTIVE;
    }

    public void recordAlert() {
        this.lastAlertFiredAt = Instant.now();
        this.status = MonitorStatus.DOWN;
    }
}