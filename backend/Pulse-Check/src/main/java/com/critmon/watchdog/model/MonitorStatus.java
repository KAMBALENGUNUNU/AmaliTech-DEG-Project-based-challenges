package com.critmon.watchdog.model;

/**
 * Represents the lifecycle states of a monitor.
 *
 * ACTIVE  — timer is running, heartbeats are expected.
 * PAUSED  — timer is stopped, no alerts will fire.
 * DOWN    — timer expired without a heartbeat, alert has been fired.
 */
public enum MonitorStatus {
    ACTIVE,
    PAUSED,
    DOWN
}