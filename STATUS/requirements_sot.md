# System Source of Truth (SoT) - v8.9.52

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

## 1. Core Architectural Baselines
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies. (Issue #322)
*   **Time Integrity**: All alarm evaluations and hardware latches (Siren, Silence, Muzzles) use monotonic time via `TimeProvider.elapsedRealtime()`. (Issue #311 / Issue #441)
*   **OS Compatibility**: Authoritative baselines are **minSdk 24** and **targetSdk 35**. (v8.9.42)
*   **Reactive System State Flows**: Implementation of cold-to-hot reactive flows for system health (Battery, Internet, Permissions) to ensure UI and forensic layers reflect real-time hardware status without polling. (Issue #404)

## 2. Branding & UI Standards
*   **Branding Authority (R865/R866)**: "Unified Identity Green" is strictly defined as **JD Branding Green (#367C2B)**. All tracker-role UI elements, icons, and status indicators must use this hex code. 
*   **Role Identity Standards (R182)**: IDs are free-form strings (min length 3). Tracker and Viewer IDs must be unique from each other. Prefixes "T" (Tracker) and "V" (Viewer) are suggested defaults but not enforced. (v8.9.47 relaxation)
*   **Ghost Mode UI (R338)**: Visual staleness indicators (dimming to `Slate500`) applied when telemetry > 10s old (`TELEMETRY_UI_STALE_THRESHOLD_MS`). (Issue #338)
*   **UI Link Health & Watchdog (Issue #427)**: The "OK/FAIL" watchdog status and link freshness indicators utilize a strict **10s** threshold (`WATCH_TIMEOUT_MS`) to maintain UX symmetry with Ghost Mode.
*   **Predictive Back Navigation**: `AlarmActivity` utilizes `BackHandler` and manifest `enableOnBackInvokedCallback=true` for standardized dismissal. (R800)
*   **Standardized Alert Titles**: Authority resides in `EngineConstants.kt` (e.g., "This device: Internet Lost", "Offline"). (Issue #424)
*   **Indicator Status**: The "LOCKOUT" badge in the StatusBar is the authoritative indicator for suppressed alarm states. (Issue #400)

## 3. Location & Trajectory Engine
*   **Authoritative Spatial Anchoring (R325)**: **`maxAccuracy` is the exclusive authority** for Geofence transitions, Distance Violations, and Trajectory Deduplication. Includes a **0.5x spatial gate factor** for persistence sensitivity (`DEDUPLICATION_SPATIAL_GATE_FACTOR`). (Issue #325 / Issue #450)
*   **Uncertainty Hindsight (R334)**: Linear interpolation of `accuracy` and `maxAccuracy` is mandatory for all rubber-band path segments during hindsight promotion. **Authoritative Buffer Size: 10 points** (`HINDSIGHT_BUFFER_SIZE`). (Issue #435)
*   **Bayesian Uncertainty Growth (Issue #328 / Issue #431)**: Uncertainty expands at **15.0m/s** (Moving) and **1.5m/s** (Stationary) when location fixes are pending, with a safety cap of **33.3m/s**. This expansion is strictly synchronized between the UI visualization and the alert engine breach detection.
*   **Adaptive Jump Confidence (Issue #332)**: Logic penalizes High-SNR signals (≥ 35.0f) coupled with zero vibration to detect signal reflections (Urban Canyons). Includes a **2.0x hold multiplier** for sustained jump gating. (Issue #411)
*   **GtoEngine Optimization**: Authorization for Towing (10m/s) and Work (5m/s) speed thresholds. (Issue #264 / Issue #433)
*   **ImmFilter Parameters**: Core probability gates are **0.8** (Stationary) and **0.2** (Kinematic) with a 5.0m measurement noise floor. (Issue #408)
*   **GPS Stability Authority**: Mandatory **98.0% reliability** threshold over a **10s** audit window. (Issue #407)
*   **Jump Engine Gates**: Jitter (10m), Security Jump (100m), Cold-Start Outlier (2000m), Vertical Velocity (5.0m/s), and Sensor Mismatch (10.0m/s).
*   **Jump Latch Duration**: Violation state is sustained for **180,000ms** (3 minutes) via `JUMP_HOLD_DURATION_MS` to prevent alert flickering. (Issue #405)
*   **GPS Polling & Persistence Intervals**:
    *   **Stationary Polling**: 20,000ms.
    *   **Stationary Persistence**: 20,000ms (`GPS_SAVE_INTERVAL_MS`). (Issue #436)
    *   **Moving**: 200ms.
    *   **Suspicious**: 1,000ms.
    *   **High Frequency**: 100ms.

## 4. Hardware & Forensic Sentinel
### 4.1. Location Sentinel Thresholds
*   **Lift**: 0.8m Δ Altitude (`BARO_LIFT_THRESHOLD_METERS`).
*   **Tilt**: 15° Δ Orientation (`TILT_THRESHOLD_DEGREES`).
*   **Light Jump**: 150 Lux Δ (`LIGHT_THRESHOLD_LUX_JUMP`).
*   **Acoustic**: 40dB Alert / 20dB Suspicious. Absolute floor 50dB.
*   **Vibration**: 0.8g (Shock) / 0.25g (Suspicious).
*   **Acoustic Hysteresis**: Engine recovery gate resets at **30s**.
*   **Muzzle Window**: 2000ms logic gate for sync suppression, with **200ms** recovery hysteresis. (Issue #376)

### 4.2. Chair Sit Detection Engine (R832)
*   **Fusion Logic**: Tilt (7.0°), Vibration (0.35g), Baro Plunge (0.08m), Velocity (0.18m/s).
*   **Timing Gates**: Plunge phase timeout **1500ms**, Duplicate guard **15,000ms**. (Issue #336/Issue #412)

### 4.3. Device-Specific Adaptations
*   **Xiaomi System Ready**: Hardened gating using `is_xiaomi_manual_override` and autostart verification. Includes **15s suppression** and **60s recovery** gates for heuristic revival. (Issue #190 / Issue #439)
*   **Samsung A15**: Proximity debounce (5000ms) and post-sync hysteresis (500ms).
*   **Samsung S21 FE**: Explicit background activity required for 10Hz GPS polling. (Issue #432)

## 5. System Health & Forensics
*   **Database Schema (v50)**: Authoritative forensic baseline including Dual-Metric accuracy and Power Forensics (`currentMa`). (Issue #337)
*   **Storage Watchdog**: Critical (< 10MB) Log Muzzle; Low (< 50MB) Throttled Logging.
*   **Alert Authority**: Battery (20%), Temp (46.0°C), Signal Loss (Tracker 180s / Viewer 30s). (Issue #381/Issue #382)
*   **Siren Engine**: Authority for **30s** auto-stop (Issue #429) and **15s** resume cooldown. Silence latches use monotonic time. (Issue #441)
*   **Timing Authority**: Startup log muzzle **10s**, Heartbeat interval **1h**, Marker pruning threshold **50**. (Issue #440)
*   **Binary Signaling (R944)**: Mandatory use of Protobuf for all signaling events. (Issue #404)
*   **Forensic Snapshots**: Mandatory inclusion of `snrSnapshot` and `vibeSnapshot` in log entries. (Issue #333)
