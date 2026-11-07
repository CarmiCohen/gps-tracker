git add .
git commit -m "Nov7.3 - MONITOR ALARMS AND SIREN

- ISSUE-NOV5.2-O11 USER DECISIONS (2026-11-07): build group 1, alarms and sirens, in slices, starting with the Monitor-side alarms; an alarm sounds until it is acknowledged. Groups 2 to 8 are not decided.
- ISSUE-NOV5.2-O11 GROUP 1 SLICE 1 ROOT CAUSE: nothing could raise an alarm and nothing in either app played a sound. TEST SIREN AUDIO only flipped a flag, and AlarmOverlayActivity was reachable only through an unused FCM path.
- FIX: new MonitorAlarms.kt, SirenPlayer.kt, MonitorAlarmEngine.kt, and AlarmAlertScreen.kt in app-monitor. The Monitor raises Tracker offline (35 s without an update), Tracker power disconnected, Tracker battery low (below 20%, ends at 22%), and Tracker temperature high (above 46 C, ends at 44 C), each on its existing Alert Settings switch and Global Mute. An alarm shows full screen, sounds the synthesized siren with the saved Siren and Audio settings, and posts an alarm notification until ACKNOWLEDGE ALARM, even after its condition clears; it is raised again only after its condition clears. MonitorSessionService runs the engine, so alarms work in the background. AlarmOverlayActivity shows the engine's alarms. TEST SIREN AUDIO plays the real siren. The Monitor asks for the notification permission.
- CHANNEL ROOT CAUSE: MonitorSessionService re-created the alarm channel at low importance, which Android applied (importance 2 on the A15). Alarm and session notifications now use monitor_alarm (high) and monitor_session (low), defined once in createMonitorNotificationChannels, and the lowered channel is deleted.
- RULE 14: SensorStealthGuardTest fails if Sensor, core-shared, or core-network code uses audio or vibration classes.
- TESTS: MonitorAlarmsTest (15), SirenSoundTest (5), SensorStealthGuardTest (3), MonitorAlarmDeviceTest (5, siren at gain 0, restores the phone's alarm volume). A first Monitor run crashed because the session channel was missing under the test application; the service now creates the channels. Two deliberate breaks each failed their test and were restored. 222 of 222 automated tests pass, including 86 on the A15. The A15 alarm volume, changed by an early test run, was restored to 1.
- LIVE: On the A15 through the production relay at low volume (user decision; Force Maximum turned off on the A15 Monitor): simulated unplug, low battery, and high temperature, and a force-stopped Sensor each raised the alarm, siren, and notification; Acknowledge silenced each; alarms stayed as cleared until acknowledged and returned after re-arming, including with the Monitor in the background, where the notification opened AlarmOverlayActivity. 0 Davey, StackLog, or fatal entries over about 7 minutes. The S21FE was not used.
- DOCS: chapters 4.2, 8, and 27 verified; 8.2 reworded; 8.3, 26.2, and 26.3 retired as superseded by the decision. Added Chapter 129, Rule 1.365, Chapter 5.72, DOCS/ALARM_AND_SIREN_MECHANISM.md section 3a, and baseline STATUS/BASELINE/Nov7.3/. QA is 54 of 119 active chapters.
- ISSUE-NOV7.3-O38 ADDED: the Sensor's siren test can never sound. ISSUE-NOV7.3-O39 ADDED: Do Not Disturb bypass needs DND access, and the full-screen start was seen only as a heads-up. ISSUE-NOV7.2-O37 EXTENDED: the Sensor sends 100% and 30 C when the battery is unknown.
- IDEAS ADDED: SIMP-NOV7.3-S1 one siren type setting, SIMP-NOV7.3-S2 remove the FCM alarm path.
- VERSION: Nov7.3, versionCode 42 (monitor) and 44 (sensor)."
git tag Nov7.3
git push origin main --tags

# Project Handover - Version Nov7.3 (Monitor Alarms And Siren)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.3 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.3 commit and tag exist. The previous commit is `77f2912` (Nov7.2), which is pushed with its tag. There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. The next Work Queue item in `DOCS/issues.md` is ISSUE-NOV5.2-O11. Its group 1 slice 2, the communication alarms (relay disconnected, the Monitor's own internet lost, GPS signal loss, GPS jump), is decided and needs nothing. Groups 2 to 8 still need user decisions. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; an alarm sounds until acknowledged, with no auto-stop or lockout. For the Nov7.3 live test the user chose a low siren volume.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.3. Relay pushes restart production, so the user runs them. Slice 3 (Sensor-detected violations) will need a relay change.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov7.3 debug Sensor on the default relay with its service started at about 15:08, and the Nov7.3 debug Monitor paired with `T`. **On the A15 Monitor, `Force Maximum Hardware Audio Volume` is turned off** for the low-volume test; the default is on. Tell the user, and turn it back on only if they want it. The A15 alarm stream volume is 1 of 15, as found. `dumpsys battery` was reset after the simulations; if the HUD ever shows a fake level, run `adb -s R58X40GV2AR shell dumpsys battery reset`. S21FE (`R5CRC14PG4F`, API 36) is a personal phone and is unplugged. It holds the Nov6.3 Monitor and the field-test table (ISSUE-NOV5.2-O16). Before the next field test, install the current version and clear that Monitor's data, with the user's agreement. Do not run instrumented tests on it, and do not touch `com.gps19.app` or `com.gps19.app.monitor` (an older app on the A15 that has its own `critical_violation_channel`). Run `adb devices` first and name the device in every command.
- **Pending user actions**: decide groups 2 to 8 of O11, and O19, O29, O30, O35, O36, and O39 when the queue reaches them; allow S21FE use for the second half of chapter 125.7; decide later on a device or emulator below API 35 for O26 (**do not download one until the user says so**); optionally add the shell permission rule to `.claude/settings.local.json`; optionally run `setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md`; group 1 slices 2 and 3 next |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | T rows in `TESTS/CHAPTER_TRIAGE.md`, `HydrationStaggeringAuditTest.kt`, `WatchdogRecoveryTest.kt`, chapter 116.3 |
| O4 Remaining H and S chapters | Medium | `TESTS/CHAPTER_TRIAGE.md` |
| O13 False MOVING | Medium | `core-ui/.../FixReading.kt` `motionState`, used by all three HUD lines |
| O21 HUD flows not limited to 2Hz | Low | `MonitorViewModel.kt` `localHudState`, `remoteHudState` |
| O22 Rotation resets screen state | Low | `MonitorActivity.kt` `MonitorScreen` `remember` flags, `AdminActivity.kt:83-88` |
| O24 `SignalingQueue` not thread safe | Low | `SignalingQueue.kt`, `SocketioSignalingClient.kt` `flushQueue` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O7 Zero-position rows stored | Low | `MapCenterResolver.kt:6` filters display only |
| O9 Cold start exceeds 20s timeout | Low | `SocketioSignalingClient.kt` `setTimeout(20000)` |
| O10 Synchronous relay cache writes | Low | `relay-server/index.js` `saveCache` |
| O16 Sequence restarts after fresh install | Low | `SensorService.kt` `onCreate`, `MonitorDatabase.kt` unique index |
| O28 Network fix replaces better GPS fix | Low | `MonitorLocationSource.kt` `fixes` |
| O29 Local trail never stored | Low | `TrailEntity.isLocalUser`, `TrailDao.pruneLocalTrail` |
| O30 Rows of unpaired trackers stay | Low | `TrailPruningWorker.doWork` |
| O33 Sensor keeps sending chunks after one fails | Low | `SensorService.kt` `setupBackfillListener` |
| O34 Sensor Setup fixed service status | Low | `AdminActivity.kt:576` |
| O35 Monitor `CLEAN` actions only unpair | Low | `MonitorActivity.kt` `CleanSetupOverlay` call, `core-ui/.../SetupOverlays.kt` |
| O36 Sensor service cannot be stopped | Low | `AdminActivity.kt` service button (about line 524), `SensorService` not exported |
| O37 Unknown values sent as numbers | Low | `SensorService.kt` `startSensingLoop` `update.speed`, `batteryReceiver` and its 100% / 30°C defaults |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

Line numbers come from Nov5.2 unless newer; recheck them before editing.

### Nov7.3 Fix Locations
`MonitorAlarms.kt`: `TRACKER_PRESENT_MS`, `AlarmType`, `allows`, `TrackerReading`, `newerReading`, `alarmConditions`, `AlarmBook.update`, `acknowledge`, `secondTicks`, `alarmBooks`. `SirenPlayer.kt`: `SirenSound`, `sirenSound`, `sirenWave`, `play`, `stop`. `MonitorAlarmEngine.kt`: `watch`, `stop`, `acknowledge`, `toggleSirenTest`, `showNotification`. `AlarmAlertScreen.kt`: `description`, `AlarmAlertScreen`. `AlarmOverlayActivity.kt`. `MonitorApp.kt`: channel IDs and `createMonitorNotificationChannels`. `MonitorSessionService.kt`: `onCreate`, `onStartCommand`, `onDestroy`. `MonitorViewModel.kt`: `isSirenPlaying`, `alarms`, `acknowledgeAlarms`, `testSirenAudio`, `TRACKER_PRESENT_MS`. `MonitorActivity.kt`: permission request and alarm screen.

### Nov7.2 Fix Locations
`core-ui/.../FixReading.kt`: `FixReading`, `toFixReading`, `metersPerSecondToKmh`, `motionState`, `withLocalReadings`. `app-sensor/.../SensorFixStore.kt`: `fix`, `publish`, `clear`. `SensorService.kt`: `fixStore`, `locationCallback`, `onDestroy`. `SensorViewModel.kt`: constructor `fixStore`, `hudState`. `MonitorHudValues.kt`: `LocalFix` only. `MonitorLocationSource.kt`: `toLocalFix`. `MonitorViewModel.kt`: imports and `localHudState`.

### Nov7.1 Fix Locations
`MonitorActivity.kt`: `MonitorScreen` `showUnpairConfirm`, `requestUnpair`, and the unpair `AlertDialog`; the `CleanSetupOverlay` call; `MonitorMenuBar` parameter `onUnpair`.

### Nov7.0 Fix Locations
`AppVersion.kt` in `core-ui`: `getAppVersionName`. `SensorService.kt`: `onCreate`. `AdminActivity.kt` and `MonitorActivity.kt`: imports only, their copies are removed.

### Nov6.4 Fix Locations
`MonitorDatabase.kt`: `SequenceGap`, `getSequenceGaps`, `getFirstSequenceId`, `countRows`. `BackfillLedger.kt`: `gapsToRequest`, `onRequestSent`, `onChunkReceived`, `onRequestComplete`. `MonitorRepository.kt`: `gapScans`, `observeGapScans`, `processIncomingUpdate`, `findGaps`, `requestMissingRanges`. `SocketioSignalingClient.kt`: `BURST_CAPACITY`.

### Nov6.3 Fix Locations
`HudState.kt`: `relayStatusText`. `MonitorActivity.kt`: `MonitorScreen`, Setup dialog `Tracker ID` and `relayStatusText` lines.

### Nov6.2 Fix Locations
`MonitorDatabase.kt`: `MAX_STORED_TRAIL_POINTS`, `getTrackerIds`, `pruneOldestRows`. `TrailPruningWorker.kt`: `doWork`, `schedule`. `MonitorApp.kt`: `workerFactory`, `workManagerConfiguration`, `onCreate`. `app-monitor/src/main/AndroidManifest.xml`: `InitializationProvider` entry.

### Nov6.1 Fix Locations
`HudState.kt`: `NO_VALUE`, `speedText`, `telemetryText`. `BatteryReading.kt`: `batteryReading`, `readBattery`. `FixReading.kt` (moved from `MonitorHudValues.kt` in Nov7.2): `motionState`, `withLocalReadings`. `MonitorViewModel.kt`: `localFix`, `localHudState`, `remoteHudState`. `MonitorLocationSource.kt`: `fixes`.

### Nov6.0 Fix Locations
`ForensicBuffer.kt`: `loadHeader`, `addChecksums`, `isHeaderConsistent`, `rebuildHeader`, `loadSlot`, `storeSlot`, `commitHeader`, `nextSequenceId`, `writeTrace`. Use only absolute `get` and `put` in that class: `position()` and the other calls that JDK 9+ redeclares are missing on older Android.

## 🟢 Status: VERIFIED ON A15 THROUGH THE PRODUCTION RELAY (Nov7.3)

Version Nov7.3 delivers ISSUE-NOV5.2-O11 group 1, slice 1:
1. **Four Monitor alarms**: tracker offline, power disconnected, battery low, temperature high, each on its own switch and Global Mute.
2. **Until acknowledged**: full-screen alarm, siren, and notification until `ACKNOWLEDGE ALARM`; a cleared alarm stays marked `(cleared)`; re-raised only after its condition clears.
3. **A real siren**: synthesized Siren, Chimes, and Pulse waves following the Siren & Audio settings; `TEST SIREN AUDIO` plays it.
4. **Alarm channel fixed**: separate high `monitor_alarm` and low `monitor_session` channels, defined once; the lowered shared channel is deleted.
5. **Sensor stays silent**: `SensorStealthGuardTest` (Rule 14).

**Resumption Token**: NOV7.3-QUEUE-O11-SLICE2-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.2-O11 group 1 slice 2

1. Group 1 slice 2, communication alarms: relay disconnected (`relayConnection`), the Monitor's own internet lost (`localInternet`), GPS signal loss (`signalLoss`), and GPS jump (`jammerDetection`), built on `MonitorAlarms.kt` and `MonitorAlarmEngine`. Then slice 3, Sensor-detected violations, which needs `SentinelLogic` (group 3), the violation flow, and a relay change the user pushes. Ask the user to decide groups 2 to 8, and O29, O30, O35, O36, and O39. Fix O13 after group 3 is decided.
2. When the user allows the S21FE: repeat the outage on two phones through the production relay (second half of 125.7), then continue the interrupted field test: a real drive for false MOVING (O13) and relay cold start (O9), alarms across two phones, and `TESTS\device_audit.cmd` for each phone. Decide on the nested repository changes (O19).
3. Fix O23, O21, O22, O24, O28, O33, O34, O37, and O38.
4. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov7.3)
- **Version Increment**: ✅ **VERIFIED** (`Nov7.3`, `versionCode` 42 monitor, 44 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (222/222; instrumented tests pinned to A15).
- **Deliberate Breaks**: ✅ **PASSED**: an unacknowledged alarm dropped on clearing failed `anUnacknowledgedAlarmStaysAfterItsConditionClears`; restoring the maximum instead of the saved volume failed `aForcedMaximumVolumeIsRestoredWhenTheSirenStops`. Both restored.
- **Alarms On Hardware (129.7)**: ✅ **PASSED** at low volume: power, offline, battery, and temperature alarms raised, silenced, kept as cleared, and re-armed, also with the Monitor in the background.
- **Locked-Screen Full-Screen Start**: ⚪ **NOT RUN**; an unlocked phone shows a heads-up instead (ISSUE-NOV7.3-O39).
- **Maximum-Volume Siren On Hardware**: ⚪ **NOT RUN** by user decision; automated proof only.
- **Forensic Audit**: ✅ **PASSED**: 0 Davey, StackLog, or fatal entries from either process, over about 7 minutes.
- **Two Phones**: ❌ **NOT RUN for Nov7.3**: the S21FE was not attached.
- **Carried From Nov6.4**: ❌ **NOT RUN**: more holes than the limit live, the request timeout live, the outage on the production relay and two phones (second half of 125.7), and the outage at the 10s battery rate.
- **Stress Protocol**: ⚪ **NOT RUN**: last result is in `TESTS/test_results.md` under Nov6.2.
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss (Nov6.0)**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **Physical Chapters**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
- `app-monitor/.../MonitorAlarms.kt`, `SirenPlayer.kt`, `MonitorAlarmEngine.kt`, `AlarmAlertScreen.kt`, `AlarmOverlayActivity.kt`, `MonitorApp.kt`; tests `app-monitor/src/test/.../MonitorAlarmsTest.kt`, `SirenSoundTest.kt`, `app-monitor/src/androidTest/.../MonitorAlarmDeviceTest.kt`, `core-ui/src/test/.../SensorStealthGuardTest.kt`: Chapter 129.
- `core-ui/.../FixReading.kt`, `core-ui/src/test/.../FixReadingTest.kt`, `app-sensor/.../SensorFixStore.kt`, `app-sensor/src/androidTest/.../SensorHudValuesTest.kt`: Chapter 128.
- `app-monitor/.../MonitorActivity.kt`, `app-monitor/src/androidTest/.../UnpairConfirmationTest.kt`: Chapter 127.
- `core-ui/.../AppVersion.kt`, `core-ui/src/test/.../VersionLiteralGuardTest.kt`, `app-sensor/src/androidTest/.../AppVersionTest.kt`: Chapter 126.
- `app-monitor/.../MonitorRepository.kt`, `BackfillLedger.kt`, `MonitorDatabase.kt`: gap scan, request tracking, gap query.
- `core-network/.../SocketioSignalingClient.kt`: burst capacity.
- `app-monitor/src/test/.../BackfillLedgerTest.kt`, `app-monitor/src/androidTest/.../BackfillGapRecoveryTest.kt`: Chapter 125.
- `core-ui/.../HudState.kt`, `app-monitor/.../MonitorActivity.kt`: relay status text and Setup dialog.
- `app-monitor/.../MonitorDatabase.kt`, `TrailPruningWorker.kt`, `MonitorApp.kt`: bounded trail storage.
- `core-ui/.../HudState.kt`, `BatteryReading.kt`, `FixReading.kt`: real HUD values and their text.
- `core-shared/.../ForensicBuffer.kt`: entry checksum, sequence reservation, header rebuild, version upgrade.
- `app-sensor/.../SensorService.kt`: `nextSequenceId` seed, `writeTrace` before `sendTelemetry`, backfill through `readRange`, fix publishing.
- `TESTS/CHAPTER_TRIAGE.md`, `TESTS/RETIRED_CHAPTERS.md`, `TESTS/device_audit.cmd`: triage, retired wording, audit tool.

---

## ⚠️ Operational Notes
- `TESTS/device_audit.cmd <serial> <package>` is the real Step 2 audit; `ProductionReadinessAuditTest` only checks math.
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- Run the Monitor and Sensor `connectedDebugAndroidTest` tasks in separate Gradle calls, in the background. Two calls joined with `&` in one `cmd /c` run one after the other and worked in Nov7.3.
- To run one instrumented class, add `-Pandroid.testInstrumentationRunnerArguments.class=<fully qualified class>`; to run one unit class, add `--tests <fully qualified class> --rerun`.
- Instrumented tests run under `HiltTestApplication`, so `MonitorApp.onCreate` does not run; anything a service needs from it must also be set up by the service, as `MonitorSessionService` does for its channels.
- `MonitorAlarmDeviceTest` plays the siren at gain 0 and restores the phone's alarm volume. Before Nov7.3's fix it left the A15 alarm volume changed; the A15 value is 1 of 15. Read it with `adb -s R58X40GV2AR shell "cmd media_session volume --stream 4 --get"`; `dumpsys audio` lists each `setStreamVolume` with its old value.
- To trigger alarms on one phone: `dumpsys battery unplug` and `dumpsys battery set status 3` (power), `dumpsys battery set level 15` (battery), `dumpsys battery set temp 470` (temperature), `am force-stop com.jd.app.sensor` (offline after about 35 seconds). Always finish with `dumpsys battery reset`. The `ACKNOWLEDGE ALARM` button is at about (540,1381) with one alarm and (540,1480) with two; read the bounds first. Alarm logs use the tags `REV_Alarm` and `REV_Siren`.
- `cleanTest` does not reset Android unit test tasks. Force them with `--rerun-tasks` or a per-task `--rerun`, and check the result file times.
- `app-sensor` has no unit tests; all 42 of its tests are instrumented.
- `VersionLiteralGuardTest` and `SensorStealthGuardTest` scan other modules, which Gradle does not track as their inputs. Run `:core-ui:testDebugUnitTest` with `--rerun`.
- Instrumented runs uninstall the Sensor and the Monitor. Reinstall with `adb -s R58X40GV2AR install -r -g app-sensor\build\outputs\apk\debug\app-sensor-debug.apk` and the same for `app-monitor`. Open `AdminActivity`, tap to pass `System Idle` (about (540,1138)), and tap `START SENSOR SERVICE` (about (540,1350)). Read the bounds first. Then open the Monitor and tap `Pair Device` (about (540,1110)); the ID field already holds `T`. The packages are `com.jd.app.sensor` and `com.jd.app.monitor`.
- On the Monitor, `Setup` is at about (1031,134); its dialog has `ALERTS` and `SIREN` buttons (`SIREN` at about (330,1384)). In Siren & Audio, `Force Maximum` is the switch at about (126,1568), `TEST SIREN AUDIO` at about (291,491), and `CLOSE (X)` at about (164,233). The A15 runs a right-to-left system language, so switches sit on the left.
- The Sensor service cannot be stopped from the shell: `am stopservice` returns `Error stopping service` because it is not exported. Only `am force-stop` ends it (ISSUE-NOV7.2-O36).
- To read the A15 screen, run `uiautomator dump /sdcard/ui.xml`, pull it with `adb exec-out cat` and `cmd` redirection into `build\`, and search it with the Grep tool. Delete both copies afterwards. To open the notification shade use `cmd statusbar expand-notifications`.
- To compare the Sensor HUD with the phone, run `dumpsys location` and read the `fused` line. Do not copy its coordinates into documents: they are real positions.
- Piping `adb logcat -d` into Windows `findstr` can return nothing; filter on the phone instead, for example `adb -s R58X40GV2AR shell "logcat -d -s REV_Alarm REV_Siren | tail -n 4"`. `dumpsys notification` keeps cancelled notifications in `mArchive`; count only the part before it.
- A new periodic WorkManager job runs at once, and `cmd jobscheduler run -f` does not run it again before its period ends. To see a second run, wait the 15 minutes.
- Reciprocal simulation: copy `relay-server/index.js` to a temp folder, run it with `NODE_PATH` set to `relay-server\node_modules`, and use `adb reverse tcp:3000 tcp:3000` with relay URL `http://127.0.0.1:3000`. This keeps `device_cache.json` and production clean. Uninstall the Monitor afterwards to remove simulated rows.
- Sensor-only outage on one phone: give the Sensor its own port and put a small Node TCP proxy between it and the private relay; change each app's relay URL before starting the service or pairing, so nothing reaches production.
- To read the Monitor table, pull `monitor_db`, `monitor_db-wal`, and `monitor_db-shm` with `adb exec-out run-as com.jd.app.monitor cat databases/<file>` and `cmd` redirection, then open it with Node 22 `node:sqlite`. Delete the copies afterwards: they hold real positions.
- S21FE safety: IDE `ui_state`, `take_screenshot`, `adb_shell_input`, and `deploy` may target it. For A15 work use `adb -s R58X40GV2AR` only. Run `adb devices` first, and check the foreground package before any capture or input.
- A15 baseline: font_scale 1.3, accelerometer_rotation 1, user_rotation 0, night mode on, Wi-Fi off, mobile_data 1, stay_on_while_plugged_in 15, alarm volume 1 of 15. The lock is swipe only. If `NotificationShade` has focus, run `cmd statusbar collapse`.
- To cut the A15 network use `svc data disable`, and restore it in the same command with `svc data enable`. Its Wi-Fi is already off.
- Shell: use `shell=cmd` with `pty=false` for short commands, and run `.\gradlew.bat`. In `cmd`, join commands with `&`, not `;`. Avoid `<` and `>` inside quoted `adb shell` commands. `2>$null` in `cmd` creates a file named `$null`; run `git status` before release. Foreground waits over about 60 seconds can time out; use PowerShell `Start-Process cmd.exe -ArgumentList '/c', $c -WindowStyle Hidden -PassThru`, then short polls. `timeout` does not work in this shell; use `Start-Sleep` in PowerShell.
- Sensor log tags are `REV_<tag>`, such as `REV_ForensicBuffer` and `REV_SensorService`. The Monitor uses the same form, such as `REV_Pruning`, `REV_Backfill`, `REV_Alarm`, and `REV_Siren`.
- `relay-server` is a separate Git repository. Changes reach production only after a push; Render then auto-deploys.
- Never commit `relay-server/device_cache.json`, and do not send simulated positions to the production relay.
- After a reboot the Sensor restarts by itself, but this can take a few minutes.

---

## ✅ Verified Milestones (Nov7.3)
1. **Automated Suite**: 222/222 on the final code: `:core-shared` 46, `:core-network` 18, `:core-ui` 29, `:app-monitor` unit 43, `:app-monitor` on A15 44, `:app-sensor` on A15 42.
2. **Hardware**: On the A15 through the production relay at low volume, with both apps at `Nov7.3` and paired with `T`: a simulated unplug raised `Tracker T power disconnected` within about a second with the full-screen alarm, the siren on the alarm stream, and the alarm notification; `ACKNOWLEDGE ALARM` silenced it; it returned after re-arming with the Monitor in the background, where its notification opened `AlarmOverlayActivity`; a force-stopped Sensor raised `Tracker offline` after about 36 seconds and stayed as `(cleared)` until acknowledged; a simulated 15% battery and 47°C raised both alarms. Both processes logged 0 Davey, StackLog, or fatal entries over about 7 minutes.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov7.3/`; tag `Nov7.3` is applied by the Git block above.

**Handover complete. System state locked at Nov7.3.**
