package com.critmon.watchdog.controller;

import com.critmon.watchdog.dto.CreateMonitorRequest;
import com.critmon.watchdog.dto.MessageResponse;
import com.critmon.watchdog.dto.MonitorResponse;
import com.critmon.watchdog.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing all monitor management endpoints.
 */
@RestController
@RequestMapping("/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * Register a new device monitor.
     */
    @PostMapping
    public ResponseEntity<MonitorResponse> register(
            @Valid @RequestBody CreateMonitorRequest request) {
        MonitorResponse response = monitorService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Send a heartbeat to reset the countdown for a monitor.
     */
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<MonitorResponse> heartbeat(@PathVariable String id) {
        MonitorResponse response = monitorService.heartbeat(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Pause a monitor, stopping its countdown timer.
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<MonitorResponse> pause(@PathVariable String id) {
        MonitorResponse response = monitorService.pause(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current state of a single monitor.
     * Developer's Choice: observability endpoint.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getMonitor(@PathVariable String id) {
        MonitorResponse response = monitorService.getMonitor(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List all registered monitors and their current states.
     * Developer's Choice: observability endpoint.
     */
    @GetMapping
    public ResponseEntity<List<MonitorResponse>> getAllMonitors() {
        List<MonitorResponse> response = monitorService.getAllMonitors();
        return ResponseEntity.ok(response);
    }
}