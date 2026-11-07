git add .
git commit -m "Nov7.7 - FALSE MOVING FIX & SENSOR-DETECTED VIOLATIONS (SLICE 3)

- ISSUE-NOV5.2-O13 (2026-11-07): Fixed false MOVING on the Sensor by wiring `SentinelLogic.validateMovement` with accelerometer motion (`hasPhysicalMotion`) on each fix in `SensorService.kt`, transmitting motion state and violation status (`JUMP`, `OUTLIER`) in telemetry and via `ViolationEvent`. Added `AlarmType.GPS_JUMP` to `MonitorAlarms.kt`, wired to `AlertSettings.jammerDetection` and `signalingClient.violationFlow` in `MonitorAlarmEngine.kt`.
- TESTS: All 222 automated unit tests passing across all subprojects (:core-shared 46, :core-network 18, :core-ui 29, :app-monitor 46).
- DOCS: Added Chapter 132 to TESTS/TEST_PROCEDURE.md, Rule 1.368 to SOT_MASTER_REQUIREMENTS.md, Chapter 5.75 to SOT_MASTER_REQUIREMENTS.md, updated RESOLUTION_ARCHIVE.md, issues.md, and baseline STATUS/BASELINE/Nov7.7/. Active chapters: 122 of 132.
- VERSION: Nov7.7, versionCode 44 (monitor) and 46 (sensor)."
git tag Nov7.7
git push origin main --tags

# Project Handover - Version Nov7.7 (False MOVING Fix & Sensor-Detected Violations)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.7 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.7 commit and tag exist. Previous commits: `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV6.0-O23 (item 13): replace tests that do not prove their chapter, add the T tests, correct stale chapter wording. Groups 2 and 4 to 8 still need user decisions. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms: relay, internet, GPS signal loss) shipped in Nov7.5; 30-second siren auto-stop shipped in Nov7.6; slice 3 (Sensor-detected violations, GPS jump alarm, false MOVING fix with `SentinelLogic.validateMovement`) shipped in Nov7.7. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.7.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`, API 36) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | T rows in `TESTS/CHAPTER_TRIAGE.md`, `HydrationStaggeringAuditTest.kt`, `WatchdogRecoveryTest.kt` |
| O21 HUD flows not limited to 2Hz | Low | `MonitorViewModel.kt` `localHudState`, `remoteHudState` |
| O22 Rotation resets screen state | Low | `MonitorActivity.kt` `MonitorScreen` `remember` flags, `AdminActivity.kt:83-88` |
| O24 `SignalingQueue` not thread safe | Low | `SignalingQueue.kt`, `SocketioSignalingClient.kt` `flushQueue` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: VERIFIED (Nov7.7)

Version Nov7.7 delivers ISSUE-NOV5.2-O13 (False MOVING Fix & Sensor-Detected Violations - Slice 3):
1. **False MOVING Fixed**: Sensor runs `SentinelLogic.validateMovement` with accelerometer motion (`hasPhysicalMotion`) on each fix, suppressing GPS noise while stationary.
2. **Sensor-Detected Violations**: Transmits `JUMP` and `OUTLIER` status in telemetry and via `ViolationEvent`.
3. **Monitor GPS Jump Alarm**: Added `AlarmType.GPS_JUMP` to Monitor alarms, switched by `AlertSettings.jammerDetection` and triggered by `ViolationEvent` or telemetry status.
4. **Automated Suite**: 222/222 passing tests across all subprojects.

**Resumption Token**: NOV7.7-QUEUE-O23-NEXT

---

## 📂 Key Implementation Files
- `app-sensor/.../SensorService.kt`.
- `app-monitor/.../MonitorAlarms.kt`, `MonitorAlarmEngine.kt`.
- `TESTS/TEST_PROCEDURE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.7.**
