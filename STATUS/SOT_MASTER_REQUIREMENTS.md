# System Source of Truth (SoT) - July.17.02 (Release)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority (Issue #526)
*   **Main-Thread Purity**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start.
*   **Lazy Safety**: All properties in `AppContainer` MUST use `LazyThreadSafetyMode.PUBLICATION` to prevent thread stalling during background warm-ups.
*   **Non-Blocking Services**: `BaseMonitorService.onCreate` MUST be logic-free. All initialization, including `startForeground`, MUST occur within a background lifecycle scope.

### 2. Silence & Persistence (Issue #R993, #R994)
*   **Notification Throttling**: Foreground notification updates MUST BE throttled (default 30s) to prevent system-wide Logcat flooding and reduce CPU wakeups.
*   **Boot Revival Logic**: Background services MUST NOT restart automatically on device reboot or app update unless the system was in an "Active/Armed" state at the time of shutdown. 
*   **Activation Authority**: The `isSystemActive` flag in `DataStore` is the definitive authority for background lifecycle revival.

### 3. Architectural Baselines
*   **Stable Stationary Anchor (R406m)**: `LocationProcessor` locks geographic anchors when speed < 0.5m/s.
*   **Manual Dependency Injection (R406c)**: manual DI via `AppContainer` is the sole injection pattern.
*   **Unified System Heartbeat (R406a)**: Global 2000ms heartbeat standard.

### 4. Version Authority
*   **Current Baseline**: `July.17.02`.
*   **Source of Truth**: `app/build.gradle` `versionName`.
