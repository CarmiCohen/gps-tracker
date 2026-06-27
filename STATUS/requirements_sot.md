# System Source of Truth (SoT) - v8.9.42

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

... (sections 1-3 remain the same) ...

## 4. Remote Forensic Verification
### 4.1. Version & Role Visibility
*   **Engine Identity**: The system operates on the v8.9.42 baseline logic.
*   **Branding Authority (R865/R866) [Active]**: The "Unified Identity Green" is strictly defined as **JD Branding Green (#367C2B)**. All tracker-role UI elements, icons, and status indicators must use this hex code. 
*   **R568a: Last Relay Traffic Monotonic Timestamp**: Implementation of `getLastRelayTrafficTs()` in `SignalingProvider` using `elapsedRealtime` to track the last received message for reliable zombie connection detection.
*   **R729: Behavioral Debouncing & Muzzle Hardening**: Unified timing gates for alert suppression (Issue #191). Defines three critical windows: **2000ms** (Global Muzzle suppression), **500ms** (Samsung A15 post-sync hysteresis), and **5000ms** (Samsung A15 proximity debounce).
*   **R730: Unified Vibration Floor Update (EMA)**: Exponential Moving Average logic for dynamic vibration floor tracking. Used to adapt sensitivity to environmental noise while maintaining shock detection integrity.
*   **R800: Unified Back Navigation**: Standardized back button behavior in `AlarmActivity` using `BackHandler` to ensure consistent dismissal of alarms and activity termination.
*   **R805: Map Marker Color Standardization**: Addition of `Purple500` (#800080) in `Color.kt` for specific map marker categorization.
*   **R832: Chair Sit Detection Engine**: Multi-sensor fusion logic in `:core:engine` using tilt, vibration, and barometric plunge thresholds to detect chair occupancy. (Issue #336)
*   **R853: Atomic HomePoint Updates**: Enhanced `SettingsRepository` to support atomic bulk updates of `homePoints` alongside other settings to ensure data consistency.
*   **R854: Siren Master Control**: Grouping of siren-related alert toggles under a unified "Siren Master Control" header in the UI strings for improved administrative clarity.
*   **R880: Evidence-based Parking Exit**: Hardened transition logic in `TrackerStateManager` requiring sustained speed or physical vibration evidence to exit the PARKING state.
*   **R944: Binary Signaling Efficiency**: Mandatory use of binary payloads (Protobuf) for signaling events to ensure minimal overhead and forensic integrity during high-frequency updates. (Issue #404)
*   **R945: Reactive System State Flows**: Implementation of cold-to-hot reactive flows for monitoring system health (Battery, Internet, Permissions) to ensure UI and forensic layers reflect real-time hardware status without polling. (Issue #404)
*   **Dynamic Versioning**: `versionCode` in `build.gradle` is generated using `git rev-list --count HEAD`. (Issue #199)
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Standardized Alert IDs**: Aligned with `EngineConstants.kt`. Includes `VISUAL_JUMP` for trajectory-based jumps.
*   **Xiaomi System Ready**: Hardened gating for Xiaomi devices using `is_xiaomi_manual_override` and autostart verification. (Issue #190)
*   **Network Serialization**: Unified to **snake_case** for Relay alignment.
*   **Time Integrity**: All alarm evaluations use monotonic time via `TimeProvider.elapsedRealtime()`. (Issue #311)
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies. (Issue #322)
*   **Role Forensic**: Mandatory `role` field present in all sync payloads and JSON exports for multi-role trace stability. Viewers explicitly latch and record peer visual jumps to local forensics.
*   **Role Identity Standards**: IDs must use enforced prefixes: **"T"** for Tracker (e.g., Ttk) and **"C"** for Viewer (e.g., Cohen). (Issue #182)
*   **Schema Cleanup**: Legacy `ver` and `vid` columns formally removed from database schema in v33 (v8.8.35).
*   **Power Parity**: `currentMa` field added to Database v35 (PendingStatusEntity and HistoryEntity) and `TrackerStatusProto` for end-to-end power forensics. (Issue #337)
*   **SIT Acknowledgement**: Discrete SIT events are synchronized via a 10s acknowledged loop to prevent forensic loss during blackouts.
*   **Ghost Mode UI**: Visual staleness indicators applied to all sensor fields and markers when telemetry > 10s old. (Issue #338)
*   **Log Spatial Anchor**: All forensic logs and critical alerts are tagged with `lat`/`lng` coordinates to enable historical marker reconstruction on the map.
*   **Accuracy Parity**: Forensic logs now include explicit `accuracy` fields, ensuring historical map markers match real-time precision.
*   **Authoritative Spatial Anchoring (R325) [Authoritative]**: The system must maintain and display both raw `accuracy` and engine-calculated `maxAccuracy` (filtered uncertainty) across all UI layers and forensic logs. However, **`maxAccuracy` is the exclusive authority** for all "out-of-range" evaluations, including Geofence transitions and Distance Violation thresholds. (Issue #325)
*   **Forensic Snapshots**: Log entries now include `snrSnapshot` and `vibeSnapshot` for Jump and Stall forensic enrichment. (Issue #333)
*   **Stability Expansion**: Added `tiltIdx` and `baroIdx` to the analytical ribbons and telemetry pipeline for enhanced "SIT" event analysis. (Issue #329)
*   **Uncertainty Context**: Propagating `locationPendingReason` for Bayesian uncertainty expansion in the UI. (Issue #326)

... (rest of the file) ...
