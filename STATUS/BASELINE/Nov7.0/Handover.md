git add .
git commit -m "Nov7.0 - REAL VERSION STRINGS

- ISSUE-NOV5.0-O6 ROOT CAUSE: Versions were written by hand in the code, and getAppVersionName was copied in both apps. SensorService logged Version: Nov1.7 (Final) on every start, and the fallback in both apps was an invented Nov4.27.
- ISSUE-NOV5.0-O6 FIX: One getAppVersionName in core-ui AppVersion.kt reads the installed package and shows -- if that fails. The copies in AdminActivity and MonitorActivity are removed, and SensorService logs the same value.
- TESTS: Added VersionLiteralGuardTest, which fails if any production string in the five modules holds a release version, and AppVersionTest on the A15. With the old strings put back, the guard named both and anUnknownPackageShowsNoVersion failed; the fix was then restored. 182 of 182 automated tests pass, including 71 on the A15.
- LIVE: On the A15 through the production relay, dumpsys, the Sensor first screen, the Sensor Setup dialog, the Monitor pairing screen, and the Monitor Setup dialog all showed Nov7.0, and the Sensor logged onCreate called. Version: Nov7.0. Sensor and Monitor logged 0 Davey, StackLog, or fatal entries over about 2 minutes. The -- case has automated proof only. The S21FE was not used.
- ISSUE-NOV6.0-O23: Noted that the title and chapters 1.2 and 1.3 of TEST_PROCEDURE.md still name Nov2.24.
- IDEA ADDED: SIMP-NOV7.0-S1, a single version source for both apps.
- DOCS: Added Chapter 126, Rule 1.362, Chapter 5.69, and baseline STATUS/BASELINE/Nov7.0/. Root Handover.md is synchronized. QA is 49 of 116 active chapters.
- VERSION: Nov7.0, versionCode 39 (monitor) and 41 (sensor)."
git tag Nov7.0
git push origin main --tags

