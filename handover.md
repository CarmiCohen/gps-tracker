# Handover: Issue Renumbering Completion (Offset +270)

## 📌 Status Summary
The renumbering of "New Phase" issues (originally #1–#31) to the #271–#300 range (Offset +270) is now **complete**. All active code references, proto definitions, and documentation have been synchronized. Historical references have been preserved using the `(Formerly #X)` notation where appropriate.

## 🛠 Actions Taken
1.  **Code Synchronization**: Updated `AppSettingsMigration.kt`, `AlarmActivity.kt`, and `TelemetryAggregator.kt` with new issue numbers.
2.  **Schema Alignment**: Updated `app/src/main/proto/app_settings.proto` and `app/src/proto/app_settings.proto` to reflect new issue IDs for battery and network logic.
3.  **Documentation Update**:
    *   `Issues.md`: Synchronized the **Resolved** section with the new numbering and updated the dashboard count.
    *   `DOCS/REQUIREMENTS_SOT.md`: Verified that issues #12 and #14 were correctly updated to #282 and #284.
4.  **Forensic Preservation**: Added `(Formerly #X)` to renumbered items to maintain traceability to the original "New Phase" design notes.

## 🗺 Mapping Reference (Offset +270)
| Original ID | New ID | Description |
| :--- | :--- | :--- |
| Issue #1 | **#271** | Uptime / Sit Metadata Persistence |
| Issue #2 | **#272** | Battery Profile / Discharge |
| Issue #3 | **#273** | Network Signaling Integrity |
| Issue #6 | **#276** | Xiaomi Documentation / Gating |
| Issue #9 | **#279** | FGS Resilience |
| Issue #11 | **#281** | SoT Naming Alignment |
| Issue #12 | **#282** | SIT Duplicate Guard |
| Issue #14 | **#284** | Light EMA Logic |
| Issue #15 | **#285** | GtoEngine Implementation |
| Issue #16 | **#286** | Hardcoded EMA Cleanup |
| Issue #17 | **#287** | Role-Aware Titles |
| Issue #18 | **#288** | Vertical Displacement / Xiaomi Override |
| Issue #19 | **#289** | Revival Flag Cleanup |
| Issue #21 | **#291** | SIT Duplicate Risk |
| Issue #22 | **#292** | Acoustic Floor Decay |
| Issue #23 | **#293** | Geofence Viewer Logic |
| Issue #24 | **#294** | Viewer Offline Detection |
| Issue #25 | **#295** | Barometric Baselining |
| Issue #26 | **#296** | Bootstrap Point Initialization |
| Issue #27 | **#297** | Hindsight Coverage Tests |

## 📂 Files Verified/Updated
- `app/src/main/java/com/gps19/app/AppSettingsMigration.kt`
- `app/src/main/java/com/gps19/app/AlarmActivity.kt`
- `core/engine/src/main/java/com/gps19/core/engine/TelemetryAggregator.kt`
- `app/src/main/proto/app_settings.proto`
- `app/src/proto/app_settings.proto`
- `Issues.md`
- `DOCS/REQUIREMENTS_SOT.md`
- `COMPLIANCE.md` (Already updated in previous steps)

## 🔍 Verification
A project-wide grep for `Issue #([1-9]|[12][0-9]|3[01])\b` (excluding "Formerly #" and Handover notes) returns zero hits in the production source and core documentation.

**Status**: ✅ **Ready for next phase.**
