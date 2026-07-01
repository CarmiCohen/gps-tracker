# System Source of Truth (SoT) - v8.9.65

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies. (Issue #322)
*   **Sensor Processing Authority (R965)**: `AppSensorManager` offloads all high-frequency sensor event processing to a dedicated `HandlerThread` (`AppSensorThread`) to ensure Main Thread fluidity and eliminate vsync jitter. (Issue #006 / v8.9.64)
*   **Connectivity Integrity (R966)**: `AppNetworkManager` implements a short-circuit reactive reconnection trigger. The system initiates immediate `wakeUpRelay()` and signaling re-join upon transport failure, bypassing passive loops. (Issue #007 / v8.9.64)
*   **Transport Authority**: The system strictly enforces `websocket` transport for low-latency signaling. (Issue #007 / v8.9.64)
*   **Service Launch Integrity (R926)**: The system enforces a mandatory **2,000ms delay** during session auto-transitions before launching background services. (Issue #320; Supersedes legacy Issue #215 and legacy ID R925)
*   **Time Integrity**: All alarm evaluations and hardware latches (Siren, Silence, Muzzles) use monotonic time via `TimeProvider.elapsedRealtime()`. (Issue #311 / Issue #441)
*   **OS Compatibility**: Authoritative baselines are **minSdk 24** and **targetSdk 35**. (v8.9.42)
*   **Reactive System State Flows (R945)**: Implementation of cold-to-hot reactive flows for system health (Battery, Internet, Permissions) to ensure UI and forensic layers reflect real-time hardware status without polling. (Issue #404)
*   **Watchdog Battery Optimization (R946)**: Conservative `AlarmManager` rescheduling utilizing exact alarms only when critical to system stability.

### 2. Branding & UI Standards
*   **Branding Authority (R865/R866)**: "Unified Identity Green" is strictly defined as **JD Branding Green (#367C2B)**. All tracker-role UI elements, icons, and status indicators must use this hex code. 
*   **Icon Authority (R935)**: The authoritative application icon is the text-free John Deere deer logo.
*   **Role Identity Standards (R182)**: IDs are free-form strings. Tracker and Viewer IDs must be unique from each other. Prefixes "T" (Tracker) and "V" (Viewer) are mandated standards for system-generated defaults. (v8.9.47/v8.9.53 alignment)
*   **VID Notes Authority (R924)**: The `HeaderBar` button row displays the hard-coded `VID_NOTES` string (authoritative baseline: "renumv") instead of the application version number. (v8.9.65 alignment)
*   **Status Badge Logic (R942)**: Dynamic labels in the StatusBar (TRK/VWR) reflect the current session role and remote peer identity. In Tracker mode, the badge represents the remote Viewer.
*   **UI Badge Redundancy (R943)**: The "DAT" (Data) badge is suppressed in Tracker mode to maintain UI cleanliness when signaling is unidirectional.
*   **Ghost Mode UI (R338)**: Visual staleness indicators (dimming to `Slate500`) applied when telemetry > 35s old (`TELEMETRY_UI_STALE_THRESHOLD_MS`). (v8.9.62 alignment; Formerly 15s)
*   **Visual Watchdog Logic (R964)**: The "OK/FAIL" watchdog status and link freshness indicators utilize a **15s** threshold (`WATCH_TIMEOUT_MS`) to maintain UX stability against network jitter. (Issue #427)
*   **Predictive Back Navigation**: `AlarmActivity` utilizes `BackHandler` and manifest `enableOnBackInvokedCallback=true` for standardized dismissal. (R800)
*   **Standardized Alert Titles (R747)**: Authority resides in `EngineConstants.kt`. Localized "This device" prefix for local events; "Tracker:" prefix removed from remote events. Subtitles standardized to use "Device" instead of "Tracker". (Issue #424)
*   **Lockout Indicator Authority (R959)**: The "LOCKOUT" badge in the StatusBar is the authoritative indicator for suppressed alarm states. (Issue #400)
*   **Accuracy Display Optimization (R325)**: Status telemetry layout is optimized for narrow devices (min 360dp) by constraining the left-side panel to **210dp**, ensuring authoritative accuracy `(±Xm)` remains visible without truncation. (Issue #R325 / v8.9.65)

### 3. Location & Trajectory Engine
*   **Authoritative Spatial Anchoring (R325)**: **`maxAccuracy` is the exclusive authority** for Geofence transitions, Distance Violations, and Trajectory Deduplication. Includes a **0.5x spatial gate factor** for persistence sensitivity (`DEDUPLICATION_SPATIAL_GATE_FACTOR`). (Issue #423 / Issue #450)
*   **Instant Recovery (R923)**: Freshness logic utilizes the maximum of the GPS timestamp and the telemetry arrival timestamp to determine real-time device proximity.
*   **Uncertainty Hindsight (R334)**: Linear interpolation of `accuracy` and `maxAccuracy` is mandatory for all rubber-band path segments during hindsight promotion. **Authoritative Buffer Size: 10 points** (`HINDSIGHT_BUFFER_SIZE`). (Issue #461 / Formerly #435)
*   **Bayesian Uncertainty Growth (R460)**: Uncertainty expands at **15.0m/s** (Moving) and **1.5m/s** (Stationary) when location fixes are pending, with a safety cap of **33.3m/s**. This expansion is strictly synchronized between the UI visualization and the alert engine breach detection. (Issue #460 / Formerly #431)
*   **Adaptive Jump Confidence (R332)**: Logic penalizes High-SNR signals (≥ 35.0f) coupled with zero vibration to detect signal reflections (Urban Canyons). Includes a **2.0x hold multiplier** for sustained jump gating. (Issue #452 / Formerly #332)
*   **GtoEngine Optimization (R950)**: Authorization for Towing (10m/s) and Work (5m/s) speed thresholds. (Issue #264 / Issue #433)
*   **ImmFilter Parameters (R960)**: Core probability gates are **0.8** (Stationary) and **0.2** (Kinematic) with a 5.0m measurement noise floor. (Issue #408)
*   **GPS Stability Authority (R951)**: Mandatory **98.0% reliability** threshold over a **10s** audit window. (Issue #407)
*   **Jump Engine Gates (R952)**: Jitter (10m), Security Jump (100m), Cold-Start Outlier (2000m), Vertical Velocity (5.0m/s), and Sensor Mismatch (10.0m/s).
*   **Jump Latch Duration**: Violation state is sustained for **180,000ms** (3 minutes) via `JUMP_HOLD_DURATION_MS` to prevent alert flickering. (Issue #405)
*   **GPS Duty Cycles (R961)**:
    *   **Stationary Polling**: 20,000ms.
    *   **Stationary Persistence**: 20,000ms (`GPS_SAVE_INTERVAL_MS`). (Issue #436)
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
*   **Muzzle & Sync Suppression (R954)**: 2000ms logic gate for sync suppression, with **200ms** recovery hysteresis to prevent race conditions during alert clearing. (Issue #376)

#### 4.2. Chair Sit Detection Engine (R832)
*   **Fusion Logic**: Tilt (7.0°), Vibration (0.35g), Baro Plunge (0.08m), Velocity (0.18m/s).
*   **Timing Gates**: Plunge phase timeout **1500ms**, Duplicate guard **15,000ms**. (Issue #459 / Formerly #336-E and #336-B)

#### 4.3. Device-Specific Adaptations
*   **Xiaomi Heuristic Revival (R955)**: Hardened gating using `is_xiaomi_manual_override` and autostart verification. Includes **15s suppression** and **60s recovery** gates for heuristic revival. (Issue #190 / Issue #439)
*   **Samsung A15 Proximity Workaround (R963)**: Proximity debounce (5000ms) and post-sync hysteresis (500ms) to accommodate virtual proximity sensor limitations. Includes Lux-Aware Proximity Gating and Stationary Virtual Sensor Protection (suppressing 'Far' transitions in darkness). (v8.9.64 alignment)
*   **Samsung S21 FE**: Explicit background activity required for 10Hz GPS polling. (Issue #432)

### 5. System Health & Lifecycle
*   **Update Smoothness (R917)**: The system maintains session continuity across version updates using `MY_PACKAGE_REPLACED` recovery infrastructure. (Issue #317; Supersedes preliminary Issue #175)
*   **Database Schema (v51)**: Authoritative forensic baseline including Dual-Metric accuracy and Power Forensics (`currentMa`). (Issue #001 / v8.9.62)
*   **Storage Watchdog (R957)**: Critical (< 10MB) Log Muzzle; Low (< 50MB) Throttled Logging to prevent storage exhaustion during forensic bursts.
*   **Alert Authority**: Battery (20%), Temp (46.0°C), Signal Loss (Tracker 180s / Viewer 35s). (Issue #381/Issue #382; Issue #002 alignment)
*   **Siren Persistence & Cooldown (R956)**: Authority for **30s** auto-stop (Issue #429) and **15s** resume cooldown. Silence latches use monotonic time. (Issue #441)
*   **Timing Authority**: Startup log muzzle **10s**, Heartbeat interval **1h**, Marker pruning threshold **50**. (Issue #440)
*   **Binary Signaling (R944)**: Mandatory use of Protobuf for all signaling events. (Issue #404)
*   **Forensic Metadata Snapshots (R958)**: Mandatory inclusion of `snrSnapshot` and `vibeSnapshot` in log entries to allow post-mortem signal reflection analysis. (Issue #333)
