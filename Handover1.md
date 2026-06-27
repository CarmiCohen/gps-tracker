# Handover Status: R325 Authoritative Spatial Anchoring (v8.9.42)

## 🎯 Requirement: R325 Authoritative Spatial Anchoring (Dual-Metric)
> The system must maintain and display both raw `accuracy` and engine-calculated `maxAccuracy` (filtered uncertainty) across all UI layers and forensic logs. However, **`maxAccuracy` is the exclusive authority** for all "out-of-range" evaluations, including Geofence transitions and Distance Violation thresholds.

## 🛠️ Work Done
### 1. Logic Authority (Exclusive)
- **Engine Logic**: Updated `MainAlarmLogic.kt` to strictly use `state.maxTrackerAccuracy` for both **Geofence Exit** (Violation) and **Geofence Entry** (Recovery). This ensures filtered uncertainty is the logic authority.
- **Buffer Calculation**: Derived `accuracyBuffer` exclusively from `maxTrackerAccuracy`.

### 2. Forensic Persistence (Database v47)
- **Schema Update**: Added `maxAccuracy` column to `logs` table in `Database.kt` (Migration `46_47`).
- **Data Models**: Updated `LogEntry`, `LogEntity`, `TrackerStatus`, and `DashboardState` to support dual accuracy fields.
- **Log Manager**: Refactored `LogManager.kt` to auto-anchor both metrics into forensic logs from the latest telemetry.

### 3. UI Visualization (Side-by-Side)
- **Status Bar**: Updated `SharedUiComponents.kt` (`StatusRowData`) to display `±Raw (±Max)`.
- **Dashboard**: Updated `OverlayComponents.kt` (`LegacyDashboardGrid`) to show side-by-side metrics.
- **Forensic Detail**: Updated `LogComponents.kt` (`LogDetailPane`) to explicitly list "RAW ACCURACY" and "UNCERTAINTY (MAX)".

### 4. Logic & Sync
- **Synchronization**: Aligned `SyncManager.kt` and `RemoteHandler.kt` to propagate both values between roles.
- **UseCase Alignment**: `DashboardUseCase.kt` now correctly segregates the two metrics for visualization.

## 📊 Current Status: IMPLEMENTED
- **SoT**: Updated `STATUS/requirements_sot.md`.
- **Codebase**: Logic and data architecture aligned with R325.

## ⚠️ Resumption Guardrails
1. **Build Verification**: Major refactorings fixed in the final turns (typos in `Models.kt`, naming consistency in `Database.kt` for `sitDz`, and `AppAlarmManager` lambda signature). Run `:app:assembleDebug` to confirm.
2. **Database Migration**: Verify `Migration_46_47` on hardware to ensure log history preservation.
3. **Dual Metric Audit**: Verify that Geofence alarms do NOT clear until `maxAccuracy` (not just raw accuracy) falls below the recovery threshold.

**Status**: R325 is structurally complete.
