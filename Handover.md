git add .
git commit -m "Nov7.10 - SCREEN STATE & ROTATION PRESERVATION (ISSUE-NOV6.0-O22)

- ISSUE-NOV6.0-O22: Converted all UI overlay visibility flags, dialog states, and map follow modes in MonitorActivity.kt (MonitorScreen, PairingWizard) and AdminActivity.kt (AdminActivity, SettingsOverlay) from plain remember to rememberSaveable, ensuring open overlays, dialogs, unlock states, and follow modes persist across screen rotation and configuration changes.
- TESTS: Verified all automated unit tests passing across all subprojects (:core-shared, :core-network, :core-ui, :app-monitor, :app-sensor).
- DOCS: Updated issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md, VERIFICATION_MANIFEST.md, TESTS/TEST_PROCEDURE.md (Chapter 134 added), CHAPTER_TRIAGE.md (Chapter 87 updated), and created baseline STATUS/BASELINE/Nov7.10/.
- VERSION: Nov7.10, versionCode 46 (monitor) and 48 (sensor)."
git tag Nov7.10
git push origin main --tags

# Project Handover - Version Nov7.10 (Screen State & Rotation Preservation)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.10 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.10 commit and tag exist. Previous commits: `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O24 (item 16): make `SignalingQueue` thread safe and keep order after a failed flush. Groups 2 and 4 to 8 still need user decisions. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms: relay, internet, GPS signal loss) shipped in Nov7.5; 30-second siren auto-stop shipped in Nov7.6; slice 3 (Sensor-detected violations, GPS jump alarm, false MOVING fix with `SentinelLogic.validateMovement`) shipped in Nov7.7. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.10.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`, API 36) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O24 `SignalingQueue` not thread safe | Low | `SignalingQueue.kt`, `SocketioSignalingClient.kt` `flushQueue` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: VERIFIED (Nov7.10)

Version Nov7.10 delivers ISSUE-NOV6.0-O22 (Screen State & Rotation Preservation):
1. **Overlay & State Persistence Across Rotation**: Converted all UI overlay visibility flags, dialog states, and map follow modes in `MonitorActivity.kt` (`MonitorScreen`, `PairingWizard`) and `AdminActivity.kt` (`AdminActivity`, `SettingsOverlay`) from plain `remember` to `rememberSaveable`. Open overlays, dialogs, unlock states (`isLocked`), and follow modes now persist across screen rotation and configuration changes.
2. **Automated Suite**: All automated unit tests passing across all subprojects (`:core-shared`, `:core-network`, `:core-ui`, `:app-monitor`, `:app-sensor`).
3. **Documentation & Manifest Update**: Updated `TESTS/CHAPTER_TRIAGE.md` Chapter 87, `TESTS/TEST_PROCEDURE.md` (Chapter 134 added), `STATUS/SOT_MASTER_REQUIREMENTS.md` (Rule 1.370 added), `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `VERIFICATION_MANIFEST.md`, and created baseline `STATUS/BASELINE/Nov7.10/`.

**Resumption Token**: NOV7.10-QUEUE-O24-NEXT

---

## 📂 Key Implementation Files
- `app-monitor/.../MonitorActivity.kt`, `app-sensor/.../AdminActivity.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.10.**
