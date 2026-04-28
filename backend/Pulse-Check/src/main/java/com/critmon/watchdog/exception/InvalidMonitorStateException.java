package com.critmon.watchdog.exception;

/**
 * Thrown when an operation is invalid for the monitor's current state.
 * For example: pausing a monitor that is already DOWN.
 */
public class InvalidMonitorStateException extends RuntimeException {
    public InvalidMonitorStateException(String message) {
        super(message);
    }
}