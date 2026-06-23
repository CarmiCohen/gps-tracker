# Handover: Issue Renumbering Task (Offset +270)

## Objective
To clear conflicts between new and historical issue numbers, a +270 offset is being applied to all "New Phase" issues originally numbered below 32. This moves them into the #271–#300 range.

## Offset Details
- **Offset**: +270
- **Range**: #271 – #300
- **Reference Mappings**:
  - Issue #1 (Uptime Cons) $\rightarrow$ Issue #271
  - Issue #2 (Battery Profile) $\rightarrow$ Issue #272
  - Issue #3 (Network Integrity) $\rightarrow$ Issue #273
  - Issue #6 (Documentation) $\rightarrow$ Issue #276
  - Issue #9 (FGS Resilience) $\rightarrow$ Issue #279
  - Issue #11 (SoT Alignment) $\rightarrow$ Issue #281
  - Issue #12 (SIT Guard) $\rightarrow$ Issue #282
  - Issue #14 (Light EMA Logic) $\rightarrow$ Issue #284
  - Issue #15 (GtoEngine) $\rightarrow$ Issue #285
  - Issue #16 (Hardcoded EMA) $\rightarrow$ Issue #286
  - Issue #17 (Role-Aware Titles) $\rightarrow$ Issue #287
  - Issue #18 (Vertical Displacement) $\rightarrow$ Issue #288
  - Issue #19 (Revival Flag) $\rightarrow$ Issue #289
  - Issue #21 (SIT Duplicate Risk) $\rightarrow$ Issue #291
  - Issue #22 (Acoustic Floor Decay) $\rightarrow$ Issue #292
  - Issue #23 (Geofence Viewer) $\rightarrow$ Issue #293
  - Issue #24 (Viewer Offline Detection) $\rightarrow$ Issue #294
  - Issue #25 (Barometric Baselining) $\rightarrow$ Issue #295
  - Issue #26 (Bootstrap Point) $\rightarrow$ Issue #296
  - Issue #27 (Hindsight Coverage) $\rightarrow$ Issue #297

## Progress Summary

### ✅ Updated Files
- `COMPLIANCE.md`
- `ViewerService.kt`
- `issues.md` (Main tracker and Resolved section synchronized)
- `TrackerService.kt`
- `EngineConstants.kt`
- `GtoEngine.kt`
- `LocationSentinel.kt`
- `MainAlarmLogic.kt`
- `HistoryManager.kt`
- `BaseMonitorService.kt`
- `SyncManager.kt`
- `IntegrityMonitor.kt`
- `SharedUiComponents.kt`
- `AppSensorManager.kt`

### ⏳ Pending Files
The following files still contain low-numbered issue references (#1 to #31) that need renumbering:
- `app/src/main/proto/app_settings.proto` (Issue #2, #3)
- `app/src/proto/app_settings.proto` (Issue #2)
- `core/engine/src/main/java/com/gps19/core/engine/TelemetryAggregator.kt` (Issue #21)
- `app/src/main/java/com/gps19/app/AppSettingsMigration.kt` (Issue #1)
- `DOCS/REQUIREMENTS_SOT.md` (Issue #12, #14)

## Next Steps
1. Finish renumbering in the pending files listed above.
2. Perform a final project-wide grep for `Issue #[0-9]{1,2}\b` (specifically 1-31) to ensure no remnants of the old numbering remain in the "New Phase" context.
