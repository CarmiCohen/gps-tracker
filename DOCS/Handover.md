git add .
git commit -m "Nov7.2 - REAL SENSOR HUD VALUES

- ISSUE-NOV6.1-O27 ROOT CAUSE: The Sensor's own HUD always showed STATIONARY, 0.0 km/h, A:+-5.0m, and AGE:0s. SensorViewModel.hudState hardcoded them, because the real fix lived only inside SensorService, where the screen could not read it.
- ISSUE-NOV6.1-O27 FIX: SensorService publishes each fix to a new @Singleton SensorFixStore and clears it in onDestroy. SensorViewModel.hudState reads the store through withLocalReadings. With no fix, motion state, speed, accuracy, and age show --.
- SHARED RULE: withLocalReadings, motionState, metersPerSecondToKmh, and a new Location.toFixReading moved from app-monitor to core-ui FixReading.kt, so the Sensor HUD and the Monitor local line use one function. LocalFix now holds position and a FixReading.
- TESTS: FixReadingTest (9) in core-ui replaces MonitorHudValuesTest (7). New SensorHudValuesTest (5) on the A15 against the real SensorViewModel and SensorFixStore. With the hardcoded values put back, 3 of the 5 failed; the fix was then restored. 194 of 194 automated tests pass, including 81 on the A15.
- LIVE: On the A15 through the production relay, with both apps at Nov7.2: the Sensor HUD showed -- before the service started, then A:+-22.4m and 1.3 km/h, matching the fused fix in dumpsys location (hAcc=22.4, vel=0.3656 m/s), with an age counting between fixes. Monitor paired with T showed live values on both lines. Sensor and Monitor logged 0 Davey, StackLog, or fatal entries over about 2 minutes. The S21FE was not used.
- ISSUE-NOV7.2-O36 ADDED: the Sensor service cannot be stopped on the device, so the stop step of chapter 128.4 has automated proof only. Decide with ISSUE-NOV5.2-O11.
- ISSUE-NOV7.2-O37 ADDED: unknown speed is sent as 0, so the Monitor sensor line can show 0.0 km/h where the Sensor HUD shows --.
- ISSUE-NOV5.2-O13 NOTE: the Sensor HUD now uses the same motionState rule, so one fix covers all three HUD lines.
- IDEA ADDED: SIMP-NOV7.2-S1, one fix copy in the Sensor.
- DOCS: Added Chapter 128, Rule 1.364, Chapter 5.71, and baseline STATUS/BASELINE/Nov7.2/. Root Handover.md is synchronized. QA is 51 of 118 active chapters.
- VERSION: Nov7.2, versionCode 41 (monitor) and 43 (sensor)."
git tag Nov7.2
git push origin main --tags

