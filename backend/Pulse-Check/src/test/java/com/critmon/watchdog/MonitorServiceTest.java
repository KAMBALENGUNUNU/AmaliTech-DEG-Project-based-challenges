package com.critmon.watchdog;

import com.critmon.watchdog.dto.CreateMonitorRequest;
import com.critmon.watchdog.dto.MonitorResponse;
import com.critmon.watchdog.exception.InvalidMonitorStateException;
import com.critmon.watchdog.exception.MonitorAlreadyExistsException;
import com.critmon.watchdog.exception.MonitorNotFoundException;
import com.critmon.watchdog.model.MonitorStatus;
import com.critmon.watchdog.service.MonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MonitorServiceTest {

    @Autowired
    private MonitorService monitorService;

    private CreateMonitorRequest buildRequest(String id, int timeout) {
        return new CreateMonitorRequest(id, timeout, "admin@critmon.com");
    }

    @Test
    void testRegister_Success() {
        MonitorResponse response = monitorService.register(buildRequest("device-reg-1", 60));
        assertEquals("device-reg-1", response.id());
        assertEquals(MonitorStatus.ACTIVE, response.status());
    }

    @Test
    void testRegister_DuplicateId_ThrowsConflict() {
        monitorService.register(buildRequest("device-dup-1", 60));
        assertThrows(MonitorAlreadyExistsException.class,
                () -> monitorService.register(buildRequest("device-dup-1", 60)));
    }

    @Test
    void testHeartbeat_ResetsTimer() {
        monitorService.register(buildRequest("device-hb-1", 60));
        MonitorResponse response = monitorService.heartbeat("device-hb-1");
        assertEquals(MonitorStatus.ACTIVE, response.status());
        assertNotNull(response.lastHeartbeatAt());
    }

    @Test
    void testHeartbeat_UnknownId_ThrowsNotFound() {
        assertThrows(MonitorNotFoundException.class,
                () -> monitorService.heartbeat("device-unknown-999"));
    }

    @Test
    void testPause_ActiveMonitor_Success() {
        monitorService.register(buildRequest("device-pause-1", 60));
        MonitorResponse response = monitorService.pause("device-pause-1");
        assertEquals(MonitorStatus.PAUSED, response.status());
    }

    @Test
    void testPause_AlreadyPaused_ThrowsConflict() {
        monitorService.register(buildRequest("device-pause-2", 60));
        monitorService.pause("device-pause-2");
        assertThrows(InvalidMonitorStateException.class,
                () -> monitorService.pause("device-pause-2"));
    }

    @Test
    void testHeartbeat_ReactivatesPausedMonitor() {
        monitorService.register(buildRequest("device-pause-3", 60));
        monitorService.pause("device-pause-3");
        MonitorResponse response = monitorService.heartbeat("device-pause-3");
        assertEquals(MonitorStatus.ACTIVE, response.status());
    }

    @Test
    void testTimerExpiry_TransitionsToDown() throws InterruptedException {
        // Register with a 2-second timeout to keep the test fast
        monitorService.register(buildRequest("device-expiry-1", 2));
        TimeUnit.SECONDS.sleep(3);
        MonitorResponse response = monitorService.getMonitor("device-expiry-1");
        assertEquals(MonitorStatus.DOWN, response.status());
        assertNotNull(response.lastAlertFiredAt());
    }

    @Test
    void testHeartbeat_RecoverDownMonitor() throws InterruptedException {
        // Register with a 2-second timeout, let it expire, then recover
        monitorService.register(buildRequest("device-recover-1", 2));
        TimeUnit.SECONDS.sleep(3);
        MonitorResponse response = monitorService.heartbeat("device-recover-1");
        assertEquals(MonitorStatus.ACTIVE, response.status());
    }

    @Test
    void testGetAllMonitors_ReturnsAll() {
        monitorService.register(buildRequest("device-list-1", 60));
        monitorService.register(buildRequest("device-list-2", 60));
        assertTrue(monitorService.getAllMonitors().size() >= 2);
    }
}