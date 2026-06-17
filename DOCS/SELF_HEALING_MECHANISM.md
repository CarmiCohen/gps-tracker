# Self-Healing & Persistence Mechanism (v8.8.35)

This document describes the background persistence architecture of the GPS Tracker project, designed to ensure the background engine remains active even under aggressive OS battery management and memory pressure.

## 1. General Concept
The project treats the tracking engine as a persistent background service. In v8.8.35, this role is split between `TrackerService` and `ViewerService`. Under R800 (Unified Back Navigation), the app is never "closed" via the UI; it is only minimized or swiped away.

## 2. Detailed Mechanism

### A. The "Sticky" Service (`onStartCommand`)
Both `TrackerService` and `ViewerService` return `START_STICKY` in their `onStartCommand` method.
- **Function**: Instructs the Android OS to recreate the service if it is killed due to memory pressure.

### B. Swipe-to-Kill Resilience
While Android usually kills services when the app is swiped out of "Recent Apps," the specialized services are designed to persist.
- **Mechanism**: The services maintain their own "should be running" state in persistent storage.
- **Self-Healing**: If the process is terminated unexpectedly, the **Watchdog Alarm** or **WorkManager** detects the absence of the service and initiates a restart.

### C. Persistent Service State
The app uses a persistent flag to track if monitoring is active:
- **Service Active State**: Stored in DataStore.
- **Navigation Policy**: R800 dictates that navigation never terminates the service. Even when "Back" is pressed at the root (Map/Landing), the app calls `moveTaskToBack`, keeping the service alive in the foreground.
- **Intentional Stop**: Tracking is only officially stopped via the "TERMINATE TRACKING SESSION" confirmation dialog.

### D. Foreground Service & WakeLocks (v8.8.35 Hardening)
- **Foreground Status**: Services run with persistent high-priority notifications and declared types (`location`, `microphone`). Correctly passes `FOREGROUND_SERVICE_TYPE_LOCATION` for Android 10+ compliance.
- **WakeLock Hardening**: The system utilizes a non-reference-counted `PARTIAL_WAKE_LOCK`. Every `acquireWakeLock` call performs a "safety-timer" refresh, resetting the 10-minute timeout (`WAKELOCK_TIMEOUT_MS`).
- **Monotonic Integrity**: Watchdog, timeout evaluations, and UI lockout windows use `TimeProvider.elapsedRealtime()` to ensure absolute consistency.

### E. Advanced Hardware Self-Healing (Issue 124)
As of v8.8.35, the system includes an **Escalated GPS Revival** mechanism:
- **Stall Detection**: Monitors if the GPS hardware is providing frozen coordinates.
- **Retry Loop**: Attempts a hardware-level refresh every 5 minutes (`GPS_REVIVAL_RETRY_INTERVAL_MS`) during a stall.
- **Critical Escalation**: If 3 attempts fail, the system emits a `CRITICAL: GPS_HARDWARE_LOCK` log to notify the monitor that physical relocation or manual intervention is required.

### F. Watchdog Precision (Exact Alarms)
On Android 12+, the system utilizes `SCHEDULE_EXACT_ALARM` permissions to ensure the 90s watchdog cycle (`SYSTEM_WATCHDOG_INTERVAL_MS`) is precise.

### G. Setup Guide Integration
The "Phone Setup" guide instructs users on manual steps to bypass OEM restrictions:
1. **Battery Whitelist**: Set to "Unrestricted" (verified via `systemStatusProvider`).
2. **Auto-Start**: Enable in system settings.
3. **Xiaomi/Poco Alignment**: Explicit gating for "Display pop-up windows" via `ALERT_ID_XIAOMI_SYSTEM_MISSING` and manual override support. Verified via `isXiaomiAutostartGranted`.

## 3. Summary of Files Involved
- `TrackerService.kt` / `ViewerService.kt`: Role-specific engine executors.
- `SystemMonitor.kt`: WakeLock and Watchdog management.
- `MainActivity.kt`: Root navigation and backgrounding logic.
- `WatchdogReceiver.kt`: Alarm response and service revival.
- `MaintenanceWorker.kt`: OS-level periodic safety net.
- **Forensic Unification**: Legacy `ver` and `vid` tags have been removed in v8.8.35 to simplify the forensic model.
