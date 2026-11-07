git add .
git commit -m "Nov7.12 - COARSE NETWORK FIX FILTERING (ISSUE-NOV6.1-O28)

- ISSUE-NOV6.1-O28: Added isBetterLocation filter to MonitorLocationSource to prevent coarse network fixes (e.g. accuracy +-77.6m) from overriding fresh, more accurate GPS fixes (<2 minutes old) on the Monitor UI and map. Added MonitorLocationSourceTest unit tests.
- TESTS: Verified all unit tests passing across all subprojects (:core-shared, :core-network, :core-ui, :app-monitor, :app-sensor).
- DOCS: Updated issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md, VERIFICATION_MANIFEST.md, TESTS/TEST_PROCEDURE.md (Chapter 136 added), CHAPTER_TRIAGE.md (Chapter 136 added), and created baseline STATUS/BASELINE/Nov7.12/.
- VERSION: Nov7.12, versionCode 47 (monitor) and 49 (sensor)."
git tag Nov7.12
git push origin main --tags

# Project Handover - Version Nov7.12 (Coarse Network Fix Filtering)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.12 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.12 commit and tag exist. Previous commits: `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O26 (item 18): run the Sensor on a device or emulator below API 35. If the user prefers other work, take the first item marked `Nothing`: ISSUE-NOV6.4-O33 (stop the sensor sending later backfill chunks after one fails). Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms) shipped in Nov7.5; 30-second siren auto-stop shipped in Nov7.6; slice 3 (Sensor-detected violations, GPS jump alarm, false MOVING fix with `SentinelLogic.validateMovement`) shipped in Nov7.7. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.12.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`, API 36) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: VERIFIED (Nov7.12)

Version Nov7.12 delivers ISSUE-NOV6.1-O28 (Coarse Network Fix Filtering):
1. **Coarse Network Fix Filtering**: Implemented standard Android `isBetterLocation` filtering in `MonitorLocationSource.kt` to prevent coarse Network location fixes (e.g., accuracy ±77.6m) from replacing fresh, more accurate GPS location fixes (< 2 minutes old) on the Monitor UI and map.
2. **Automated Suite**: All automated unit tests passing across all subprojects (`:core-shared`, `:core-network`, `:core-ui`, `:app-monitor`, `:app-sensor`).
3. **Documentation & Manifest Update**: Updated `TESTS/CHAPTER_TRIAGE.md` (Chapter 136 added), `TESTS/TEST_PROCEDURE.md` (Chapter 136 added), `STATUS/SOT_MASTER_REQUIREMENTS.md` (Rule 1.372 added), `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `VERIFICATION_MANIFEST.md`, and created baseline `STATUS/BASELINE/Nov7.12/`.

**Resumption Token**: NOV7.12-LOCATION-O26-NEXT

---

## 📂 Key Implementation Files
- `app-monitor/.../MonitorLocationSource.kt`, `app-monitor/src/test/.../MonitorLocationSourceTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.12.**
