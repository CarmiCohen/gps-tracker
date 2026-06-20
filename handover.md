# Forensic Handover - v8.9.8 (Zombie Hardening & Muzzle Stability)

## Current Status: Session Continuity Protocol
This document serves as the "source of truth" to resume engineering on the GPS-Tracker project. All architectural alignments and forensic logic mentioned below are verified in the v8.9.8 codebase.

### 1. RECENTLY FIXED (v8.9.x Series)
- **Issue #193: Zombie Telemetry UX [COMPLETE]**:
    - **OverlayComponents.kt**: Hardened `LegacyDashboardGrid` and `DebugTable`.
    - **Logic**: All forensic badges (`[SUSPICIOUS]`, `[TAMPER]`, `[SITTING]`, `[BATT HEALTH]`) and all sensor-derived fields (Vibration, Compass, Tilt, Noise, Lux, Proximity, Forensics, Battery mA) now consistently dim to `Slate500` ("Ghost Mode") when `isTelemetryFresh` is false (10s threshold).
    - **SharedUiComponents.kt**: Verified `StatusRowData` and `StatusBar` parity. Role-based battery/temp icons and text now reflect telemetry staleness correctly.
    - **DashboardUseCase.kt**: Confirmed `isTelemetryFresh` calculation uses `TELEMETRY_UI_STALE_THRESHOLD_MS` (10s).
- **Issue #191: Muzzle Window Hardening [VERIFIED]**:
    - **TrackerService.kt**: Confirmed device-specific hysteresis (500ms for A15, 200ms default) in the `SyncManager` handshake. Suppresses I/O-induced vibration alarms after sync.
- **Plunge Matching: Advanced SIT Detection [COMPLETE]**:
    - `AppSensorManager.kt` and `LocationProcessor.kt` now fully propagate `sitVzTs` for forensic parity.
    - **Database v38**: Added `sitVzTs` to `connection_history` and `pending_status_updates`. Implemented `MIGRATION_37_38`.
- **Issue #194: SIT Marker Reconstruction [COMPLETE]**:
    - **RemoteHandler.kt**: Recovered "Sit Detected" events from synced logs now trigger immediate `recordViolationMarkers` on the Viewer map.

### 2. OPEN ISSUES & PENDING ACTIONS
- **Issue #190: Xiaomi Verification [RESUME HERE]**:
    - **Task**: Field test "Unknown" Autostart handling on MIUI 14.
    - **Status**: Gating logic in `MainAlarmLogic.kt` is implemented (muzzling alerts if status is UNKNOWN and Manual Override is ON). Needs hardware confirmation that MIUI 14 doesn't trigger "Denied" during boot transitions.
- **Issue #193: Final UI Surface Sweep**:
    - **Task**: Audit `MapComponents.kt` and `LogComponents.kt`. Ensure that if telemetry detail pop-ups exist, they respect the 10s `isTelemetryFresh` threshold.

### 3. ARCHITECTURAL BASELINE
- **Module Boundary**: Logic in `:core:engine`. Android integration in `:app`.
- **Temporal Consistency**: `TimeProvider` (Monotonic Clock) is mandatory for all intervals.
- **Forensic Standard**: `Slate500` is the reserved color for stale/offline/ghosted data. Map markers for security events are Magenta Squares (`ALERT_ID_VISUAL_JUMP`) or Red Circles (`ALERT_ID_TRACKER_GEOFENCE`).

---
**Resume at**: `MainAlarmLogic.kt` (Issue #190) or `MapComponents.kt` (Issue #193 sweep).
