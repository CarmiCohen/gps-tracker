git add .
git commit -m "Nov7.9 - HUD FLOW 2HZ RATE LIMITING (ISSUE-NOV6.0-O21)

- ISSUE-NOV6.0-O21: Applied atUiRate() (500ms sampling window) to localHudState and remoteHudState in MonitorViewModel.kt and to hudState in SensorViewModel.kt. Throttled incoming telemetry/fix streams (uiTelemetry = pairedTelemetry.atUiRate(), uiFix = fixStore.fix.atUiRate()) before combine blocks to prevent 10Hz re-evaluations of system service checks (isNetworkAvailable, isLocationProviderEnabled) and cap UI state emissions to 2Hz.
- TESTS: Added HudRateTest in :app-monitor testing 2Hz rate-limiting of 10Hz telemetry/HUD streams. All automated unit tests passing across all subprojects (:core-shared, :core-network, :core-ui, :app-monitor, :app-sensor).
- DOCS: Updated issues.md, RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md, VERIFICATION_MANIFEST.md, TESTS/TEST_PROCEDURE.md (Chapter 133 added), CHAPTER_TRIAGE.md (Chapter 75 updated), and baseline STATUS/BASELINE/Nov7.9/.
- VERSION: Nov7.9, versionCode 45 (monitor) and 47 (sensor)."
git tag Nov7.9
git push origin main --tags

# Project Handover - Version Nov7.9 (HUD Flow 2Hz Rate Limiting)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.9 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.9 commit and tag exist. Previous commits: `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O22 (item 15): keep overlays and follow mode across rotation. Groups 2 and 4 to 8 still need user decisions. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms: relay, internet, GPS signal loss) shipped in Nov7.5; 30-second siren auto-stop shipped in Nov7.6; slice 3 (Sensor-detected violations, GPS jump alarm, false MOVING fix with `SentinelLogic.validateMovement`) shipped in Nov7.7. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.9.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`, API 36) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O22 Rotation resets screen state | Low | `MonitorActivity.kt` `MonitorScreen` `remember` flags, `AdminActivity.kt:83-88` |
| O24 `SignalingQueue` not thread safe | Low | `SignalingQueue.kt`, `SocketioSignalingClient.kt` `flushQueue` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: VERIFIED (Nov7.9)

Version Nov7.9 delivers ISSUE-NOV6.0-O21 (HUD Flow 2Hz Rate Limiting):
1. **Monitor HUD Flow Rate-Limiting**: `uiTelemetry = pairedTelemetry.atUiRate()` prevents 10Hz re-evaluations of `isNetworkAvailable` and `isLocationProviderEnabled` inside `localHudState` and `remoteHudState` `combine` blocks in `MonitorViewModel.kt`. `.atUiRate()` appended to both flows caps UI emissions to 2Hz (500ms sampling window).
2. **Sensor HUD Flow Rate-Limiting**: `uiFix = fixStore.fix.atUiRate()` in `SensorViewModel.kt` throttles fix updates and `.atUiRate()` caps `hudState` UI emissions to 2Hz.
3. **Automated Suite**: Added `HudRateTest` in `app-monitor` testing rate-limiting of 10Hz telemetry and HUD streams to 2Hz. All unit tests passing across all subprojects.
4. **Documentation & Manifest Update**: Updated `TESTS/CHAPTER_TRIAGE.md` Chapter 75, `TESTS/TEST_PROCEDURE.md` (Chapter 133 added), `STATUS/SOT_MASTER_REQUIREMENTS.md` (Rule 1.369 added), `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `VERIFICATION_MANIFEST.md`, and baseline `STATUS/BASELINE/Nov7.9/`.

**Resumption Token**: NOV7.9-QUEUE-O22-NEXT

---

## 📂 Key Implementation Files
- `app-monitor/.../MonitorViewModel.kt`, `app-sensor/.../SensorViewModel.kt`, `app-monitor/.../HudRateTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.9.**
