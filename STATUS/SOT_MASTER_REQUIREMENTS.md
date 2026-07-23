# System Source of Truth (SoT) - July.24.01 (Permission Immediacy)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Foreground Service Immediacy (R406b)**: `startForeground` MUST be invoked directly in the Main-thread `onCreate` of any `LifecycleService`. Delayed or background-threaded invocation is forbidden to prevent `ForegroundServiceDidNotStartInTimeException`. (July.23.11)
*   **Notification IPC Throttling (R993b)**: To prevent Main-thread ANRs during hardware recovery bursts, Foreground Service notification updates MUST be double-throttled: a 2000ms hard gate in `AppNotificationManager` and a 5000ms gate in `BaseMonitorService`. (Issue #113, July.23.12)
*   **Startup Silence Authority (R993c)**: Background services MUST suppress status notification pulses (Battery/Satellites) until the system is explicitly marked as "Active" via `isSystemActiveFlow`. This ensures the Landing Page remains responsive during cold starts. (Issue #113, July.23.12)
*   **Cold-Start Hardening (R955b)**: Implement a mandatory 500ms staggered delay before starting base observations. (Issue #099)
*   **Startup Recovery Protection (R955c)**: `MaintenanceWorker` MUST implement a 60-second grace period from the `appStartTime` before attempting recovery. (Issue #108)
*   **Startup Maintenance Authority (R104)**: Execute a proactive `deepPruneLogs` operation immediately upon initialization in both UI and Service lifecycles. (Issue #104, #104b)
*   **Stability Audit Authority (R951)**: Continuous stability auditing. Gaps > 200ms relative to heartbeat MUST be logged. (Issue #031)
*   **Power Optimization (R403b)**: System MUST adaptively increase logic tick interval (2s to 10s) when device is `STATIONARY` and GPS is `STALLED`. (Issue #526)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Dual-time strategy using monotonic `rt` for logic and wall-clock `ts` for forensic logging. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across engine, persistence, telemetry, and UI for all 15+ forensic parameters. (Issue #118, #122, #525)
*   **Remote Peer State Authority (R522)**: All remote tracker telemetry MUST be centralized in `RemoteStatusRepository`. (Issue #522)
*   **Forensic Pipeline Consolidation (R523)**: Use an atomic `ForensicSnapshot` for all sensor-based evaluations to prevent peak double-consumption. (Issue #523)
*   **Monotonic Timeline Reconstruction (R105)**: Reconstruct monotonic timeline on startup using `clock_drift_ref`. (Issue #105)
*   **Unified Method for Ribbon Rendering (R106)**: Unified method for rendering ribbons across all scales. Explicitly visualize "Black Gaps." (Issue #106)

### 3. Persistence & Service Reliability
*   **Activation Authority**: The `isSystemActive` flag in `DataStore` is the definitive authority for background lifecycle revival.
*   **Tracker Stealth Authority (R872)**: The device MUST remain silent and visually dark when operating in Tracker mode. Local audio sirens and red-screen overlays MUST be suppressed. (July.23.11)
*   **Siren Persistence (R527)**: Active alarm states MUST be persisted to DataStore and restored upon service revival. (Issue #527)
*   **Notification Throttling (R993)**: Foreground notification updates MUST BE throttled (default 30s). (Issue #R993)

### 4. Dependency & Hardware Hardening
*   **Permission Immediacy (R107c)**: Permission state queries following a user-initiated refresh MUST be synchronous to ensure UI consistency and prevent stale setup alerts. (Issue #098, July.24.01)
*   **Reactive Sensor Recovery (R107d)**: Transitions of critical permissions (specifically `ACTIVITY_RECOGNITION`) from DENIED to GRANTED state MUST trigger an immediate sensor re-synchronization command to the background service. (Issue #098, July.24.01)
*   **Restoration Permission Authority (R107b)**: The Automatic Restoration flow in `MainAppContent` MUST verify all critical permissions (including `ACTIVITY_RECOGNITION`) before reviving a session. If permissions are missing, the UI MUST trigger the request launcher instead of allowing the background service to stall. (Issue #113, July.23.12)
*   **Hilt Universal Authority (R120b)**: Full migration to Hilt. Manual DI is forbidden. Circularities resolved via `Provider<T>`. (Issue #120, #124, #126, #126b, #513)
*   **Samsung A15 Battery Authority (R405b)**: Proactively trigger configuration overlay if battery exemption is missing on Samsung A15. (Issue #101)
*   **Samsung Stay-Alive Hardening (R405c)**: Engage Accelerometer-based stay-alive pulse on sensor failure. Perform hardware "poke" via `SystemMonitor`. (Issue #098, #113)
*   **Step Detector Permission (R107)**: Explicitly track `android.permission.ACTIVITY_RECOGNITION`. Hardware registration MUST be deferred if permission is not granted. (Issue #098, #107)

### 5. Architectural Baselines
*   **Anchor Logic Authority (R990e)**: `AnchorEvaluator` is the central authority for stationary state. It MUST implement a safety valve that accelerates breakout if GPS displacement consistently exceeds 2x the threshold. (Issue #533b)
*   **Type Safety Authority (R999)**: All internal telemetry and pipelines MUST use `Double` precision. (Issue #077, #532)
*   **Binary Telemetry Authority (R988)**: Prioritize binary Protobuf-based telemetry for tracker updates.

### 6. Version Authority
*   **Current Release**: July.24.01.
*   **Source of Truth**: app/build.gradle versionName.
