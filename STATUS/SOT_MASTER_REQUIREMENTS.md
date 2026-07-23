# System Source of Truth (SoT) - July.23.04 (Hardening & Persistence)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Cold-Start Hardening (R955b)**: Implement a mandatory 500ms staggered delay before starting base observations. (Issue #099)
*   **Startup Recovery Protection (R955c)**: `MaintenanceWorker` MUST implement a 60-second grace period from the `appStartTime` before attempting recovery. (Issue #108)
*   **Startup Maintenance Authority (R104)**: Execute a proactive `deepPruneLogs` operation immediately upon initialization in both UI and Service lifecycles. (Issue #104, #104b)
*   **Stability Audit Authority (R951)**: Continuous stability auditing. Gaps > 200ms relative to heartbeat MUST be logged. (Issue #031)
*   **Power Optimization (R403b)**: To preserve battery during long-duration parking, the system MUST adaptively increase the logic tick interval from 2s to 10s when the device is confirmed `STATIONARY` and the GPS is `STALLED`. (Issue #526)
*   **Adaptive Hardware Sampling (R406a-H)**: When the device enters logic-level power save, high-frequency hardware sensors (Linear Acceleration) MUST be downgraded to `SENSOR_DELAY_NORMAL` to reduce CPU wake-up frequency. (Issue #526)
*   **Acoustic Duty Cycle (R810-L2)**: During logic-level power save, the microphone MUST switch to a 20% duty cycle (2s ON / 8s OFF) to reduce energy footprint while maintaining forensic sampling. (Issue #526)

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Dual-time strategy using monotonic `rt` for logic and wall-clock `ts` for forensic logging. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across engine, persistence, telemetry, and UI for all 15+ forensic parameters. (Issue #118, #122, #525)
*   **Remote Peer State Authority (R522)**: All remote tracker telemetry MUST be centralized in `RemoteStatusRepository`. (Issue #522)
*   **Forensic Pipeline Consolidation (R523)**: Use an atomic `ForensicSnapshot` for all sensor-based evaluations to prevent peak double-consumption. (Issue #523)
*   **Monotonic Timeline Reconstruction (R105)**: Reconstruct monotonic timeline on startup using `clock_drift_ref`. (Issue #105)
*   **Unified Method for Ribbon Rendering (R106)**: Unified method for rendering ribbons across all scales. Explicitly visualize "Black Gaps." (Issue #106)
*   **Forensic Visual Authority (R404b)**: Use standardized `FORENSIC_PINK_COLOR` (#FF1493) for all forensic events. (v9.3.18)

### 3. Persistence & Service Reliability
*   **Activation Authority**: The `isSystemActive` flag in `DataStore` is the definitive authority for background lifecycle revival.
*   **Siren Persistence (R527)**: Active alarm states MUST be persisted to DataStore. If the background service is killed and restarted by the OS, the siren state MUST be restored and audio MUST automatically resume if the violation is unresolved. (Issue #527)
*   **DataStore Singleton Authority (R511)**: Initialize DataStore via `Context.dataStore` property delegate to ensure singleton instance. (Issue #511)
*   **Notification Throttling (R993)**: Foreground notification updates MUST BE throttled (default 30s). (Issue #R993)
*   **Database Migration Integrity (R956b)**: Version bump and explicit `Migration` for any `@Entity` change. (Issue #097, #118)
*   **Standardized Proto Path (R973)**: All Protobuf schemas MUST be located in `app/src/main/proto`. (Issue #030)

### 4. Dependency & Hardware Hardening
*   **Hilt Universal Authority (R120b)**: Full migration to Hilt. Manual DI is forbidden. Circularities resolved via `Provider<T>`. (Issue #120, #124, #126, #126b, #513)
*   **Samsung A15 Battery Authority (R405b)**: Proactively trigger configuration overlay if battery exemption is missing on Samsung A15. (Issue #101)
*   **Samsung Stay-Alive Hardening (R405c)**: Engage Accelerometer-based stay-alive pulse on sensor failure. Perform hardware "poke" via `SystemMonitor` on budget budget hardware. (Issue #098, #113)
*   **Step Detector Permission (R107)**: Explicitly track `android.permission.ACTIVITY_RECOGNITION`. (Issue #107)

### 5. Architectural Baselines
*   **Accuracy Recovery Grace (R529)**: The Jump Engine MUST implement an "Accuracy Recovery" grace logic. Spatial corrections resulting from a transition from low to high accuracy MUST NOT be flagged as erratic jumps if the displacement is within the previous fix's uncertainty range. (Issue #529)
*   **Stationary Anchor Convergence (R990c)**: Use a coordinate-averaging buffer (8-point sliding window) to stabilize the stationary position. Refine breakout scoring with displacement trends and velocity weights to suppress GPS drift and "spaghetti" trails. (Issue #533)
*   **Unified System Heartbeat (R403)**: Global 2000ms heartbeat standard (`TICK_INTERVAL_MS`).
*   **Type Safety Authority (R999)**: All internal telemetry and pipelines MUST use `Double` precision. (Issue #077)
*   **Binary Telemetry Authority (R988)**: Prioritize binary Protobuf-based telemetry for tracker updates.
*   **Stationary Anchor Hard-Lock (R990b)**: Establish coordinate "Hard-Lock" when stationary. (Issue #018)

### 6. Version Authority
*   **Current Release**: `July.23.04`.
*   **Source of Truth**: `app/build.gradle` `versionName`.
