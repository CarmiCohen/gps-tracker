# System Source of Truth (SoT) - v8.9.78

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies. (Issue #322)
*   **Sensor Processing Authority (R965)**: `AppSensorManager` offloads all high-frequency sensor event processing to a dedicated `HandlerThread` (`AppSensorThread`). (Issue #006 / Issue #013 / v8.9.70)
*   **Connectivity Integrity (R966)**: `AppNetworkManager` implements a short-circuit reactive reconnection trigger. (Issue #007 / v8.9.64)
*   **Transport Authority**: The system strictly enforces `websocket` transport for low-latency signaling. (Issue #007 / v8.9.64)
*   **Service Launch Integrity (R926)**: The system enforces a mandatory **2,000ms delay** during session auto-transitions before launching background services. (Issue #320)
*   **Time Integrity**: All alarm evaluations and hardware latches use monotonic time via `TimeProvider.elapsedRealtime()`. (Issue #311 / Issue #441)
*   **OS Compatibility**: Authoritative baselines are **minSdk 24** and **targetSdk 35**. (v8.9.42)
*   **Reactive System State Flows (R945)**: Implementation of cold-to-hot reactive flows for system health (Battery, Internet, Permissions). (Issue #404)
*   **Watchdog Battery Optimization (R946)**: Conservative `AlarmManager` rescheduling utilizing exact alarms only when critical.
*   **Foreground Service Transition (R967)**: The system maintains a **15-second "Recent UI Pulse" window** (`UI_PULSE_TIMEOUT_MS`) to bridge Android 14+ `MICROPHONE` type transitions. (Issue #019 / v8.9.74)
*   **Type Safety Authority**: All telemetry fields (Accuracy, Speed, Bearing, Sensor Indices) are standardized to `Double` across the entire chain (Engine, App, Room). (Issue #014 / v8.9.75)

### 2. Branding & UI Standards
*   **Branding Authority (R865/R866)**: "Unified Identity Green" is strictly defined as **JD Branding Green (#367C2B)**.
*   **Icon Authority (R935)**: The authoritative application icon is the text-free John Deere deer logo.
*   **Role Identity Standards (R182)**: IDs are free-form strings. Prefixes "T" (Tracker) and "V" (Viewer) are mandated.
*   **VID Notes Authority (R924)**: The `HeaderBar` button row displays the hard-coded `VID_NOTES` string (authoritative baseline: "Th1030").
*   **Status Badge Logic (R942)**: Dynamic labels in the StatusBar (TRK/VWR) reflect the current session role.
*   **UI Badge Redundancy (R943)**: The "DAT" (Data) badge is suppressed in Tracker mode.
*   **Ghost Mode UI (R338)**: Visual staleness indicators (dimming to `Slate500`) applied when telemetry > 35s old. (v8.9.62)
*   **Visual Watchdog Logic (R964)**: The "OK/FAIL" watchdog status utilizes a **15s** threshold. (Issue #427)
*   **Predictive Back Navigation**: `AlarmActivity` utilizes `BackHandler` and manifest `enableOnBackInvokedCallback=true`. (R800)
*   **Standardized Alert Titles (R747)**: Authority resides in `EngineConstants.kt`. Localized "This device" prefix. (Issue #424)
*   **Lockout Indicator Authority (R959)**: The "LOCKOUT" badge in the StatusBar is the authoritative indicator for suppressed alarm states. (Issue #400)
*   **Accuracy Display Optimization (R325)**: Status telemetry layout is optimized for narrow devices by constraining the left-side panel to **210dp**. (Issue #R325 / v8.9.65)
*   **Forensic UI Visibility (R729)**: The system must expose adaptive metrics `proximityDebounceMs` and `vibrationRollingSum` to the UI dashboard. (Issue #013 / v8.9.71)
*   **Anchor Lock Visibility**: The system provides forensic UI visibility (dashboard/badge) when the Stationary Anchor Hard-Lock is active. (Issue #018 / v8.9.78)

### 3. Location & Trajectory Engine
*   **Authoritative Spatial Anchoring (R325)**: **`maxAccuracy` is the exclusive authority** for Geofence transitions, Distance Violations, and Trajectory Deduplication. (Issue #423 / Issue #450)
*   **Stationary Anchor Hard-Lock (R018)**: To eliminate coordinate drift in Urban Canyons, coordinates are strictly clamped to a fixed `parkingAnchorPoint` when `stationaryProb > 0.9`. Breakout occurs if spatial displacement exceeds `max(20m, 0.8x accuracy)`. (Issue #018 / v8.9.78)
*   **Instant Recovery (R923)**: Freshness logic utilizes the maximum of the GPS timestamp and the telemetry arrival timestamp.
*   **Uncertainty Hindsight (R334)**: Linear interpolation of `accuracy` and `maxAccuracy` is mandatory for hindsight segments. (Issue #461)
*   **Bayesian Uncertainty Growth (R460)**: Uncertainty expands at **15.0m/s** (Moving) and **1.5m/s** (Stationary) with a safety cap of **33.3m/s**. (Issue #460)
*   **Adaptive Jump Confidence (R332)**: Logic penalizes High-SNR signals (≥ 35.0f) coupled with zero vibration. (Issue #452)
*   **GtoEngine Optimization (R950)**: Authorization for Towing (10m/s) and Work (5m/s) speed thresholds. (Issue #264 / Issue #433)
*   **ImmFilter Parameters (R960)**: Core probability gates are **0.8** (Stationary) and **0.2** (Kinematic). (Issue #408)
*   **GPS Stability Authority (R951)**: Mandatory **98.0% reliability** threshold over a **10s** audit window. (Issue #407)
*   **Jump Engine Gates (R952)**: Jitter (10m), Security Jump (100m), Cold-Start Outlier (2000m), Vertical Velocity (5.0m/s), and Sensor Mismatch (10.0m/s).
*   **Jump Latch Duration**: Violation state is sustained for **180,000ms** (3 minutes). (Issue #405)
*   **GPS Duty Cycles (R961)**:
    *   **Stationary Polling**: 20,000ms.
    *   **Stationary Persistence**: 20,000ms (`GPS_SAVE_INTERVAL_MS`).
    *   **Moving**: 200ms.
    *   **Suspicious**: 1,000ms.
    *   **High Frequency**: 100ms.

### 4. Hardware & Forensic Sentinel
#### 4.1. Location Sentinel Thresholds
*   **Physical Sentinel Deltas (R962)**:
    *   **Lift**: 0.8m Δ Altitude (`BARO_LIFT_THRESHOLD_METERS`).
    *   **Tilt**: 15° Δ Orientation (`TILT_THRESHOLD_DEGREES`).
    *   **Light Jump**: 150 Lux Δ (`LIGHT_THRESHOLD_LUX_JUMP`).
*   **Acoustic Sentinel Thresholds (R953)**: 40dB Jump Alert / 20dB Jump Suspicious. Absolute floor 50dB. (Issue #437)
*   **Vibration**: 0.8g (Shock) / 0.25g (Suspicious).
*   **Acoustic Hysteresis**: Engine recovery gate resets at **30s**.
*   **Muzzle & Sync Suppression (R954)**: 2000ms logic gate for sync suppression. (Issue #376)
*   **Forensic Labeling (R730)**: The system must log service events when hardware muzzles suppress sensor violations. (Issue #011 / v8.9.71)

#### 4.2. Chair Sit Detection Engine (R832)
*   **Fusion Logic**: Tilt (7.0°), Vibration (0.35g), Baro Plunge (0.08m), Velocity (0.18m/s).
*   **Timing Gates**: Plunge phase timeout **1500ms**, Duplicate guard **15,000ms**. (Issue #459)

#### 4.3. Device-Specific Adaptations
*   **Xiaomi Heuristic Revival (R955)**: Hardened gating using manual override and autostart verification. (Issue #190 / Issue #439)
*   **Samsung A15 Proximity Workaround (R963)**: Proximity debounce with adaptive scaling based on stationary duration. (Issue #012 / v8.9.71)
*   **Samsung S21 FE**: Explicit background activity required for 10Hz GPS polling. (Issue #432)

### 5. System Health & Lifecycle
*   **Update Smoothness (R917)**: The system maintains session continuity across version updates using `MY_PACKAGE_REPLACED`. (Issue #317)
*   **Database Schema (v52)**: Authoritative forensic baseline including `isAnchorLocked` flag, Dual-Metric accuracy, and Power Forensics. (Issue #001 / Issue #018 / v8.9.78)
*   **Storage Watchdog (R957)**: Critical (< 10MB) Log Muzzle; Low (< 50MB) Throttled Logging.
*   **Alert Authority**: Battery (20%), Temp (46.0°C), Signal Loss (Tracker 180s / Viewer 35s). (Issue #381/Issue #382; Issue #002 alignment)
*   **Siren Persistence & Cooldown (R956)**: Authority for **30s** auto-stop and **15s** resume cooldown. (Issue #429 / Issue #441)
*   **Timing Authority**: Startup log muzzle **10s**, Heartbeat interval **1h**, Marker pruning threshold **50**. (Issue #440)
*   **Binary Signaling (R944)**: Mandatory use of Protobuf for all signaling events. (Issue #404)
*   **Forensic Metadata Snapshots (R958)**: Mandatory inclusion of `snrSnapshot` and `vibeSnapshot` in log entries. (Issue #333)
