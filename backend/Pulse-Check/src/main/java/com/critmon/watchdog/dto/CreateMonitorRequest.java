package com.critmon.watchdog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for registering a new monitor.
 */
public record CreateMonitorRequest(
        @NotBlank(message = "Device ID is required")
        String id,

        @Positive(message = "Timeout must be a positive number of seconds")
        int timeout,

        @NotBlank(message = "Alert email is required")
        @Email(message = "Alert email must be a valid email address")
        String alert_email
) {}