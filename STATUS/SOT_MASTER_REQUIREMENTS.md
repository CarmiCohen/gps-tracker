# System Source of Truth (SoT) - July.19.04

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Drift Reference Persistence (R103)**: To maintain forensic continuity across process restarts, the system MUST persist the monotonic-to-wall-clock drift reference (`clock_drift_ref`). This ensures that gap-filling logic in `HistoryManager` can correctly identify system clock adjustments made while the application was inactive, preserving the 1Hz ribbon fidelity even after cold starts. (July.19.04 / Issue #103)
*   **Temporal Forensic Integrity (R102)**: To ensure logic stability against system clock drifts or manual adjustments, the engine MUST employ a dual-time strategy. (a) **Monotonic Time (`rt`)**: All internal state transitions, debouncing, hindsight buffer aging, and duration-based logic MUST use `SystemClock.elapsedRealtime()`. (b) **Forensic Time (`ts`)**: Wall-clock (UTC) timestamps MUST be preserved strictly for human-readable logging and external reporting. All engine models (Geo/Connection points) MUST explicitly carry both time-streams. (July.19.01 / Issue #102)
*   **Cold-Start Hardening Authority (R955b)**: To prevent Main-thread frame skipping and ANRs on low-end hardware, the system MUST: (a) Implement a mandatory 500ms staggered delay before starting base observations, (b) Cache all hardware property checks (e.g., `Build.MODEL` checks for A15) at the service/provider level, and (c) Defer all non-critical IPC permission status checks until after the initial UI composition is stable. (July.19.01 / Issue #099)
*   **Samsung Stay-Alive Hardware Fallback (R405c)**: The system MUST detect hardware sensor registration failures (e.g., Step Detector returning `false` on `registerListener`) and immediately engage the Accelerometer-based stay-alive pulse to maintain process priority. (July.19.00 / Issue #098)
*   **Samsung A15 Battery Prompt Authority (R405b)**: The system MUST proactively trigger the configuration overlay if battery exemption is missing on Samsung A15 hardware, ensuring user awareness of critical background requirements. (July.18.03 / Issue #101)
*   **Database Migration Integrity (R956b)**: Any change to an `@Entity` class MUST be accompanied by a version bump and an explicit `Migration` object. To resolve `IllegalStateException` integrity errors (Identity Hash mismatch), the physical schema MUST strictly align with Room's generated SQL, including precise default value formatting. Re-harmonization via table recreation is the authoritative remediation for schema drift. (July.18.01 / Issue #097)
*   **Dynamic Anchor Breakout Authority (R990)**: To prevent "sticky anchors" during physical movement, the system MUST employ a displacement-weighted monitor. This monitor (`anchorEscapeScore`) MUST evaluate: (a) Physical motion detection via sensors, (b) Positional trend analysis over a window of points, and (c) Progress through a distance-based transition zone relative to accuracy-weighted thresholds. (v9.3.56 / Issue #062)
*   **Database Default Normalization (R956)**: ALL `Double` (REAL) column defaults MUST use integer string representation `"0"` to ensure cross-platform SQLite normalization consistency and prevent Room Identity Hash mismatches. (July.18.00 / Issue #096)
*   **Startup IO Offloading Authority (R955)**: All operations that trigger database opening or initial persistence loading (e.g., `loadInitialData`) MUST be executed on `Dispatchers.IO`. This prevents Room migrations or heavy disk reads from starving the Main thread during the cold start landing page sequence. (v9.3.55 / Issue #096)
*   **Landing Page Event Suppression (R954)**: To ensure state purity and minimize startup contention, the system MUST reject all frame notices and telemetry events while the Landing Page is active. (v9.3.52)
*   **Data Flow Offloading Authority (R953)**: To ensure UI responsiveness and prevent ANRs, all mapping operations from database entities to UI models MUST be offloaded to `Dispatchers.Default` using `.flowOn()` at the Repository level. (v9.3.52 / Issue #092)
*   **Reactive Setup Flow Authority (R952)**: The system MUST implement a reactive transition from the Landing Page to the operational mode. (v9.3.45 / Issue #095)
*   **Differential IPC Polling Authority (R951)**: To prevent main thread starvation (ANRs) on low-end hardware, the system MUST use differential polling for permissions. (v9.3.45 / Issue #095)
*   **Non-Blocking System API Authority (R406)**: The system MUST NOT execute system IPC calls using `runBlocking` or on the Main thread. All system status checks MUST be implemented as `suspend` functions and offloaded to `Dispatchers.IO`. (v9.3.30 / Issue #092)
*   **Automatic Mode Transition Delay (R925)**: The app MUST pause for two seconds (`LANDING_PAGE_PAUSE_MS`) on the landing page before automatically entering Tracker or Viewer mode. (Issue #092 / v9.3.36)
*   **Unified System Heartbeat (R403/R405)**: The system MUST use a standardized 2000ms heartbeat (`TICK_INTERVAL_MS`). (v9.3.20)
*   **Type Safety Authority (R999)**: All internal telemetry, sensor data, and engine pipelines MUST use `Double` precision. (Issue #077 / v9.3.15)
*   **Binary Telemetry Authority (R988)**: The system MUST prioritize binary Protobuf-based telemetry for high-frequency tracker updates to minimize relay bandwidth. (v9.3.25)
*   **Alias-Aware Identity Uniqueness (R182b)**: Tracker and Viewer IDs MUST NOT conflict with reserved legacy aliases (`T`, `V`, `Trk`, `viewer`). (v9.3.25)
*   **Samsung A15 Hardening Authority (R405)**: The system MUST prioritize background persistence on Samsung A15 devices via battery optimization prompts, unified heartbeat, and hardware sensor subscriptions. (v9.3.25)
*   **Relay Configuration Authority (R404)**: The system MUST enforce a centralized relay configuration via `MainRepository.DEFAULT_RELAY_URL`. (v9.3.18)
*   **Forensic Visual Authority (R404b)**: The system MUST use a standardized `FORENSIC_PINK_COLOR` (#FF1493) for all forensic events. (v9.3.18)
*   **System API Synchronization Authority (R999b)**: The background service layer MUST maintain strict signature parity with the `:core:engine` telemetry pipeline. (Issue #079 / v9.3.16)
*   **Map Follow Mode Persistence (R981b)**: The map system MUST respect the user's manual focus intent by persisting a `MapFollowMode` state. (Issue #078 / v9.3.16)
*   **System API Throttling (R998)**: The system MUST enforce a minimum 10,000ms TTL cache for all system API permission checks. (Issue C-068-1 / v9.3.14)
*   **Logcat Forensic Integrity (R996)**: The system MUST use cached package identifiers to prevent logcat spillage. (Issue #068 / v9.3.11)
*   **Signaling Pulse Acknowledgement (R995)**: The Tracker MUST explicitly acknowledge Viewer signaling pulses. (Issue #073 / v9.3.10)
*   **Background Resilience Health Check (R997)**: The system MUST provide a dedicated Diagnostics interface for permissions and hardware states. (Issue #059 / v9.3.11)
*   **Service Component Injection (R978)**: All core service components MUST be field-injected via Hilt. (Issue #058 / v9.3.12)
*   **Peer Activity HUD Authority (R980)**: The `GlobalStatusBar` MUST use role-specific freshness logic for peer badges. (Issue #074 / v9.3.12)
*   **Map Marker Stability Authority (R981)**: The map system MUST use the `optimizedPoint` for all remote marker updates. (Issue #072 / v9.3.8)
*   **Identity Rejection Feedback (R977)**: The system MUST provide explicit UI feedback when settings updates are rejected. (Issue #039 / v9.3.4)
*   **Sanitization Visibility (R976)**: The system MUST provide a UI notification when malformed IDs are reset. (Issue #042 / v9.2.2)
*   **Forensic Logging Authority (R979)**: The system MUST use a standardized `ForensicLogUseCase` for Pink logging. (Issue #061 / v9.3.12)
*   **Map Metadata Alignment (R400)**: Map status messages MUST be anchored to the bottom-center. (Issue #400 / v9.3.0)
*   **Standardized Proto Path Authority (R973)**: All Protobuf schemas MUST be located in `app/src/main/proto`. (Issue #030 / v9.3.0)
*   **Screen-Off Optimization Authority (R994)**: The system MUST reduce GPS polling frequency when the screen is off. (Issue R994 / v9.2.9)
*   **Notification Throttling Authority (R993)**: The system MUST throttle foreground service notification updates. (Issue R993 / v9.2.8)
*   **HUD Local Capability Grouping (R960)**: The `GlobalStatusBar` MUST group fundamental local hardware indicators. (Issue R960 / v9.2.7)
*   **HUD Context Mapping Authority (R049)**: The `GlobalStatusBar` MUST implement mode-aware telemetry binding. (Issue #049 / v9.2.6)
*   **HUD Local Health Standardization (R991)**: The top-level HUD status badges MUST reflect the Local Health. (Issue #044 / v9.2.3)
*   **Intelligent Uncertainty UX (R326)**: The system MUST provide specific contextual reasons for Bayesian uncertainty expansion. (Issue #326 / v9.2.2)
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies.
*   **Sensor Processing Authority (R965)**: `AppSensorManager` offloads high-frequency sensor event processing to a dedicated thread.
*   **Stationary Anchor Hard-Lock (R990b)**: The engine MUST establish a coordinate \"Hard-Lock\" when stationary. (Issue #018 / v9.2.1)
*   **Connectivity Integrity (R966)**: `AppNetworkManager` implements reactive reconnection.
*   **Transport Authority**: The system strictly enforces `websocket` transport.
*   **Service Launch Integrity (R926)**: The system enforces a mandatory 2,000ms delay during session transitions.
*   **Log Spillage Remediation (Issue #005)**: The system mandates static User Agent and manually defined storage paths.
*   **Time Integrity**: All alarm evaluations use monotonic time.
*   **Bootstrap Staggering (R984)**: All background services MUST implement a staggered initialization sequence.
*   **Foreground Service Hardening (R983)**: The system strictly enforces state-aware foreground service types.
*   **Foreground Service Transition (R967)**: The system maintains a 45-second \"Recent UI Pulse\" window.
*   **Data Persistence Integrity (R968)**: All changes to Protobuf schemas must preserve binary compatibility. (Issue #076 / v9.3.12)
*   **Database Schema Hardening (R985)**: Room Entity fields MUST be decorated with explicit `@ColumnInfo(defaultValue)`.
*   **Authoritative State Model (R986)**: The `TrackerState` MUST be computed exclusively by the Tracker.
*   **Binary Telemetry Authority (R988)**: The binary telemetry contract MUST maintain field parity with authoritative state flow.
*   **Speed Unit Standardization (R987)**: All internal telemetry and engine pipelines MUST use raw meters per second (m/s).
*   **HUD Freshness Duality (R989)**: The HUD MUST differentiate between Telemetry Freshness and GPS Freshness. (Issue R989 / v9.2.0)
*   **Telemetry Freshness Authority (Issue #029)**: Data health (`DAT` badge) is determined by arrival of any packet.
*   **Document Integrity Authority (R969)**: Core documentation is subject to a Growth-Only Constraint.
*   **A15 Jitter Stabilization (R970)**: The system applies hardened spatial gates and 5s muzzle on Samsung A15.
*   **G990E Display Hardening (R971)**: The system muzzles proximity transitions during ON/DOZE toggling on S21 FE.
*   **Forensic Staleness Authority (R972)**: The system enforces a strict 15-second staleness gate for forensic fields.
*   **Identity Uniqueness Authority (R974)**: The system enforces uniqueness between Tracker and Viewer identities.
*   **Identity Sanitization Authority (R975)**: The system enforces a strict alphanumeric contract for all IDs.
*   **Identity Locking Authority (R982)**: The system enforces strict peer-to-peer authorization.
*   **Temporal Authority (#075)**: The system MUST use receipt-time deltas for skew-immune GPS freshness. (v9.3.12)

### 2. Branding & UI Standards
*   **Branding Authority (R799e)**: \"Unified Identity Green\" is JD Vivid Green (#78BE20).
*   **Viewer Identity Color (R799d)**: \"Viewer Role Identity\" is Cyan (#06B6D4).
*   **Icon Authority (R935)**: The authoritative application icon is the deer logo.
*   **Role Identity Standards (R182)**: IDs are free-form strings. Prefixes \"T\" and \"V\" mandated.
