# System Source of Truth (SoT) - July.17.00 (Release)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority (Issue #526)
*   **Main-Thread Purity**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start.
*   **Lazy Safety**: All properties in `AppContainer` MUST use `LazyThreadSafetyMode.PUBLICATION` to prevent thread stalling during background warm-ups.
*   **Non-Blocking Services**: `BaseMonitorService.onCreate` MUST be logic-free. All initialization, including `startForeground`, MUST occur within a background lifecycle scope to maintain UI responsiveness on budget hardware (Samsung A15).
*   **Notification Decoupling**: `AppNotificationManager` MUST remain independent of the `MainRepository` and `Database` to allow instant foreground service binding.

### 2. Architectural Baselines
*   **Stable Stationary Anchor (R406m)**: `LocationProcessor` locks geographic anchors when speed < 0.5m/s.
*   **Passive Tilt Zeroing (R406l)**: `LocationSentinel` adopts resting orientation as baseline after 5 mins of stability.
*   **Manual Dependency Injection (R406c)**: manual DI via `AppContainer` is the sole injection pattern.
*   **Unified System Heartbeat (R406a)**: Global 2000ms heartbeat standard.

### 3. Version Authority
*   **Current Baseline**: `July.17.00`.
*   **Source of Truth**: `app/build.gradle` `versionName`.
