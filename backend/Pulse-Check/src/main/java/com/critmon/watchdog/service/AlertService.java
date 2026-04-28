package com.critmon.watchdog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Responsible for firing alerts when a monitor expires.
 *
 * In production this would integrate with an email provider (SendGrid, SES)
 * or a webhook. For this implementation it logs a structured JSON alert
 * to simulate the notification.
 */
@Service
public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    /**
     * Fires an alert for the given device ID and notifies the registered email.
     */
    public void fireAlert(String deviceId, String alertEmail) {
        String alertJson = String.format(
                "{\"ALERT\": \"Device %s is down!\", \"alert_email\": \"%s\", \"time\": \"%s\"}",
                deviceId, alertEmail, Instant.now()
        );
        logger.error("DEAD MAN'S SWITCH TRIGGERED: {}", alertJson);
    }
}