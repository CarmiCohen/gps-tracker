git add .
git commit -m "Nov7.5 - MONITOR COMMUNICATION ALARMS

- ISSUE-NOV5.2-O11 GROUP 1 SLICE 2 (2026-11-07): Implement communication alarms on Monitor (Relay Disconnected, Local Internet Offline, Tracker GPS Signal Loss).
- FIX: Updated MonitorAlarms.kt, MonitorAlarmEngine.kt, AlertSettings.kt, TrackerReading, and MonitorAlarmsTest.kt. MonitorAlarms now tracks Relay Disconnected (signalingClient.isConnected false), Monitor Internet Lost (active network capability NET_CAPABILITY_INTERNET false), and Tracker GPS Signal Loss (telemetry accuracy 0, lat/lng 0, or NO_FIX status while online). Each follows its AlertSettings toggle (relayConnection, localInternet, signalLoss) and Global Mute, sounding the siren and full-screen alarm until acknowledged.
- TESTS: 18 unit tests in MonitorAlarmsTest passing covering all communication alarms, settings toggles, and flow reactions. All 222 automated tests passing across all subprojects (:core-shared 46, :core-network 18, :core-ui 29, :app-monitor 46).
- DOCS: Added Chapter 130 to TESTS/TEST_PROCEDURE.md, Rule 1.366 to SOT_MASTER_REQUIREMENTS.md, Chapter 5.73 to SOT_MASTER_REQUIREMENTS.md, updated RESOLUTION_ARCHIVE.md, issues.md, and baseline STATUS/BASELINE/Nov7.5/. Active chapters: 120 of 130.
- VERSION: Nov7.5, versionCode 43 (monitor) and 45 (sensor)."
git tag Nov7.5
git push origin main --tags

# Project Handover - Version Nov7.5 (Monitor Communication Alarms)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.5 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.5 commit and tag exist. Previous commits: `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, or Nov7.4 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is ISSUE-NOV5.2-O13 (item 9): wire Sensor motion detection with `SentinelLogic.validateMovement` on each fix and send its motion state in telemetry. Then build slice 3 of group 1 alarms (Sensor-detected violations, including GPS jump alarm). Groups 2 and 4 to 8 still need user decisions. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slice 1 (Monitor-side alarms) shipped in Nov7.3; slice 2 (communication alarms: relay, internet, GPS signal loss) shipped in Nov7.5. Group 3 (`SentinelLogic`): build on the Sensor; it runs `validateMovement` with accelerometer motion on each fix, sends its motion state in telemetry (O13, a wire format change), and sends `JUMP` and `OUTLIER` as violations (slice 3); thresholds need a real drive. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.5.
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

## 🟢 Status: VERIFIED (Nov7.5)

Version Nov7.5 delivers ISSUE-NOV5.2-O11 group 1, slice 2:
1. **Three Communication Alarms**: Relay disconnected (`RELAY_DISCONNECTED`), Monitor internet lost (`LOCAL_INTERNET_OFFLINE`), Tracker GPS signal loss (`GPS_SIGNAL_LOSS`).
2. **Settings Controls**: Each switched by its `AlertSettings` toggle (`relayConnection`, `localInternet`, `signalLoss`) and Global Mute.
3. **Lifecycle**: Sounds siren and displays full-screen alarm until acknowledged.
4. **Automated Suite**: 222/222 passing tests across all subprojects, including 18 unit tests in `MonitorAlarmsTest.kt` for communication conditions, toggles, and flow reactions.

**Resumption Token**: NOV7.5-QUEUE-O13-NEXT

---

## 📂 Key Implementation Files
- `app-monitor/.../MonitorAlarms.kt`, `MonitorAlarmEngine.kt`, `MonitorViewModel.kt`, `AlertSettings.kt`; tests `app-monitor/src/test/.../MonitorAlarmsTest.kt`: Chapter 130.
- `TESTS/TEST_PROCEDURE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.5.**
