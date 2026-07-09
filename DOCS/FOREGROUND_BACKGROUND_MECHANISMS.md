# GPS-Tracker: Foreground & Background Mechanisms (v9.3.6)

This document provides a comprehensive technical overview of how the GPS-Tracker app maintains its high-persistence state on Android using specialized service roles and a Triple-Lock Watchdog system.

## 1. General Architecture
The app is designed with a **"Reliability-First"** philosophy. Unlike typical apps that rely on the UI being visible, this application is built around specialized services that operate independently of the Activity lifecycle.

### Specialized Service Roles
The service architecture is split into two role-based services, both extending `BaseMonitorService`:
*   **TrackerService**: Optimized for the "Tracker" role. It focuses on battery efficiency, sensor fidelity, and high-persistence location reporting. Features 10Hz polling (Issue #432), escalated hardware revival (Issue #341), and Xiaomi suppression recovery (Issue #439).
*   **ViewerService**: Optimized for the "Viewer" role. It manages real-time HUD telemetry, analytical ribbons, and remote data synchronization. Includes background location polling for relative geofencing.

---

## 2. Foreground Mechanism (The Primary Layer)

### BaseMonitorService (Android 15 Alignment)
Both `TrackerService` and `ViewerService` inherit from `BaseMonitorService`, which is implemented as a `LifecycleService`.
*   **Foreground Service Types**: 
    *   **TrackerService**: Requests `location` and `microphone` types. The microphone type is managed with a 45s hysteresis (`FGS_STICKY_DELAY_MS`).
    *   **ViewerService**: Requests `location` type. Correctly passes `FOREGROUND_SERVICE_TYPE_LOCATION` for Android 10+ compliance.
*   **Foreground Notification**: Upon creation, the active service immediately calls `startForeground()`.
*   **START_STICKY**: If killed under extreme pressure, the OS recreates the service.

### Foreground UI Overlay
The app utilizes `SYSTEM_ALERT_WINDOW` permissions to draw critical red-screen alerts over other apps. A mandatory lockout period applies after dismissal using `TimeProvider.elapsedRealtime()`.

---

## 3. Background Persistence (The Core Layer - Triple-Lock Watchdog)

### Layer 1: WakeLock Hardening
The `SystemMonitor` manages a `PowerManager.WakeLock`. 
*   **Implementation**: A non-reference-counted `PARTIAL_WAKE_LOCK` is acquired.
*   **Active Renewal**: Refreshes the timeout on every service tick to ensure persistent execution.

### Layer 2: The Watchdog Alarm (AlarmManager)
The `SystemMonitor` schedules a "Watchdog Alarm" using Android's `AlarmManager`.
*   **Mechanism**: Schedules `ACTION_ALARM_WAKEUP` every 90s (`SYSTEM_WATCH_DOG_INTERVAL_MS`).
*   **Heartbeat Integration**: Integrated into the main service tick loop to maintain a heartbeat chain (Issue #456 / Formerly #366-R).

### Layer 3: Second Line of Defense (WorkManager)
The `MaintenanceWorker` provides a higher-level reliability layer, verifying if the required service is running and invoking recovery logic if a termination is detected. Scheduled on application startup (Issue #456 / Formerly #366-R).

### Layer 4: Boot Recovery
The `BootReceiver` listens for `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` to ensure the correct service role is restored across reboots and app updates.

---

## 4. Networking Persistence (AppNetworkManager)

### Zombie Connection Detection
Proactive detection for "Zombie" TCP sockets common in mobile handovers.
*   **HTTP Health Pulses**: Independent pulses to the relay. If HTTP succeeds but Socket.io is silent, a "Zombie" state is declared and reconnection is forced.
*   **Sync Interval**: Uses `PING_INTERVAL_MS` (10,000ms) with RTT-aware scaling (Issue #315).

---

## 5. Security & Integrity Monitoring (v9.3.6 Baseline)

In the background, the system continuously evaluates:
*   **Internet Integrity**: Detects local internet loss via `ALERT_ID_LOCAL_INTERNET`.
*   **Signal Integrity**: Monitors remote device connectivity. Uses 35s R338 mandate for staleness indicators.
*   **Storage Integrity**: Dual-tier watchdog (< 50MB Low, < 10MB Critical) protects database health.
*   **GNSS Integrity**: Detects hardware stalls and triggers revival retry every 120s (Issue #341).
*   **Power Integrity**: Full parity for **battery current** (`currentMa`) across all forensic layers (Issue #337).
*   **Log Spatial Anchor**: All background integrity events are geographically anchored with Dual-Metric accuracy (Issue #325).
*   **Xiaomi Hardening (Issue #439)**: Includes 30s boot grace, 15s suppression detection, and 60s recovery cooldown for heuristic revival.
