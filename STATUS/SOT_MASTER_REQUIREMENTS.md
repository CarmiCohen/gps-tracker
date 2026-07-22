# System Source of Truth (SoT) - July.22.06 (Hilt Baseline)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization (Database, Hardware Managers) during cold start. (Issue #526)
*   **Cold-Start Hardening (R955b)**: To prevent Main-thread frame skipping and ANRs on low-end hardware, the system MUST implement a mandatory 500ms staggered delay before starting base observations. (Issue #099)
*   **Startup Maintenance Authority (R104)**: To prevent I/O bottlenecks and ANRs during cold starts, the system MUST execute a proactive `deepPruneLogs` operation on `Dispatchers.IO` immediately upon initialization. (Issue #104)
*   **Lazy Safety**: All Hilt managers and repositories MUST use `LazyThreadSafetyMode.PUBLICATION` if any internal state requires lazy initialization to prevent thread stalling.

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: To ensure logic stability against system clock drifts or manual adjustments, the engine MUST employ a dual-time strategy using monotonic `rt` for logic and wall-clock `ts` for forensic logging. (Issue #102)
*   **Forensic Parity Authority (R118)**: The system MUST maintain strict field parity across engine models (`LocationUpdate`), persistence (`HistoryEntity`, Database `v59`), telemetry pipelines (Binary/JSON Relay), and UI data structures for all 15+ forensic SIT (Sit Detection) and Indexing parameters. (Issue #118, #122)
*   **Monotonic Timeline Reconstruction (R105)**: To ensure "1Hz Ribbon Fidelity" across process boundaries, the system MUST reconstruct the monotonic timeline on startup using `clock_drift_ref`. (Issue #105)
*   **Unified Method for Ribbon Rendering (R106)**: The system MUST implement a unified method for rendering ribbons across all scales. Missing data periods (app-off or service-death) MUST be explicitly visualized as "Black Gaps." (Issue #106)
*   **Forensic Visual Authority (R404b)**: The system MUST use a standardized `FORENSIC_PINK_COLOR` (#FF1493) for all forensic events. (v9.3.18)

### 3. Persistence & Service Reliability
*   **Activation Authority**: The `isSystemActive` flag in `DataStore` is the definitive authority for background lifecycle revival. Background services MUST NOT restart automatically unless this flag is set.
*   **DataStore Singleton Authority (R511)**: To prevent `IllegalStateException` during startup, Jetpack DataStore MUST be initialized via the `Context.dataStore` property delegate. This ensures exactly one instance exists per process across all Hilt entry points. (Issue #511)
*   **Notification Throttling (R993)**: Foreground notification updates MUST BE throttled (default 30s) to prevent system-wide Logcat flooding and reduce CPU wakeups. (Issue #R993)
*   **Database Migration Integrity (R956b)**: Any change to an `@Entity` class MUST be accompanied by a version bump and an explicit `Migration` object. (Issue #097, #118)
*   **Standardized Proto Path (R973)**: All Protobuf schemas MUST be located in `app/src/main/proto`. (Issue #030)

### 4. Dependency & Hardware Hardening
*   **Hilt Universal Authority (R120b)**: The manual `AppContainer` is fully decommissioned. All core repositories, UseCases, and managers MUST be integrated into the Hilt graph using `@Inject` constructors and `@Singleton` scoping. Manual DI is forbidden. Circularities MUST be resolved via Dagger `Provider<T>`. (Issue #120, #124, #126)
*   **Samsung A15 Battery Authority (R405b)**: The system MUST proactively trigger the configuration overlay if battery exemption is missing on Samsung A15 hardware. (Issue #101)
*   **Samsung Stay-Alive Fallback (R405c)**: The system MUST detect hardware sensor registration failures and immediately engage the Accelerometer-based stay-alive pulse. (Issue #098)
*   **Step Detector Permission (R107)**: The system MUST explicitly track `android.permission.ACTIVITY_RECOGNITION` to ensure hardware Step Detector availability on API 29+. (Issue #107)

### 5. Architectural Baselines
*   **Unified System Heartbeat (R403)**: Global 2000ms heartbeat standard (`TICK_INTERVAL_MS`).
*   **Type Safety Authority (R999)**: All internal telemetry, sensor data, and engine pipelines MUST use `Double` precision. (Issue #077)
*   **Binary Telemetry Authority (R988)**: The system MUST prioritize binary Protobuf-based telemetry for high-frequency tracker updates.
*   **Stationary Anchor Hard-Lock (R990b)**: The engine MUST establish a coordinate "Hard-Lock" when stationary. (Issue #018)

### 6. Version Authority
*   **Current Release**: `July.22.06`.
*   **Source of Truth**: `app/build.gradle` `versionName`.
