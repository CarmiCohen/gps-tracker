# Self-Healing & Persistence Mechanism (v8.9.37)

This document describes the background persistence architecture of the GPS Tracker project, designed to ensure the background engine remains active even under aggressive OS battery management and memory pressure.

## 1. General Concept
The project treats the tracking engine as a persistent background service. In v8.9.37, this role is split between `TrackerService` and `ViewerService`. Under the unified navigation model, the app is never "closed" via the UI; it is minimized or swiped away.

## 2. Detailed Mechanism

### A. The "Sticky" Service (`onStartCommand`)
Services return `START_STICKY`, instructing the OS to recreate them if killed due to memory pressure.

### B. Swipe-to-Kill Resilience
Services maintain their own "should be running" state in persistent storage. If terminated unexpectedly, the **Watchdog Alarm** or **WorkManager** detects the absence and initiates a restart.

### C. Persistent Service State
Navigation never terminates the service. Even when "Back" is pressed at the root, the app calls `moveTaskToBack`, keeping the service alive in the foreground.

### D. Foreground Service & WakeLocks
- **Compliance**: Correctly passes `FOREGROUND_SERVICE_TYPE_LOCATION` (and `MICROPHONE` when active) for Android 10+ compatibility (Issue #247).
- **WakeLock Hardening**: Utilizes a non-reference-counted `PARTIAL_WAKE_LOCK` with active renewal on every service tick (Issue #148).
- **Monotonic Integrity**: Watchdog and timeout evaluations use `TimeProvider.elapsedRealtime()` (Issue #413).

### E. Advanced Hardware Self-Healing
- **Stall Detection**: Monitors if coordinates are frozen. Hardened to **60s** (`GPS_STALL_THRESHOLD_MS`).
- **Retry Loop**: Attempts a hardware-level refresh every **120 seconds** (`GPS_REVIVAL_RETRY_INTERVAL_MS`).
- **Critical Escalation**: After 3 failed attempts, a `CRITICAL: GPS_HARDWARE_LOCK` log is emitted (Issue #124).
- **Log Spatial Anchor**: These critical revival and lock events are automatically anchored with `lat`/`lng` coordinates to help forensic reconstruction (Issue #208).

### F. Xiaomi Boot Resilience
Includes `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient "System Not Ready" alarms during the MIUI/HyperOS boot transition phase (Issue #190).

## 3. Summary of Files Involved
- `TrackerService.kt` / `ViewerService.kt`: Role-specific engine executors.
- `SystemMonitor.kt`: WakeLock and Watchdog management.
- `MainActivity.kt`: Root navigation and backgrounding logic.
- `LogManager.kt`: Coordinate attachment for self-healing logs.
