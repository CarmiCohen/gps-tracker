# Project Handover - v8.9.10 Logic Baseline

## 1. Context Summary
- **Project**: `gps-tracker` (Native Android, Kotlin/Compose).
- **Current Baseline**: **v8.9.10** (Log Spatial Anchoring).
- **Architecture**: Clean Architecture with modular separation. 
- **Toolchain**: Java 17 and Android SDK 35 (Android 15) confirmed.

## 2. Completed in v8.9.10
### Major Feature: Log Spatial Anchor (Issue #208)
- **Implementation**: Updated `LogManager.submitToLogSink` to auto-anchor logs using the last known coordinates from `TelemetryRepository`.
- **Integration**: `AppAlarmManager`, `TrackerService`, and `ViewerService` now propagate current coordinates for Alarms, SIT detection, and hardware events.
- **Persistence**: Database schema (v39) and `LogEntry` model fully support `lat`/`lng` fields.
- **Forensic Utility**: Critical alerts and system events can now be reconstructed on the map even if they occurred during relay blackouts.

### Documentation & Hardening
- **Issue #207**: Fixed syntax typos in `REQUIREMENTS_SOT.md` (quotes vs backticks).
- **Issue #203**: Synchronized all core documentation to the v8.9.10 baseline.
- **Issue #190 Refinement**: Hardened Xiaomi boot resilience logic and verified the 30s `XIAOMI_BOOT_GRACE_MS` window in `MainAlarmLogic.kt`.

## 3. Pending Tasks
1.  **Issue #190: MIUI 14 Field Verification**:
    - Awaiting physical hardware verification of the boot grace period.
2.  **Release Build**:
    - The `versionCode` in `build.gradle` is git-driven; ensure a clean tag is created before distribution.

## 4. Key Files for Reference
- `issues.md`: Master tracking of all fixed and open issues.
- `DOCS/REQUIREMENTS_SOT.md`: Definitive system thresholds and constants.
- `app/src/main/java/com/gps19/app/LogManager.kt`: Center of the new anchoring logic.