# Project Handover - Version Nov7.0 (Real Version Strings)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.0 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.0 commit and tag exist. The previous commit is `ba93a58` (Nov6.4). There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. Continue the Work Queue in `DOCS/issues.md`, one item at a time, starting with ISSUE-NOV5.2-O15. Display each issue before fixing it (Rule 1).
- **Relay**: v6.044 is on production: the field test on 2026-11-06 saw backfill work through it (ISSUE-NOV5.2-O12, closed). No relay code changed in Nov6.4 or Nov7.0. Relay pushes restart production, so the user runs them.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov7.0 debug Sensor on the default relay with its service started, and the Nov7.0 debug Monitor paired with `T` on the default relay. Both were reinstalled after the instrumented runs, so the Sensor sequence restarted and the Monitor table is new. S21FE (`R5CRC14PG4F`, API 36) is a personal phone and is unplugged. It holds the Nov6.3 Monitor and the field-test table, with sequences above the restarted Sensor (ISSUE-NOV5.2-O16). Before the next field test, install the current version and clear that Monitor's data, with the user's agreement. The user allowed installing the Monitor on it. Do not run instrumented tests on it, and do not touch `com.gps19.app`. Run `adb devices` first and name the device in every command.
- **Correction**: the S21FE was attached at 14:51:41 during the Nov6.3 Monitor suite, not at about 14:53 as the Nov6.3 commit message says. Tooling read its properties only; its adbd log shows no install, uninstall, or test.
- **Pending user actions**: make the O11, O19, O29, and O30 decisions when the queue reaches them; allow S21FE use for the second half of chapter 125.7; decide later on a device or emulator below API 35 for O26 (**do not download one until the user says so**); optionally add the shell permission rule to `.claude/settings.local.json`, which Claude cannot write itself; optionally run `setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | T rows in `TESTS/CHAPTER_TRIAGE.md`, `HydrationStaggeringAuditTest.kt`, `WatchdogRecoveryTest.kt`, chapter 116.3 |
| O27 Sensor HUD hardcoded values | Medium | `SensorViewModel.kt` `hudState` |
| O4 Remaining H and S chapters | Medium | `TESTS/CHAPTER_TRIAGE.md` |
| O13 False MOVING | Medium | `MonitorViewModel.kt:164` |
| O21 HUD flows not limited to 2Hz | Low | `MonitorViewModel.kt:85` and HUD `combine` blocks |
| O22 Rotation resets screen state | Low | `MonitorActivity.kt:258-267`, `AdminActivity.kt:83-88` |
| O24 `SignalingQueue` not thread safe | Low | `SignalingQueue.kt`, `SocketioSignalingClient.kt` `flushQueue` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O7 Zero-position rows stored | Low | `MapCenterResolver.kt:6` filters display only |
| O9 Cold start exceeds 20s timeout | Low | `SocketioSignalingClient.kt` `setTimeout(20000)` |
| O10 Synchronous relay cache writes | Low | `relay-server/index.js` `saveCache` |
| O15 Gear unpairs immediately | Low | `MonitorMenuBar`, `MonitorActivity.kt:563` |
| O16 Sequence restarts after fresh install | Low | `SensorService.kt:116`, `MonitorDatabase.kt` unique index |
| O28 Network fix replaces better GPS fix | Low | `MonitorLocationSource.kt` `fixes` |
| O29 Local trail never stored | Low | `TrailEntity.isLocalUser`, `TrailDao.pruneLocalTrail` |
| O30 Rows of unpaired trackers stay | Low | `TrailPruningWorker.doWork` |
| O33 Sensor keeps sending chunks after one fails | Low | `SensorService.kt` `setupBackfillListener` |
| O34 Sensor Setup fixed service status | Low | `AdminActivity.kt:576` |

Line numbers come from Nov5.2 unless newer; recheck them before editing.

### Nov7.0 Fix Locations
`AppVersion.kt` in `core-ui`: `getAppVersionName`. `SensorService.kt`: `onCreate`. `AdminActivity.kt` and `MonitorActivity.kt`: imports only, their copies are removed.

### Nov6.4 Fix Locations
`MonitorDatabase.kt`: `SequenceGap`, `getSequenceGaps`, `getFirstSequenceId`, `countRows`. `BackfillLedger.kt`: `gapsToRequest`, `onRequestSent`, `onChunkReceived`, `onRequestComplete`. `MonitorRepository.kt`: `gapScans`, `observeGapScans`, `processIncomingUpdate`, `findGaps`, `requestMissingRanges`. `SocketioSignalingClient.kt`: `BURST_CAPACITY`.

### Nov6.3 Fix Locations
`HudState.kt`: `relayStatusText`. `MonitorActivity.kt`: `MonitorScreen`, Setup dialog `Tracker ID` and `relayStatusText` lines.

### Nov6.2 Fix Locations
`MonitorDatabase.kt`: `MAX_STORED_TRAIL_POINTS`, `getTrackerIds`, `pruneOldestRows`. `TrailPruningWorker.kt`: `doWork`, `schedule`. `MonitorApp.kt`: `workerFactory`, `workManagerConfiguration`, `onCreate`. `app-monitor/src/main/AndroidManifest.xml`: `InitializationProvider` entry.

### Nov6.1 Fix Locations
`HudState.kt`: `NO_VALUE`, `speedText`, `telemetryText`. `BatteryReading.kt`: `batteryReading`, `readBattery`. `MonitorHudValues.kt`: `LocalFix`, `motionState`, `withLocalReadings`. `MonitorViewModel.kt`: `localFix`, `localHudState`, `remoteHudState`. `MonitorLocationSource.kt`: `fixes`.

### Nov6.0 Fix Locations
`ForensicBuffer.kt`: `loadHeader`, `addChecksums`, `isHeaderConsistent`, `rebuildHeader`, `loadSlot`, `storeSlot`, `commitHeader`, `nextSequenceId`, `writeTrace`. Use only absolute `get` and `put` in that class: `position()` and the other calls that JDK 9+ redeclares are missing on older Android.

## 🟢 Status: VERIFIED ON A15 THROUGH THE PRODUCTION RELAY (Nov7.0)

Version Nov7.0 delivers these changes (ISSUE-NOV5.0-O6):
1. **Real versions**: Every version shown or logged comes from the installed package; an unreadable one shows `--`.
2. **One function**: `getAppVersionName` lives in `core-ui`, in place of a copy in each app.
3. **Guarded**: `VersionLiteralGuardTest` fails if a production string holds a release version.

**Resumption Token**: NOV7.0-QUEUE-O15-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.2-O15

1. Fix O15, which needs no product decision.
2. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule (O11). Group the decisions by feature, and include O29 and O30. Fix O13 afterwards, since it depends on whether `SentinelLogic` is wired in.
3. When the user allows the S21FE: repeat the outage on two phones through the production relay (second half of 125.7), then continue the interrupted field test: a real drive for false MOVING (O13) and relay cold start (O9), and `TESTS\device_audit.cmd` for each phone. Decide on the nested repository changes (O19).
4. Fix O23, O21, O22, O24, O27, O28, O33, and O34.
5. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov7.0)
- **Version Increment**: ✅ **VERIFIED** (`Nov7.0`, `versionCode` 39 monitor, 41 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (182/182; instrumented tests pinned to A15).
- **Deliberate Break**: ✅ **PASSED**: with the old strings back, the guard named both and `anUnknownPackageShowsNoVersion` failed on the A15, then the fix was restored.
- **Version On Hardware (126.3)**: ✅ **PASSED**: all four version displays and the Sensor log showed `Nov7.0`.
- **Forensic Audit**: ✅ **PASSED**: 0 Davey, StackLog, or fatal entries from either process, over about 2 minutes.
- **Unknown Version, Live**: ⚪ **NOT OBSERVED**: an installed app can always read its package; automated proof only.
- **Two Phones**: ❌ **NOT RUN for Nov7.0**: the S21FE was not attached.
- **Carried From Nov6.4**: ❌ **NOT RUN**: more holes than the limit live, the request timeout live, the outage on the production relay and two phones (second half of 125.7), and the outage at the 10s battery rate.
- **Stress Protocol**: ⚪ **NOT RUN**: last result is in `TESTS/test_results.md` under Nov6.2.
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss (Nov6.0)**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **Physical Chapters**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
- `core-ui/.../AppVersion.kt`, `core-ui/src/test/.../VersionLiteralGuardTest.kt`, `app-sensor/src/androidTest/.../AppVersionTest.kt`: Chapter 126.
- `app-monitor/.../MonitorRepository.kt`, `BackfillLedger.kt`, `MonitorDatabase.kt`: gap scan, request tracking, gap query.
- `core-network/.../SocketioSignalingClient.kt`: burst capacity.
- `app-monitor/src/test/.../BackfillLedgerTest.kt`, `app-monitor/src/androidTest/.../BackfillGapRecoveryTest.kt`: Chapter 125.
- `core-ui/.../HudState.kt`, `app-monitor/.../MonitorActivity.kt`: relay status text and Setup dialog.
- `app-monitor/src/androidTest/.../RelayStatusDialogTest.kt`: Chapter 124.
- `app-monitor/.../MonitorDatabase.kt`, `TrailPruningWorker.kt`, `MonitorApp.kt`: bounded trail storage.
- `app-monitor/src/androidTest/.../TrailStorageBoundTest.kt`: Chapter 123.
- `core-ui/.../HudState.kt`, `BatteryReading.kt`, `app-monitor/.../MonitorHudValues.kt`: real HUD values and their text.
- `core-shared/.../ForensicBuffer.kt`: entry checksum, sequence reservation, header rebuild, version upgrade.
- `app-sensor/.../SensorService.kt`: `nextSequenceId` seed, `writeTrace` before `sendTelemetry`, backfill through `readRange`.
- `TESTS/CHAPTER_TRIAGE.md`, `TESTS/RETIRED_CHAPTERS.md`, `TESTS/device_audit.cmd`: triage, retired wording, audit tool.

---

## ⚠️ Operational Notes
- `TESTS/device_audit.cmd <serial> <package>` is the real Step 2 audit; `ProductionReadinessAuditTest` only checks math.
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- Run the Monitor and Sensor `connectedDebugAndroidTest` tasks in separate Gradle calls, in the background. In one call they failed with `Starting 0 tests` and install errors.
- `cleanTest` does not reset Android unit test tasks, so `cleanTest test` can run nothing. Force them with a per-task `--rerun`, and check the result file times.
- `app-sensor` has no unit tests; all 37 of its tests are instrumented.
- `VersionLiteralGuardTest` scans other modules, which Gradle does not track as its inputs. Run `:core-ui:testDebugUnitTest` with `--rerun`, or a new version string can pass unseen.
- Instrumented runs uninstall the Sensor and the Monitor. Reinstall with `adb -s R58X40GV2AR install -r -g app-sensor\build\outputs\apk\debug\app-sensor-debug.apk` and the same for `app-monitor`. Open `AdminActivity`, tap to pass `System Idle`, and tap `START SENSOR SERVICE`. Read its bounds first, because the layout shifts with URL length. Then open the Monitor and tap `Pair Device`; the ID field already holds `T`. The packages are `com.jd.app.sensor` and `com.jd.app.monitor`.
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

## ✅ Verified Milestones (Nov7.0)
1. **Automated Suite**: 182/182 on the final code: `:core-shared` 46, `:core-network` 18, `:core-ui` 17, `:app-monitor` unit 30, `:app-monitor` on A15 34, `:app-sensor` on A15 37.
2. **Hardware**: On the A15 through the production relay, `dumpsys`, the Sensor first screen, the Sensor Setup dialog, the Monitor pairing screen, and the Monitor Setup dialog all showed `Nov7.0`, and the Sensor logged `onCreate called. Version: Nov7.0`. The Sensor and Monitor processes logged 0 Davey, StackLog, or fatal entries over about 2 minutes.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov7.0/`; tag `Nov7.0` is applied by the Git block above.

**Handover complete. System state locked at Nov7.0.**
