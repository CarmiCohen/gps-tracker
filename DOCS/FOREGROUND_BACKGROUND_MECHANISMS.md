# GPS-Tracker: Foreground & Background Mechanisms (vAug.07.06)

This document provides a comprehensive technical overview of how the GPS-Tracker app maintains its high-persistence state on Android using specialized service roles and a Triple-Lock Watchdog system. As of vAug.07.06, all terminology follows the R747 Locality Authority.

## 1. General Architecture
The app is designed with a **"Reliability-First"** philosophy. Services operate independently of the Activity lifecycle to ensure forensic continuity.

### Specialized Service Roles
The service architecture is split into two role-based services, both extending `BaseMonitorService`:
*   **TrackerService (Device)**: Optimized for stealth and battery efficiency on the remote device. Focuses on sensor fidelity, JdMbrain stabilization (R746), and location reporting.
*   **ViewerService**: Optimized for real-time HUD telemetry on this device, analytical ribbons, and remote data synchronization.

---

## 2. Foreground Mechanism (The Primary Layer)

### BaseMonitorService (R406b Hardening)
Both services inherit from `BaseMonitorService`, which is implemented as a `LifecycleService`.
*   **Foreground Service Immediacy (R406b)**: To prevent `ForegroundServiceDidNotStartInTimeException`, the service MUST call `startForeground()` immediately within the Main-thread `onCreate()`. Delayed initialization inside coroutines or background threads is strictly forbidden.
*   **Foreground Service Types**: 
    *   **TrackerService**: Requests `location`, `microphone`, and `specialUse`. The microphone type is managed with a 45s hysteresis (`FGS_STICKY_DELAY_MS`).
    *   **ViewerService**: Requests `location` type. 
*   **START_STICKY**: If killed under extreme pressure, the OS recreates the service automatically.

### Foreground UI Overlay
The app utilizes `SYSTEM_ALERT_WINDOW` permissions to draw critical red-screen alerts over other apps. 
*   **Stealth Requirement (R872)**: These overlays are strictly suppressed in Tracker mode on the device.

---

## 3. Background Persistence (Triple-Lock Watchdog)

### Layer 1: WakeLock Hardening
The `SystemMonitor` manages a `PowerManager.WakeLock` (`PARTIAL_WAKE_LOCK`) which is actively renewed on every service tick.

### Layer 2: The Watchdog Alarm (AlarmManager)
Schedules `ACTION_ALARM_WAKEUP` every 90s to maintain a heartbeat chain.

### Layer 3: Second Line of Defense (WorkManager)
The `MaintenanceWorker` provides a high-level reliability layer, verifying service health and invoking recovery logic if a termination is detected.

---

## 4. Security & Integrity Monitoring
In the background, the system continuously evaluates Internet, Signal, Storage, GNSS, and Power integrity. All events are geographically anchored with Dual-Metric accuracy and follow the R747 locality mapping (e.g., "This device: Internet Lost" for local events).
