# Handover Document - Hardening Phase (v8.9.62)

## 🎯 Recent Changes Summary
This document tracks high-assurance changes and forensic alignment fixes applied during the current session.

### v8.9.62
- **Issue #001: Room Schema Divergence (Root Cause Remediation)**:
    - Incremented database version to **51**.
    - Restored missing `name` column in `pending_status_updates` table.
    - Corrected historical migration scripts (`MIGRATION_32_33`, `MIGRATION_35_36`) to ensure schema consistency for all installation paths.
    - Added `MIGRATION_50_51` for immediate upgrade path.
- **Issue #002: GPS UI Status Mismatch**:
    - Increased `GPS_UI_FAIL_THRESHOLD_MS` and `TELEMETRY_UI_STALE_THRESHOLD_MS` to **35s** in `EngineConstants.kt`.
    - Aligned UI failure gates with the 20s stationary GPS polling interval to prevent false-positive "Red" indicators.
- **Issue #003: Main Thread Jitter Optimization**:
    - Moved behavioral state computations (`computeTrackerState`, `shouldShowRedScreen`) to `Dispatchers.Default` in `MainViewModel`.
    - Resolved `Davey!` events and initialization frame skips by offloading CPU-intensive engine logic from the main thread.
- **Issue #004: Refined A15 Virtual Proximity Protection**:
    - Updated `AppSensorManager` to allow "Far" transitions in darkness if the device is not stationary.
    - Mitigated virtual sensor lock-up while maintaining protection against dark-room false triggers.
- **Issue #005: Map Provider Log Spillage**:
    - Silenced `osmdroid` debug logging and enforced a static user agent in `GpsApplication`.
    - Eliminated constant `getPackageName` log spam, improving forensic visibility.

### v8.9.61
- **R924/R924-A: UI Version String Replacement**:
    - Introduced `SignalingConstants.VID_NOTES` set to "renumb".
    - Updated `HeaderBar` in `SharedUiComponents.kt` to display `VID_NOTES` instead of the version number in both portrait and landscape layouts.

### v8.9.60
- **Forensic Ghost Mode Restoration (Issue #458)**: Fixed a bug where Tracker-role devices showed local status as stale. Corrected timestamp propagation in `SharedUiComponents.kt`.
- **Telemetry Freshness Logic (Issue #460)**: Relaxed `isLocalTelemetryFresh` check to support sensor-only telemetry existence, preventing "Ghost Mode" before the first GPS fix.
- **Unicode Label Fix (Issue #459)**: Corrected double-escaped thin-space characters in `StatusBar` and `GlobalStatusBar` labels.

## ⚠️ Open Technical Debt
- **Issue #461**: Settings uniqueness enforcement is implemented in the repository but needs verified UI feedback for the error string.

## 📊 Compliance Status
- **R325 (Spatial Anchoring)**: Side-by-side accuracy visualization implemented. Pending validation on small-screen devices.
- **R338 (Ghost Mode)**: Thresholds unified at 15s across all UI components (Note: v8.9.62 increased this to 35s to align with hardware polling).
- **R924 (VID_Notes Display)**: Successfully migrated button row version display to hard-coded notes string.