# Project Handover - Version Nov7.2 (Real Sensor HUD Values)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.2 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.2 commit and tag exist. The previous commit is `e6592be` (Nov7.1). There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. The next Work Queue item in `DOCS/issues.md` is ISSUE-NOV5.2-O11, which needs user decisions; if the user is not ready, take ISSUE-NOV6.0-O23, the first item that needs nothing. Display each issue before fixing it (Rule 1).
- **Relay**: v6.044 is on production: the field test on 2026-11-06 saw backfill work through it (ISSUE-NOV5.2-O12, closed). No relay code changed in Nov6.4, Nov7.0, Nov7.1, or Nov7.2. Relay pushes restart production, so the user runs them.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov7.2 debug Sensor on the default relay with its service started at 11:42:47, and the Nov7.2 debug Monitor paired with `T` on the default relay. Both were reinstalled after the instrumented runs, so the Sensor sequence restarted and the Monitor table is new. S21FE (`R5CRC14PG4F`, API 36) is a personal phone and is unplugged. It holds the Nov6.3 Monitor and the field-test table, with sequences above the restarted Sensor (ISSUE-NOV5.2-O16). Before the next field test, install the current version and clear that Monitor's data, with the user's agreement. The user allowed installing the Monitor on it. Do not run instrumented tests on it, and do not touch `com.gps19.app`. Run `adb devices` first and name the device in every command.
- **Correction (from Nov7.0)**: the S21FE was attached at 14:51:41 during the Nov6.3 Monitor suite, not at about 14:53 as the Nov6.3 commit message says. Tooling read its properties only; its adbd log shows no install, uninstall, or test.
- **Pending user actions**: make the O11, O19, O29, O30, O35, and O36 decisions when the queue reaches them; allow S21FE use for the second half of chapter 125.7; decide later on a device or emulator below API 35 for O26 (**do not download one until the user says so**); optionally add the shell permission rule to `.claude/settings.local.json`, which Claude cannot write itself; optionally run `setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | T rows in `TESTS/CHAPTER_TRIAGE.md`, `HydrationStaggeringAuditTest.kt`, `WatchdogRecoveryTest.kt`, chapter 116.3 |
| O4 Remaining H and S chapters | Medium | `TESTS/CHAPTER_TRIAGE.md` |
| O13 False MOVING | Medium | `core-ui/.../FixReading.kt` `motionState`, used by all three HUD lines |
| O21 HUD flows not limited to 2Hz | Low | `MonitorViewModel.kt:85` and HUD `combine` blocks |
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
| O37 Unknown speed sent as 0 | Low | `SensorService.kt` `startSensingLoop` `update.speed = loc.speed` |

Line numbers come from Nov5.2 unless newer; recheck them before editing.

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
`HudState.kt`: `NO_VALUE`, `speedText`, `telemetryText`. `BatteryReading.kt`: `batteryReading`, `readBattery`. `MonitorHudValues.kt` (moved to `core-ui/.../FixReading.kt` in Nov7.2): `LocalFix`, `motionState`, `withLocalReadings`. `MonitorViewModel.kt`: `localFix`, `localHudState`, `remoteHudState`. `MonitorLocationSource.kt`: `fixes`.

### Nov6.0 Fix Locations
`ForensicBuffer.kt`: `loadHeader`, `addChecksums`, `isHeaderConsistent`, `rebuildHeader`, `loadSlot`, `storeSlot`, `commitHeader`, `nextSequenceId`, `writeTrace`. Use only absolute `get` and `put` in that class: `position()` and the other calls that JDK 9+ redeclares are missing on older Android.

## 🟢 Status: VERIFIED ON A15 THROUGH THE PRODUCTION RELAY (Nov7.2)

Version Nov7.2 delivers these changes (ISSUE-NOV6.1-O27):
1. **Real fix on the Sensor HUD**: `SensorService` publishes each fix to `SensorFixStore`, and `SensorViewModel.hudState` shows its motion state, speed, accuracy, and age.
2. **No invented values**: with no fix, or before the service starts, these values show `--`.
3. **One rule for both apps**: the Sensor HUD and the Monitor local line both use `withLocalReadings` in `core-ui`.
4. **Guarded**: `SensorHudValuesTest` fails if the hardcoded values return.

**Resumption Token**: NOV7.2-QUEUE-O11-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.2-O11

1. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule (O11). Group the decisions by feature, and include O29, O30, O35, and O36. Fix O13 afterwards, since it depends on whether `SentinelLogic` is wired in; the fix belongs in `core-ui` `motionState`, which all three HUD lines use. If the user is not ready to decide, start with step 3.
2. When the user allows the S21FE: repeat the outage on two phones through the production relay (second half of 125.7), then continue the interrupted field test: a real drive for false MOVING (O13) and relay cold start (O9), and `TESTS\device_audit.cmd` for each phone. Decide on the nested repository changes (O19).
3. Fix O23, O21, O22, O24, O28, O33, O34, and O37.
4. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov7.2)
- **Version Increment**: ✅ **VERIFIED** (`Nov7.2`, `versionCode` 41 monitor, 43 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (194/194; instrumented tests pinned to A15).
- **Deliberate Break**: ✅ **PASSED**: with the hardcoded values put back in `SensorViewModel`, 3 of 5 `SensorHudValuesTest` tests failed on the A15, then the fix was restored and the full suite passed.
- **Sensor HUD On Hardware (128.4)**: ✅ **PASSED**: `--` before the service started; then `A:±22.4m` and `1.3 km/h`, matching the fused fix in `dumpsys location`, with the age counting 0 to 4 seconds between fixes.
- **Stop Step Of 128.4**: ⚪ **NOT RUN**: no control stops the service, and `am stopservice` is refused (ISSUE-NOV7.2-O36); automated proof only.
- **Forensic Audit**: ✅ **PASSED**: 0 Davey, StackLog, or fatal entries from either process, over about 2 minutes.
- **Two Phones**: ❌ **NOT RUN for Nov7.2**: the S21FE was not attached.
- **Carried From Nov6.4**: ❌ **NOT RUN**: more holes than the limit live, the request timeout live, the outage on the production relay and two phones (second half of 125.7), and the outage at the 10s battery rate.
- **Stress Protocol**: ⚪ **NOT RUN**: last result is in `TESTS/test_results.md` under Nov6.2.
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss (Nov6.0)**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **Physical Chapters**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
- `core-ui/.../FixReading.kt`, `core-ui/src/test/.../FixReadingTest.kt`, `app-sensor/.../SensorFixStore.kt`, `app-sensor/src/androidTest/.../SensorHudValuesTest.kt`: Chapter 128.
- `app-monitor/.../MonitorActivity.kt`, `app-monitor/src/androidTest/.../UnpairConfirmationTest.kt`: Chapter 127.
- `core-ui/.../AppVersion.kt`, `core-ui/src/test/.../VersionLiteralGuardTest.kt`, `app-sensor/src/androidTest/.../AppVersionTest.kt`: Chapter 126.
- `app-monitor/.../MonitorRepository.kt`, `BackfillLedger.kt`, `MonitorDatabase.kt`: gap scan, request tracking, gap query.
- `core-network/.../SocketioSignalingClient.kt`: burst capacity.
- `app-monitor/src/test/.../BackfillLedgerTest.kt`, `app-monitor/src/androidTest/.../BackfillGapRecoveryTest.kt`: Chapter 125.
- `core-ui/.../HudState.kt`, `app-monitor/.../MonitorActivity.kt`: relay status text and Setup dialog.
- `app-monitor/src/androidTest/.../RelayStatusDialogTest.kt`: Chapter 124.
- `app-monitor/.../MonitorDatabase.kt`, `TrailPruningWorker.kt`, `MonitorApp.kt`: bounded trail storage.
- `app-monitor/src/androidTest/.../TrailStorageBoundTest.kt`: Chapter 123.
- `core-ui/.../HudState.kt`, `BatteryReading.kt`, `FixReading.kt`: real HUD values and their text.
- `core-shared/.../ForensicBuffer.kt`: entry checksum, sequence reservation, header rebuild, version upgrade.
- `app-sensor/.../SensorService.kt`: `nextSequenceId` seed, `writeTrace` before `sendTelemetry`, backfill through `readRange`, fix publishing.
- `TESTS/CHAPTER_TRIAGE.md`, `TESTS/RETIRED_CHAPTERS.md`, `TESTS/device_audit.cmd`: triage, retired wording, audit tool.

---

## ⚠️ Operational Notes
- `TESTS/device_audit.cmd <serial> <package>` is the real Step 2 audit; `ProductionReadinessAuditTest` only checks math.
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- Run the Monitor and Sensor `connectedDebugAndroidTest` tasks in separate Gradle calls, in the background. In one call they failed with `Starting 0 tests` and install errors.
- To run one instrumented class, add `-Pandroid.testInstrumentationRunnerArguments.class=<fully qualified class>`; this was used for the Nov7.1 and Nov7.2 deliberate breaks.
- `cleanTest` does not reset Android unit test tasks, so `cleanTest test` can run nothing. Force them with `--rerun-tasks` or a per-task `--rerun`, and check the result file times.
- `app-sensor` has no unit tests; all 42 of its tests are instrumented.
- `VersionLiteralGuardTest` scans other modules, which Gradle does not track as its inputs. Run `:core-ui:testDebugUnitTest` with `--rerun`, or a new version string can pass unseen.
- Instrumented runs uninstall the Sensor and the Monitor. Reinstall with `adb -s R58X40GV2AR install -r -g app-sensor\build\outputs\apk\debug\app-sensor-debug.apk` and the same for `app-monitor`. Open `AdminActivity`, tap to pass `System Idle` (Nov7.2: at about (540,1138)), and tap `START SENSOR SERVICE` (Nov7.2: about (540,1350)). Read its bounds first, because the layout shifts with URL length. Then open the Monitor and tap `Pair Device` (Nov7.2: about (540,1110)); the ID field already holds `T`. The packages are `com.jd.app.sensor` and `com.jd.app.monitor`.
- The Sensor service cannot be stopped from the shell: `am stopservice` returns `Error stopping service` because it is not exported. Only `am force-stop` ends it (ISSUE-NOV7.2-O36).
- To read the A15 screen, run `uiautomator dump /sdcard/ui.xml`, pull it with `adb exec-out cat` and `cmd` redirection into `build\`, and search it with the Grep tool. Delete both copies afterwards. In Nov7.1 the Monitor gear was at (947,134), `Disconnect` at (60,134), and the dialog's `Cancel` and `Unpair` at (565,1372) and (287,1372); recheck the bounds before tapping.
- To compare the Sensor HUD with the phone, run `dumpsys location` and read the `fused` line: `hAcc` is the HUD accuracy, and `vel` in m/s times 3.6 is the HUD speed. Do not copy its coordinates into documents: they are real positions.
- Piping `adb logcat -d` into Windows `findstr` can return nothing; filter on the phone instead, for example `adb -s R58X40GV2AR shell "logcat -d --pid=$(pidof com.jd.app.sensor) | grep -E 'onCreate called'"`.
- A new periodic WorkManager job runs at once, and `cmd jobscheduler run -f` does not run it again before its period ends. To see a second run, wait the 15 minutes.
- Reciprocal simulation: copy `relay-server/index.js` to a temp folder, run it with `NODE_PATH` set to `relay-server\node_modules`, and use `adb reverse tcp:3000 tcp:3000` with relay URL `http://127.0.0.1:3000`. This keeps `device_cache.json` and production clean. Node 22 has a built-in `WebSocket`, so no client package is needed. Uninstall the Monitor afterwards to remove simulated rows.
- Sensor-only outage on one phone: removing an `adb reverse` entry does not cut an open socket, so it causes no outage. Give the Sensor its own port and put a small Node TCP proxy between it and the private relay; to cut, discard the data, then close and refuse connections. Change each app's relay URL before starting the service or pairing, so nothing reaches production. On the Sensor the path is Setup, `CHANGE RELAY URL`; `RESET DEFAULT` saves at once.
- To read the Monitor table, pull `monitor_db`, `monitor_db-wal`, and `monitor_db-shm` with `adb exec-out run-as com.jd.app.monitor cat databases/<file>` and `cmd` redirection, then open it with Node 22 `node:sqlite`. Delete the copies afterwards: they hold real positions.
- S21FE safety: IDE `ui_state`, `take_screenshot`, `adb_shell_input`, and `deploy` may target it. For A15 work use `adb -s R58X40GV2AR` only, including `uiautomator dump` to read the screen. Run `adb devices` first, and check the foreground package before any capture or input.
- A15 baseline: font_scale 1.3, accelerometer_rotation 1, user_rotation 0, night mode on, Wi-Fi off, mobile_data 1, stay_on_while_plugged_in 15. The lock is swipe only. If `NotificationShade` has focus, run `cmd statusbar collapse`.
- To cut the A15 network use `svc data disable`, and restore it in the same command with `svc data enable`. Its Wi-Fi is already off.
- Shell: use `shell=cmd` with `pty=false` for short commands, and run `.\gradlew.bat`, not `gradlew.bat`. In `cmd`, join commands with `&`, not `;`, and `%VAR%` set earlier on the same line is not expanded. Avoid `<` and `>` inside quoted `adb shell` commands: `cmd` treats them as redirects and can leave stray files in the repository. `2>$null` in `cmd` creates a file named `$null`; run `git status` before release. Foreground waits over about 60 seconds can time out. A `cmd` background start can report a timeout and either still run or not run at all, so check for its log before retrying. The reliable way is PowerShell `Start-Process cmd.exe -ArgumentList '/c', $c -WindowStyle Hidden -PassThru`, then short polls. `echo %ERRORLEVEL%` on the same line as the command is expanded before it runs, so read the log or result files instead. `timeout` does not work in this shell; use `Start-Sleep` in PowerShell.
- Sensor log tags are `REV_<tag>`, such as `REV_ForensicBuffer` and `REV_SensorService`. The Monitor uses the same form, such as `REV_Pruning` and `REV_Backfill`.
- `relay-server` is a separate Git repository. Changes reach production only after a push; Render then auto-deploys.
- Never commit `relay-server/device_cache.json`, and do not send simulated positions to the production relay.
- After a reboot the Sensor restarts by itself, but this can take a few minutes.

---

## ✅ Verified Milestones (Nov7.2)
1. **Automated Suite**: 194/194 on the final code: `:core-shared` 46, `:core-network` 18, `:core-ui` 26, `:app-monitor` unit 23, `:app-monitor` on A15 39, `:app-sensor` on A15 42.
2. **Hardware**: On the A15 through the production relay, with both apps at `Nov7.2`: before `START SENSOR SERVICE` the Sensor HUD showed `--`, `-- km/h`, and `A:--|AGE:--`; after it, `STATIONARY`, `1.3 km/h`, and `A:±22.4m|AGE:2s`, matching the fused fix in `dumpsys location`; later samples followed the GPS from `±13.8m` to `±12.8m` with the age counting. The Monitor, paired with `T`, showed live values on both lines. The Sensor logged `onCreate called. Version: Nov7.2`. Both processes logged 0 Davey, StackLog, or fatal entries over about 2 minutes.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov7.2/`; tag `Nov7.2` is applied by the Git block above.

**Handover complete. System state locked at Nov7.2.**
