git add .
git commit -m "Nov7.6 - 30-SECOND SIREN AUTO-STOP (TIMER OR ACKNOWLEDGE)

- ISSUE-NOV7.6-O40 (2026-11-07): Implemented 30-second siren auto-stop for Monitor alarms: siren automatically stops after 30 seconds or when acknowledged (whichever comes first).
- FIX: Updated MonitorAlarmEngine.kt to add a 30-second timeout on siren playback (`sirenTimedOut` flow/job), allowing the visual alarm and notification to remain pending until explicitly acknowledged while stopping the sound after 30s.
- TESTS: All 222 automated unit tests passing across all subprojects (:core-shared 46, :core-network 18, :core-ui 29, :app-monitor 46).
- DOCS: Added Chapter 131 to TESTS/TEST_PROCEDURE.md, Rule 1.367 to SOT_MASTER_REQUIREMENTS.md, Chapter 5.74 to SOT_MASTER_REQUIREMENTS.md, updated RESOLUTION_ARCHIVE.md, issues.md, and baseline STATUS/BASELINE/Nov7.6/. Active chapters: 121 of 131.
- VERSION: Nov7.6, versionCode 44 (monitor) and 46 (sensor)."
git tag Nov7.6
git push origin main --tags

# Project Handover - Version Nov7.6 (30-Second Siren Auto-Stop)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.6 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.6 commit and tag exist. Previous commits: `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV5.2-O13 (item 9): wire Sensor motion detection with `SentinelLogic.validateMovement` on each fix and send its motion state in telemetry. Then build slice 3 of group 1 alarms (Sensor-detected violations, including GPS jump alarm). Groups 2 and 4 to 8 still need user decisions. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms: relay, internet, GPS signal loss) shipped in Nov7.5; 30-second siren auto-stop (timer or acknowledge) shipped in Nov7.6. Group 3 (`SentinelLogic`): build on the Sensor; it runs `validateMovement` with accelerometer motion on each fix, sends its motion state in telemetry (O13, a wire format change), and sends `JUMP` and `OUTLIER` as violations (slice 3); thresholds need a real drive. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.6.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`, API 36) unplugged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O13 False MOVING | Medium | `core-ui/.../FixReading.kt` `motionState`, used by all three HUD lines; wire SentinelLogic on Sensor next |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md`; group 1 slice 3 next |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | T rows in `TESTS/CHAPTER_TRIAGE.md`, `HydrationStaggeringAuditTest.kt`, `WatchdogRecoveryTest.kt` |
| O21 HUD flows not limited to 2Hz | Low | `MonitorViewModel.kt` `localHudState`, `remoteHudState` |
| O22 Rotation resets screen state | Low | `MonitorActivity.kt` `MonitorScreen` `remember` flags, `AdminActivity.kt:83-88` |
| O24 `SignalingQueue` not thread safe | Low | `SignalingQueue.kt`, `SocketioSignalingClient.kt` `flushQueue` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: VERIFIED (Nov7.6)

Version Nov7.6 delivers ISSUE-NOV7.6-O40 (30-Second Siren Auto-Stop):
1. **Siren Auto-Stop Timer**: Siren automatically stops after 30 seconds (`30s`) or when acknowledged by the user, whichever occurs first.
2. **Pending Notification**: When the siren auto-stops via the 30-second timer, the visual alarm and notification remain pending until explicitly acknowledged.
3. **Automated Suite**: 222/222 passing tests across all subprojects.

**Resumption Token**: NOV7.6-QUEUE-O13-NEXT

---

## 📂 Key Implementation Files
- `app-monitor/.../MonitorAlarmEngine.kt`, `MonitorAlarms.kt`, `SirenPlayer.kt`.
- `TESTS/TEST_PROCEDURE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.6.**
