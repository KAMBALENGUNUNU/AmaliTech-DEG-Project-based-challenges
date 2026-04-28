# Pulse-Check API ("Watchdog" Sentinel)

A **Dead Man's Switch API** for CritMon Servers Inc. Remote devices register a monitor with a countdown timer. If the device fails to send a heartbeat before the timer expires, the system automatically fires an alert — no human log-checking required.



##  Architecture & Logic Flow

![Sequence Diagram](image/Pulse-Check-diagram.png)

### Monitor State Machine

| Current State | Event | Next State | Notes |
| :--- | :--- | :--- | :--- |
| **ACTIVE** | `heartbeat` | **ACTIVE** | Timer reset |
| **ACTIVE** | `timeout` | **DOWN** | Alert fired |
| **ACTIVE** | `pause` | **PAUSED** | Timer cancelled |
| **PAUSED** | `heartbeat` | **ACTIVE** | Timer restarted |
| **DOWN** | `heartbeat` | **ACTIVE** | Recovery — timer restarted |
| **DOWN** | `pause` | **409 Conflict** | Invalid transition |
| **PAUSED** | `pause` | **409 Conflict** | Invalid transition |

### Diagram Participants

| Participant | Role |
|---|---|
| **Device / Client** | Remote sensor or e-commerce system sending API requests |
| **MonitorController** | REST layer — validates input and routes to the service |
| **MonitorService** | Core orchestrator — owns all state transitions and timer management |
| **ScheduledExecutorService** | JDK thread pool — holds one cancellable `ScheduledFuture` per monitor |
| **InMemoryMonitorRepository** | `ConcurrentHashMap`-backed store for all `Monitor` records |
| **AlertService** | Fires structured alerts when a monitor expires |

### Monitor State Machine


### How to Read the Diagram

**Scenario 1 — Registration (Happy Path)**
A device calls `POST /monitors`. The service creates a `Monitor` record with status `ACTIVE`, saves it to the repository, and schedules a `ScheduledFuture` to fire after the configured timeout. Returns `201 Created`.

**Scenario 2 — Heartbeat (Timer Reset)**
The device calls `POST /monitors/{id}/heartbeat`. The existing `ScheduledFuture` is cancelled, the monitor's `lastHeartbeatAt` is updated and status set back to `ACTIVE`, and a fresh `ScheduledFuture` is scheduled. Works from both `ACTIVE` and `PAUSED` states (recovery from `DOWN` also supported).

**Scenario 3 — Timer Expiry (Alert)**
No heartbeat arrives before the `ScheduledFuture` fires. The task checks the monitor status — if still `ACTIVE`, it calls `monitor.recordAlert()` (sets status to `DOWN`) and `alertService.fireAlert()` which logs a structured JSON alert to the console simulating an email notification.

**Scenario 4 — Pause**
The device calls `POST /monitors/{id}/pause`. The existing `ScheduledFuture` is cancelled and the status is set to `PAUSED`. No alert will fire. A subsequent heartbeat call reactivates the monitor and restarts the timer from the full duration.

**Scenario 5 — Duplicate Registration**
Attempting to register an ID that already exists returns `409 Conflict` immediately. No timer is created.

### Key Technical Mechanisms

| Mechanism | Purpose |
|---|---|
| `ScheduledExecutorService` (pool of 10) | Manages independent per-monitor countdown timers concurrently |
| `ScheduledFuture` per monitor | Cancellable handle to each timer — cancelled on heartbeat or pause |
| `ConcurrentHashMap` for task store | Thread-safe map of `id → ScheduledFuture` alongside the monitor store |
| Status re-check inside expiry task | Prevents stale timers from incorrectly marking a paused monitor as DOWN |

---

##  Setup & Execution

### Prerequisites
- Java 17+
- Maven 3.8+

### Run the Application
```bash
mvn spring-boot:run
```
Server starts on `http://localhost:8080`.

### Run Tests
```bash
mvn test
```

---

##  API Documentation

### Endpoints Overview

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/monitors` | Register a new device monitor |
| `POST` | `/monitors/{id}/heartbeat` | Reset the countdown timer |
| `POST` | `/monitors/{id}/pause` | Pause the countdown timer |
| `GET` | `/monitors/{id}` | Get current state of a monitor |
| `GET` | `/monitors` | List all monitors and their states |

---

### `POST /monitors` — Register a Monitor

**Request Body**

| Field | Type | Validation |
|---|---|---|
| `id` | string | Required, must be unique |
| `timeout` | integer | Required, must be positive (seconds) |
| `alert_email` | string | Required, must be a valid email |

**Example Request**
```bash
curl -X POST http://localhost:8080/monitors \
  -H "Content-Type: application/json" \
  -d '{"id": "device-123", "timeout": 60, "alert_email": "admin@critmon.com"}'
