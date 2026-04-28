package com.critmon.watchdog.service;

import com.critmon.watchdog.dto.CreateMonitorRequest;
import com.critmon.watchdog.dto.MonitorResponse;
import com.critmon.watchdog.exception.InvalidMonitorStateException;
import com.critmon.watchdog.exception.MonitorAlreadyExistsException;
import com.critmon.watchdog.exception.MonitorNotFoundException;
import com.critmon.watchdog.model.Monitor;
import com.critmon.watchdog.model.MonitorStatus;
import com.critmon.watchdog.repository.InMemoryMonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Core service that orchestrates monitor lifecycle:
 * registration, heartbeat, pause, and expiry.
 *
 * Each monitor gets its own ScheduledFuture — a cancellable
 * countdown task stored alongside the monitor data.
 */
@Service
public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);

    private final InMemoryMonitorRepository repository;
    private final ScheduledExecutorService scheduler;
    private final AlertService alertService;

    // Holds the active countdown task for each monitor ID.
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public MonitorService(InMemoryMonitorRepository repository,
                          ScheduledExecutorService scheduler,
                          AlertService alertService) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.alertService = alertService;
    }

    /**
     * Registers a new monitor and starts its countdown timer.
     */
    public MonitorResponse register(CreateMonitorRequest request) {
        if (repository.existsById(request.id())) {
            throw new MonitorAlreadyExistsException(request.id());
        }

        Monitor monitor = new Monitor(request.id(), request.timeout(), request.alert_email());
        repository.save(monitor);
        scheduleExpiry(monitor);

        logger.info("Monitor registered: {} with timeout {}s", monitor.getId(), monitor.getTimeoutSeconds());
        return MonitorResponse.from(monitor);
    }

    /**
     * Resets the countdown for an existing monitor.
     * If the monitor was PAUSED, it is reactivated.
     * If the monitor was DOWN, it is recovered and restarted.
     */
    public MonitorResponse heartbeat(String id) {
        Monitor monitor = repository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));

        cancelExistingTask(id);
        monitor.recordHeartbeat();
        scheduleExpiry(monitor);

        logger.info("Heartbeat received for monitor: {}. Timer reset to {}s.", id, monitor.getTimeoutSeconds());
        return MonitorResponse.from(monitor);
    }

    /**
     * Pauses a monitor, stopping its countdown timer.
     * Only ACTIVE monitors can be paused.
     */
    public MonitorResponse pause(String id) {
        Monitor monitor = repository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));

        if (monitor.getStatus() == MonitorStatus.DOWN) {
            throw new InvalidMonitorStateException(
                    "Cannot pause monitor " + id + " because it is already DOWN.");
        }

        if (monitor.getStatus() == MonitorStatus.PAUSED) {
            throw new InvalidMonitorStateException(
                    "Monitor " + id + " is already PAUSED.");
        }

        cancelExistingTask(id);
        monitor.setStatus(MonitorStatus.PAUSED);

        logger.info("Monitor paused: {}", id);
        return MonitorResponse.from(monitor);
    }

    /**
     * Returns the current state of a single monitor.
     */
    public MonitorResponse getMonitor(String id) {
        Monitor monitor = repository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
        return MonitorResponse.from(monitor);
    }

    /**
     * Returns all registered monitors and their current states.
     */
    public List<MonitorResponse> getAllMonitors() {
        return repository.findAll().stream()
                .map(MonitorResponse::from)
                .toList();
    }

    /**
     * Schedules a countdown task for the monitor.
     * When it fires, it marks the monitor as DOWN and triggers the alert.
     */
    private void scheduleExpiry(Monitor monitor) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            // Re-fetch to get the latest state at time of expiry
            repository.findById(monitor.getId()).ifPresent(m -> {
                if (m.getStatus() == MonitorStatus.ACTIVE) {
                    m.recordAlert();
                    alertService.fireAlert(m.getId(), m.getAlertEmail());
                    logger.warn("Monitor EXPIRED: {}", m.getId());
                }
            });
        }, monitor.getTimeoutSeconds(), TimeUnit.SECONDS);

        scheduledTasks.put(monitor.getId(), future);
    }

    /**
     * Cancels the existing countdown task for a monitor, if one exists.
     */
    private void cancelExistingTask(String id) {
        ScheduledFuture<?> existing = scheduledTasks.remove(id);
        if (existing != null) {
            existing.cancel(false);
        }
    }
}