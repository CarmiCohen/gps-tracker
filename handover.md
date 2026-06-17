# Handover Document

## Recent Changes (v8.8.35)
- **Issue 164 (Telemetry Validation Parity)**:
    - Standardized `TelemetryUseCase.kt` to use `PhysicsUtils.isValidLocation` for all coordinate validation.
    - Removed redundant local `isValidLocation` implementation.
- **Issue 161 (Viewer Alarm Title Confusion)**:
    - Updated `EngineConstants.kt` to use "This device:" prefix for local system alerts (`LOCAL_INTERNET`, `RELAY_OFFLINE`, `STORAGE_LOW`, `STORAGE_CRITICAL`, `XIAOMI_MISSING`).
    - Refined `getTrackerTitle` in `MainAlarmLogic.kt` to strip role prefixes ("Tracker:", "This device:") only when in Tracker mode, ensuring clear attribution on the Viewer.
    - Standardized `detectViolations` to pass all alert titles through `getTrackerTitle`.
- **Issue 162 (Constant Redundancy)**:
    - Migrated network and scheduling constants (`NETWORK_TIMEOUT_MS`, `DAILY_CLEANUP_HOUR`, etc.) to `EngineConstants.kt`.
    - Removed duplicated values from `Constants.kt` to enforce a single source of truth.
- **Issue 148 (GPS Polling Stabilization - A15)**:
    - Added `A15_STABLE_GPS_POLLING_MS` (1000ms) to `EngineConstants.kt`.
    - Updated `ServiceBehaviorUseCase.kt` to enforce 1Hz polling for Samsung A15 devices.
- **Issue 163 (Power Tamper Recovery)**: 
    - Reconnected `IntegrityMonitor` sustained violation callbacks to `TrackerService`.
    - Hardened power detection in `IntegrityMonitor.pollSystemStatus` using `EXTRA_PLUGGED`.
    - Restored "sticky" power alarm logic in `TrackerService`.
- **Issue 160**: Fixed Xiaomi gating logic error by decoupling autostart from MIUI status.
- **Issue 147**: Migrated marker pools to `SnapshotStateList` in `MapComponents.kt`.
- **Issue 159**: Cleaned up database schema (Migration v33) to remove legacy forensic tags.
- **Issue 146**: Optimized startup performance with background OsmConfig initialization.