```

**Response — `201 Created`**
```json
{
  "id": "device-123",
  "timeoutSeconds": 60,
  "alertEmail": "admin@critmon.com",
  "status": "ACTIVE",
  "registeredAt": "2026-04-28T10:00:00Z",
  "lastHeartbeatAt": "2026-04-28T10:00:00Z",
  "lastAlertFiredAt": null
}
```

---

### `POST /monitors/{id}/heartbeat` — Send Heartbeat

**Example Request**
```bash
curl -X POST http://localhost:8080/monitors/device-123/heartbeat
```

**Response — `200 OK`**
```json
{
  "id": "device-123",
  "status": "ACTIVE",
  "lastHeartbeatAt": "2026-04-28T10:00:45Z"
}
```

**Error — `404 Not Found`**
```json
{"error": "Monitor not found: device-123"}
```

---

### `POST /monitors/{id}/pause` — Pause a Monitor

**Example Request**
```bash
curl -X POST http://localhost:8080/monitors/device-123/pause
```

**Response — `200 OK`**
```json
{
  "id": "device-123",
  "status": "PAUSED"
}
```

**Error — `409 Conflict`** (if already PAUSED or DOWN)
```json
{"error": "Cannot pause monitor device-123 because it is already DOWN."}
```

---

### `GET /monitors/{id}` — Get Monitor State

**Example Request**
```bash
curl http://localhost:8080/monitors/device-123
```

**Response — `200 OK`**
```json
{
  "id": "device-123",
  "status": "ACTIVE",
  "timeoutSeconds": 60,
  "lastHeartbeatAt": "2026-04-28T10:00:45Z",
  "lastAlertFiredAt": null
}
```

---

### `GET /monitors` — List All Monitors

**Example Request**
```bash
curl http://localhost:8080/monitors
```

**Response — `200 OK`**
```json
[
  {"id": "device-123", "status": "ACTIVE", ...},
  {"id": "device-456", "status": "DOWN", ...}
]
```

---

### Response Status Reference

| Scenario | Status Code |
|---|---|
| Monitor registered | `201 Created` |
| Heartbeat accepted / Pause accepted | `200 OK` |
| Monitor ID already exists | `409 Conflict` |
| Invalid state transition | `409 Conflict` |
| Monitor ID not found | `404 Not Found` |
| Validation error | `400 Bad Request` |

---

##  Design Decisions

- **`ScheduledFuture` per monitor**: Rather than a global polling loop that checks all monitors every second, each monitor gets its own cancellable timer task. This scales cleanly — adding 1000 monitors does not increase polling overhead.

- **Status re-check inside expiry task**: The expiry task re-fetches the monitor from the repository and checks its status before firing. This prevents a race condition where a heartbeat or pause arrives just as the timer fires — a stale `ScheduledFuture` cannot incorrectly mark a paused monitor as DOWN.

- **Recovery from DOWN via heartbeat**: The spec does not define this behaviour. Treating a heartbeat on a DOWN monitor as a recovery signal is the most operationally useful decision — it allows devices to self-recover without requiring manual re-registration.

- **Thread pool of 10**: `Executors.newScheduledThreadPool(10)` allows up to 10 alert tasks to fire simultaneously. For a small monitoring service this is sufficient. In production this would be backed by a distributed scheduler.

---

## 🛠 The Developer's Choice: Observability Endpoints

**Feature Added**: `GET /monitors/{id}` and `GET /monitors`.

**Rationale**: The original spec provides only write operations — register, heartbeat, pause. Without a read endpoint, the system is completely unobservable. A support engineer cannot query device state without reading raw logs. These two endpoints allow any operator to instantly see the status of one or all devices, making the system production-usable rather than write-only.

---

##  Test Coverage

### `WatchdogApplicationTests`
Smoke test verifying the application context loads without errors.

### `MonitorServiceTest`

| Test | What it proves |
|---|---|
| `testRegister_Success` | Monitor is created with ACTIVE status |
| `testRegister_DuplicateId_ThrowsConflict` | Duplicate IDs are rejected with 409 |
| `testHeartbeat_ResetsTimer` | Heartbeat updates lastHeartbeatAt and keeps status ACTIVE |
| `testHeartbeat_UnknownId_ThrowsNotFound` | Unknown IDs return 404 |
| `testPause_ActiveMonitor_Success` | Pausing transitions status to PAUSED |
| `testPause_AlreadyPaused_ThrowsConflict` | Double-pause is rejected |
| `testHeartbeat_ReactivatesPausedMonitor` | Heartbeat on PAUSED monitor reactivates it |
| `testTimerExpiry_TransitionsToDown` | After timeout, monitor status becomes DOWN |
| `testHeartbeat_RecoverDownMonitor` | Heartbeat on DOWN monitor recovers it to ACTIVE |
| `testGetAllMonitors_ReturnsAll` | List endpoint returns all registered monitors |

---

> **Production Note**: Replace `InMemoryMonitorRepository` with a persistent store (PostgreSQL + Spring Data JPA) and the `ScheduledExecutorService` with a distributed scheduler (Quartz, Redis TTL events, or AWS EventBridge) for multi-node deployments.