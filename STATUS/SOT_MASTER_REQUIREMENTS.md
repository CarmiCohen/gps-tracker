# System Source of Truth (SoT) - v9.3.15

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Type Safety Authority (R999)**: All internal telemetry, sensor data, and engine pipelines MUST use `Double` precision. Redundant `toDouble()`/`toFloat()` conversions MUST be eliminated by capturing system API values (Floats) exactly once at the entry boundary. All persistence and signaling fields MUST maintain `Double` parity to prevent precision jitter. (Issue #077 / v9.3.15 Verified)
*   **System API Throttling (R998)**: The system MUST enforce a minimum 10,000ms TTL (Time-To-Live) cache for all system API permission and status checks. This is CRITICAL to eliminate internal logcat jitter on Samsung G990/A15 and Xiaomi devices. (Issue C-068-1 / v9.3.14 Verified)
*   **Dynamic Anchor Breakout (R990c)**: The engine MUST implement a displacement-weighted monitor to prevent "sticky anchors". This monitor MUST integrate cumulative displacement trends, IMM-filter velocity estimates, and physical motion triggers. (Issue #062 / v9.3.13 Verified)
*   **Logcat Forensic Integrity (R996)**: The system MUST use cached package identifiers in all high-frequency execution paths to prevent `getPackageName` logcat spillage on Samsung/Xiaomi devices. (Issue #068 / v9.3.11 Verified)
*   **Signaling Pulse Acknowledgement (R995)**: The Tracker MUST explicitly acknowledge Viewer signaling pulses by updating the local remote activity timestamp. (Issue #073 / v9.3.10 Verified)
*   **Background Resilience Health Check (R997)**: The system MUST provide a dedicated Diagnostics interface to monitor critical background permissions and hardware-specific states. (Issue #059 / v9.3.11 Verified)
*   **Service Component Injection (R978)**: All core service components MUST be field-injected via Hilt and initialized using a standardized `Listener` pattern. (Issue #058 / v9.3.12 Verified)
*   **Peer Activity HUD Authority (R980)**: The `GlobalStatusBar` MUST use role-specific freshness logic for peer badges. (Issue #074 / v9.3.12 Verified)
*   **Map Marker Stability Authority (R981)**: The map system MUST use the `optimizedPoint` from `LocationProcessor` for all remote marker updates. (Issue #072 / v9.3.8 Verified)
*   **Identity Rejection Feedback (R977)**: The system MUST provide explicit UI feedback when settings updates or commits are rejected due to identity collisions. (Issue #039 / v9.3.4 Verified)
*   **Sanitization Visibility (R976)**: The system MUST provide a UI notification when malformed Tracker or Viewer IDs are automatically reset. (Issue #042 / v9.3.2 Verified)
*   **Forensic Logging Authority (R979)**: The system MUST use a standardized `ForensicLogUseCase` for all high-visibility (Pink) logging. (Issue #061 / v9.3.12 Verified)
*   **Map Metadata Alignment (R400)**: Map-level status messages MUST be anchored to the bottom-center of the map view. (Issue #400 / v9.3.0 Verified)
*   **Standardized Proto Path Authority (R973)**: All Protobuf schemas MUST be located in `app/src/main/proto`. (Issue #030 / v9.3.0 Verified)
*   **Screen-Off Optimization Authority (R994)**: The system MUST optimize power consumption by reducing GPS polling frequency when the device screen is off. (Issue R994 / v9.2.9 Verified)
*   **Notification Throttling Authority (R993)**: The system MUST throttle foreground service notification updates. (Issue R993 / v9.2.8 Verified)
*   **HUD Local Capability Grouping (R960)**: The `GlobalStatusBar` MUST group fundamental local hardware indicators. (Issue R960 / v9.2.7 Verified)
*   **HUD Context Mapping Authority (R049)**: The `GlobalStatusBar` MUST implement mode-aware telemetry binding. (Issue #049 / v9.2.6 Verified)
*   **HUD Local Health Standardization (R991)**: The top-level HUD status badges MUST reflect the Local Health. (Issue #044 / v9.2.3 Verified)
*   **Intelligent Uncertainty UX (R326)**: The system MUST provide specific contextual reasons for Bayesian uncertainty expansion. (Issue #326 / v9.2.2 Verified)
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies.
*   **Sensor Processing Authority (R965)**: `AppSensorManager` offloads high-frequency sensor event processing to a dedicated thread.
*   **Stationary Anchor Hard-Lock (R990b)**: The engine MUST establish a coordinate "Hard-Lock" when stationary (`stationaryProb > 0.9`). (Issue #018 / v9.2.1)
*   **Anchor Lock Breakout (R990)**: The engine MUST implement a displacement-weighted monitor to breakout of Hard-Locks. (Issue #062 / v9.3.6)
*   **Connectivity Integrity (R966)**: `AppNetworkManager` implements reactive reconnection.
*   **Transport Authority**: The system strictly enforces `websocket` transport.
*   **Service Launch Integrity (R926)**: The system enforces a mandatory 2,000ms delay during session transitions.
*   **Log Spillage Remediation (Issue #005)**: The system mandates static User Agent and manually defined storage paths.
*   **Time Integrity**: All alarm evaluations use monotonic time.
*   **Bootstrap Staggering (R984)**: All background services MUST implement a staggered initialization sequence.
*   **Foreground Service Hardening (R983)**: The system strictly enforces state-aware foreground service types.
*   **Foreground Service Transition (R967)**: The system maintains a 45-second "Recent UI Pulse" window.
*   **Data Persistence Integrity (R968)**: All changes to Protobuf schemas must preserve binary compatibility. (Issue #076 / v9.3.12 Verified)
*   **Database Schema Hardening (R985)**: Room Entity fields MUST be decorated with explicit `@ColumnInfo(defaultValue)`.
*   **Authoritative State Model (R986)**: The `TrackerState` MUST be computed exclusively by the Tracker.
*   **Binary Forensic Parity (R988)**: The binary telemetry contract MUST maintain field parity with authoritative state flow.
*   **Speed Unit Standardization (R987)**: All internal telemetry and engine pipelines MUST use raw meters per second (m/s).
*   **HUD Freshness Duality (R989)**: The HUD MUST differentiate between Telemetry Freshness and GPS Freshness. (Issue R989 / v9.2.0 Verified)
*   **Telemetry Freshness Authority (Issue #029)**: Data health (`DAT` badge) is determined by arrival of any packet.
*   **Document Integrity Authority (R969)**: Core documentation is subject to a Growth-Only Constraint.
*   **A15 Jitter Stabilization (R970)**: The system applies hardened spatial gates and 5s muzzle on Samsung A15.
*   **G990E Display Hardening (R971)**: The system muzzles proximity transitions during ON/DOZE toggling on S21 FE.
*   **Forensic Staleness Authority (R972)**: The system enforces a strict 15-second staleness gate for forensic fields.
*   **Identity Uniqueness Authority (R974)**: The system enforces uniqueness between Tracker and Viewer identities.
*   **Identity Sanitization Authority (R975)**: The system enforces a strict alphanumeric contract for all IDs.
*   **Identity Locking Authority (R982)**: The system enforces strict peer-to-peer authorization.
*   **Temporal Authority (#075)**: The system MUST use receipt-time deltas for skew-immune GPS freshness. (v9.3.12 Verified)

### 2. Branding & UI Standards
*   **Branding Authority (R799e)**: "Unified Identity Green" is JD Vivid Green (#78BE20).
*   **Viewer Identity Color (R799d)**: "Viewer Role Identity" is Cyan (#06B6D4).
*   **Icon Authority (R935)**: The authoritative application icon is the deer logo.
*   **Role Identity Standards (R182)**: IDs are free-form strings. Prefixes "T" and "V" mandated.
