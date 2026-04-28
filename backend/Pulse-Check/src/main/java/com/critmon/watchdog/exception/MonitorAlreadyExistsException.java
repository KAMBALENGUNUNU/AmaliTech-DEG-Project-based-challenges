package com.critmon.watchdog.exception;

/**
 * Thrown when attempting to register a monitor ID that already exists.
 */
public class MonitorAlreadyExistsException extends RuntimeException {
    public MonitorAlreadyExistsException(String id) {
        super("Monitor already exists: " + id);
    }
}