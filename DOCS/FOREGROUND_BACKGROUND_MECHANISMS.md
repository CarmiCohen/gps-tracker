# GPS-Tracker: Foreground & Background Mechanisms (v8.8.35)

This document provides a comprehensive technical overview of how the GPS-Tracker app maintains its high-persistence state on Android using specialized service roles.

## 1. General Architecture
The app is designed with a **"Reliability-First"** philosophy. Unlike typical apps that rely on the UI being visible, this application is built around specialized services that operate independently of the Activity lifecycle.

### Specialized Service Roles
The monolithic `AppService` has been split into two role-based services, both extending `BaseMonitorService`:
*   **TrackerService**: Optimized for the "Tracker" role. It focuses on battery efficiency, sensor fidelity, and high-persistence location reporting.
*   **ViewerService**: Optimized for the "Viewer" role. It manages real-time HUD telemetry, analytical ribbons, and remote data synchronization.

These services act as the "Manager Containers" that orchestrate location tracking, network signaling, and system monitoring. They are designed to be:
*   **Persistent**: Resist system-initiated kills (`START_STICKY`).
*   **Self-Healing**: Automatically recover from connection or hardware stalls.
*   **Visible**: Maintain a presence in the notification shade to prevent OS-level background restrictions.

---

## 2. Foreground Mechanism (The Primary Layer)

### BaseMonitorService (Android 14/15 Alignment)
Both `TrackerService` and `ViewerService` inherit from `BaseMonitorService`, which is implemented as a `LifecycleService`.
*   **Foreground Service Types**: 
    *   **TrackerService**: Requests `location` and `microphone` types. The microphone type is managed with a 45s hysteresis (`FGS_STICKY_DELAY_MS`) for acoustic monitoring session stability.
    *   **ViewerService**: Requests `location` type for proximity-based tracking. Correctly passes `FOREGROUND_SERVICE_TYPE_LOCATION` for Android 10+ (v8.8.7).
*   **Foreground Notification**: Upon creation, the active service immediately calls `startForeground()`. This grants higher priority in memory management.
*   **START_STICKY**: If killed under extreme pressure, the OS recreates the service.
*   **Lifecycle Awareness**: Uses `lifecycleScope` for automatic coroutine cleanup.

### Foreground UI Overlay
The app utilizes `SYSTEM_ALERT_WINDOW` permissions to draw critical red-screen alerts over other apps. This ensures visibility even when the device is locked. A mandatory lockout period of 30s (`ALARM_OVERLAY_THROTTLE_MS`) applies after dismissal to prevent accidental re-launches while allowing new alarm types to propagate logs and status.

---

## 3. Background Persistence (The Core Layer)

### Layer 1: WakeLock Hardening
The `SystemMonitor` (utilized by the active service) manages a `PowerManager.WakeLock`. 
*   **Implementation**: A non-reference-counted `PARTIAL_WAKE_LOCK` is acquired.
*   **Safety-Timer Refresh**: Refreshes the 10-minute timeout (`WAKELOCK_TIMEOUT_MS`) regardless of current state, ensuring the CPU never enters deep sleep.

### Layer 2: The Watchdog Alarm (AlarmManager)
The `SystemMonitor` schedules a "Watchdog Alarm" using Android's `AlarmManager`.
*   **Mechanism**: Schedules `ACTION_ALARM_WAKEUP` every 90s (`SYSTEM_WATCHDOG_INTERVAL_MS`).
*   **Exact Alarms**: On Android 12+, utilizes `SCHEDULE_EXACT_ALARM` permissions.

### Layer 3: Second Line of Defense (WorkManager)
The `MaintenanceWorker` provides a higher-level reliability layer, verifying if the required service (`TrackerService` or `ViewerService`) is running and invoking recovery logic if a termination is detected.

### Layer 4: Boot Recovery
The `BootReceiver` listens for `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` to ensure the correct service role is restored across reboots and app updates.

---

## 4. Networking Persistence (AppNetworkManager)

### Zombie Connection Detection
Proactive detection for "Zombie" TCP sockets common in mobile handovers.
*   **The Solution**: Independent **HTTP Health Pulses** to the relay. If HTTP succeeds but Socket.io is silent, a "Zombie" state is declared and reconnection is forced.

---

## 5. Security & Integrity Monitoring (v8.8.35 Baseline)

In the background, the system continuously evaluates:
*   **Internet Integrity**: Detects local internet loss (`INTERNET_LOSS_THRESHOLD_MS` 60s).
*   **Signal Integrity**: Monitors the remote device heartbeat (`TRACKER_SIGNAL_LOSS_THRESHOLD_MS` 180s).
*   **Storage Integrity**: Dual-tier watchdog (<50MB/10MB) protects database health by gating non-essential I/O.
*   **GNSS Integrity**: Detects hardware stalls (`GPS_STALL_THRESHOLD_MS` 180s) and active jamming via per-satellite SNR monitoring (`snrIdx`).
*   **Role Identification**: Every forensic packet is tagged with the mandatory `role` field. Legacy version tags have been removed from models to simplify architecture.

## Summary Table: Persistence Layers

| Layer | Component | Purpose | Trigger |
| :--- | :--- | :--- | :--- |
| **Foreground** | `Tracker/ViewerService` | OS Visibility | App Start / Role Selection |
| **CPU** | `WakeLock` | Prevent Sleep | 10m Refresh |
| **Recovery** | `Watchdog Alarm` | Scheduled Check | 90s Interval |
| **Safety Net** | `WorkManager` | Periodic Check | OS Constraint |
| **Boot** | `BootReceiver` | Restart Role | Device Reboot |
| **Network** | `HTTP Pulse` | Socket Recovery | Every 3 mins |
