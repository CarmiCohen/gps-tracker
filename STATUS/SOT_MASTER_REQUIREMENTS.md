# System Source of Truth (SoT) - July.24.02 (ANR & IPC Hardening)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Startup Suppression Window (R993d)**: To prevent Main-thread starvation during cold-start, all Foreground Service notification type updates MUST be suppressed for the first 10 seconds of service life if a previous notification has already been successfully posted. (Issue #534, July.24.02)
*   **Notification IPC Throttling (R993b)**: To prevent Main-thread ANRs during hardware recovery bursts, Foreground Service notification updates MUST be double-throttled: a 2000ms hard gate in `AppNotificationManager` and a 10,000ms global throttle for service type changes in `BaseMonitorService` descendants. (Issue #113, #535, July.24.02)
*   **Foreground Service Immediacy (R406b)**: `startForeground` MUST be invoked directly in the Main-thread `onCreate` of any `LifecycleService`. (July.23.11)
*   **Startup Silence Authority (R993c)**: Background services MUST suppress status notification pulses (Battery/Satellites) until the system is explicitly marked as "Active". (Issue #113, July.23.12)
*   **Cold-Start Hardening (R955b)**: Implement a mandatory 500ms staggered delay before starting base observations. (Issue #099)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Dual-time strategy using monotonic `rt` for logic and wall-clock `ts` for forensic logging. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across engine, persistence, telemetry, and UI for all 15+ forensic parameters. (Issue #118, #122, #525)
*   **Remote Peer State Authority (R522)**: All remote tracker telemetry MUST be centralized in `RemoteStatusRepository`. (Issue #522)
*   **Forensic Pipeline Consolidation (R523)**: Use an atomic `ForensicSnapshot` for all sensor-based evaluations. (Issue #523)

### 3. Persistence & Service Reliability
*   **Activation Authority**: The `isSystemActive` flag in `DataStore` is the definitive authority for background lifecycle revival.
*   **Tracker Stealth Authority (R872)**: The device MUST remain silent and visually dark when operating in Tracker mode. (July.23.11)
*   **Siren Persistence (R527)**: Active alarm states MUST be persisted to DataStore and restored upon service revival. (Issue #527)

### 4. Dependency & Hardware Hardening
*   **Permission Immediacy (R107c)**: Permission state queries following a user-initiated refresh MUST be synchronous to ensure UI consistency and prevent stale setup alerts. (Issue #098, July.24.01)
*   **Reactive Sensor Recovery (R107d)**: Transitions of critical permissions (specifically `ACTIVITY_RECOGNITION`) from DENIED to GRANTED state MUST trigger an immediate sensor re-synchronization command to the background service. (Issue #098, July.24.01)
*   **Restoration Permission Authority (R107b)**: The Automatic Restoration flow in `MainAppContent` MUST verify all critical permissions before reviving a session. (Issue #113, July.23.12)
*   **Samsung Stay-Alive Hardening (R405c)**: Engage Accelerometer-based stay-alive pulse on sensor failure. Perform hardware "poke" via `SystemMonitor`. (Issue #098, #113)
*   **Step Detector Permission (R107)**: Explicitly track `android.permission.ACTIVITY_RECOGNITION`. Hardware registration MUST be deferred if permission is not granted. (Issue #098, #107)

### 5. Architectural Baselines
*   **Anchor Logic Authority (R990e)**: `AnchorEvaluator` is the central authority for stationary state. (Issue #533b)
*   **Type Safety Authority (R999)**: All internal telemetry and pipelines MUST use `Double` precision. (Issue #077, #532)

### 6. Version Authority
*   **Current Release**: July.24.02.
*   **Source of Truth**: app/build.gradle versionName.
