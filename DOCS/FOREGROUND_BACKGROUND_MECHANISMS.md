# GPS-Tracker: Foreground & Background Mechanisms (v8.9.37)

This document provides a comprehensive technical overview of how the GPS-Tracker app maintains its high-persistence state on Android using specialized service roles.

## 1. General Architecture
The app is designed with a **"Reliability-First"** philosophy. Unlike typical apps that rely on the UI being visible, this application is built around specialized services that operate independently of the Activity lifecycle.

### Specialized Service Roles
The service architecture is split into two role-based services, both extending `BaseMonitorService`:
*   **TrackerService**: Optimized for the "Tracker" role. It focuses on battery efficiency, sensor fidelity, and high-persistence location reporting. Features 10Hz polling (`A15_STABLE_GPS_POLLING_MS` 1000ms for stable sampling) and escalated hardware revival (Issue #341).
*   **ViewerService**: Optimized for the "Viewer" role. It manages real-time HUD telemetry, analytical ribbons, and remote data synchronization. Includes background location polling for relative geofencing.

---

## 2. Foreground Mechanism (The Primary Layer)

### BaseMonitorService (Android 15 Alignment)
Both `TrackerService` and `ViewerService` inherit from `BaseMonitorService`, which is implemented as a `LifecycleService`.
*   **Foreground Service Types (Issue #247)**: 
    *   **TrackerService**: Requests `location` and `microphone` types. The microphone type is managed with a 45s hysteresis (`FGS_STICKY_DELAY_MS`).
    *   **ViewerService**: Requests `location` type. Correctly passes `FOREGROUND_SERVICE_TYPE_LOCATION` for Android 10+ compliance.
*   **Foreground Notification**: Upon creation, the active service immediately calls `startForeground()`.
*   **START_STICKY**: If killed under extreme pressure, the OS recreates the service.

### Foreground UI Overlay
The app utilizes `SYSTEM_ALERT_WINDOW` permissions to draw critical red-screen alerts over other apps. A mandatory lockout period of 30s (`ALARM_OVERLAY_THROTTLE_MS`) applies after dismissal (Issue #311).

---

## 3. Background Persistence (The Core Layer)

### Layer 1: WakeLock Hardening
The `SystemMonitor` manages a `PowerManager.WakeLock`. 
*   **Implementation**: A non-reference-counted `PARTIAL_WAKE_LOCK` is acquired.
*   **Safety-Timer Refresh**: Refreshes the timeout regardless of current state to ensure active WakeLock renewal on every service tick (Issue #363).

### Layer 2: The Watchdog Alarm (AlarmManager)
The `SystemMonitor` schedules a "Watchdog Alarm" using Android's `AlarmManager`.
*   **Mechanism**: Schedules `ACTION_ALARM_WAKEUP` every 90s (`SYSTEM_WATCH_DOG_INTERVAL_MS`).
*   **Exact Alarms**: On Android 12+, utilizes `SCHEDULE_EXACT_ALARM` permissions.

### Layer 3: Second Line of Defense (WorkManager)
The `MaintenanceWorker` provides a higher-level reliability layer, verifying if the required service is running and invoking recovery logic if a termination is detected.

### Layer 4: Boot Recovery (Issue #175)
The `BootReceiver` listens for `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` to ensure the correct service role is restored across reboots and app updates.

---

## 4. Networking Persistence (AppNetworkManager)

### Zombie Connection Detection
Proactive detection for "Zombie" TCP sockets common in mobile handovers.
*   **The Solution**: Independent **HTTP Health Pulses** to the relay. If HTTP succeeds but Socket.io is silent, a "Zombie" state is declared and reconnection is forced. Uses `PING_INTERVAL_MS` (10,000ms) for baseline synchronization (Issue #315).

---

## 5. Security & Integrity Monitoring (v8.9.37 Baseline)

In the background, the system continuously evaluates:
*   **Internet Integrity**: Detects local internet loss via `ALERT_ID_LOCAL_INTERNET`.
*   **Signal Integrity**: Monitors the remote device connectivity via `WATCH_TIMEOUT_MS` (30s).
*   **Storage Integrity (Issue #316)**: Dual-tier watchdog (`SYSTEM_STORAGE_LOW_THRESHOLD_MB`/`SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB`) protects database health.
*   **GNSS Integrity (Issue #341)**: Detects hardware stalls and triggers hardware revival retry every 120s.
*   **Power Integrity (Issue #337)**: Absolute parity for **battery current** (`currentMa`) across all models and the database.
*   **Log Spatial Anchor (Issue #208)**: All background integrity events (Stalls, Power, Storage) are now geographically anchored with `lat`/`lng` coordinates.
*   **Xiaomi Boot Grace (Issue #190)**: Implemented 30s grace period (`XIAOMI_BOOT_GRACE_MS`) to suppress transient boot alarms and handle "Unknown" autostart status.
