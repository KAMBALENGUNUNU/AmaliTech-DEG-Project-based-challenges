package com.critmon.watchdog.exception;

/**
 * Thrown when a monitor ID does not exist in the store.
 */
public class MonitorNotFoundException extends RuntimeException {
    public MonitorNotFoundException(String id) {
        super("Monitor not found: " + id);
    }
}