git add .
git commit -m "Nov6.4 - BACKFILL GAP RECOVERY

- ISSUE-NOV6.3-O32 ROOT CAUSE: A gap existed only at the moment it was detected. Any request that was over the limit, could not be sent, or was never answered lost its rows for good: the two-phone field test left 162 rows missing in 13 holes after a 60-second outage.
- ISSUE-NOV6.3-O32 FIX: The Monitor reads gaps from the trails table with TrailDao.getSequenceGaps, so a hole stays findable until it is filled. The new BackfillLedger keeps the limit of 10 requests in flight, takes the newest gaps first, and does not repeat an answered range in the same process. A table pruned to its cap does not request its deleted rows.
- ISSUE-NOV6.3-O32 DROPPED UPDATES: SocketioSignalingClient silently dropped updates when its flow buffers filled: with a busy collector only 64 of 600 telemetry updates and 8 of 200 backfill chunks got through. The flows now hold a full burst (BURST_CAPACITY, 5000). This is a worst-case test; it likely explains the sparse arrival in the field test, but that was not confirmed there.
- ISSUE-NOV6.2-O31: An unanswered request is released after 30 seconds without a chunk and asked again by a later scan. Scans run only while telemetry arrives. The timeout was not observed on a device.
- ISSUE-NOV5.2-O12: Closed on the field test of 2026-11-06 with the Nov6.3 apps: 11 requests reached the Sensor through the production relay and 11 chunks returned.
- TESTS: Added BackfillLedgerTest, BackfillGapRecoveryTest, and two burst tests in SignalingBatchTest. Limiting the scan to the newest gap made two recovery tests fail. 177 of 177 automated tests pass, including 69 on the A15.
- LIVE: On the A15 through a private relay, a 60-second Sensor outage at 10Hz left one gap of 195 rows, refilled about 2 seconds after reconnecting; the table then held 3,011 rows with no holes. Sensor and Monitor logged 0 Davey, StackLog, or fatal entries. That outage produced one hole, so more holes than the limit has automated proof only. Not run on the production relay or on two phones.
- NEW ISSUES: ISSUE-NOV6.4-O33, the Sensor keeps sending chunks after one fails. ISSUE-NOV6.4-O34, Sensor Setup shows a fixed service status.
- IDEA ADDED: SIMP-NOV6.4-S1, forward relay batches as one event.
- CORRECTION: The S21FE was attached at 14:51:41 during the Nov6.3 Monitor suite, not at about 14:53 as the Nov6.3 commit message says. Working documents are corrected.
- DOCS: Added Chapter 125, Rule 1.361, Chapter 5.68, and baseline STATUS/BASELINE/Nov6.4/. Root Handover.md is synchronized. QA is 48 of 115 active chapters.
- VERSION: Nov6.4, versionCode 38 (monitor) and 40 (sensor)."
git tag Nov6.4
git push origin main --tags

# Project Handover - Version Nov6.4 (Backfill Gap Recovery)

## ▶️ Resumption State (Read First)
- **Version**: Nov6.4 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov6.4 commit and tag exist. The previous commit is `06d4103` (Nov6.3). There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. Continue the Work Queue in `DOCS/issues.md`, one item at a time, starting with ISSUE-NOV5.0-O6. Display each issue before fixing it (Rule 1).
- **Relay**: v6.044 is on production: the field test on 2026-11-06 saw backfill work through it (ISSUE-NOV5.2-O12, closed). No relay code changed in Nov6.4. Relay pushes restart production, so the user runs them.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov6.4 debug Sensor on the default relay with its service started. Its sequence restarted at 1 after the instrumented run. Its Nov6.4 debug Monitor was cleared with `pm clear`, reinstalled with `-g`, and force-stopped, so it needs pairing. S21FE (`R5CRC14PG4F`, API 36) is a personal phone and is unplugged. It holds the Nov6.3 Monitor and the field-test table, with sequences above the restarted Sensor (ISSUE-NOV5.2-O16). Before the next field test, install Nov6.4 and clear that Monitor's data, with the user's agreement. The user allowed installing the Monitor on it. Do not run instrumented tests on it, and do not touch `com.gps19.app`. Run `adb devices` first and name the device in every command.
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
| O6 Stale version strings | Low | `SensorService.kt:107`, `MonitorActivity.kt:232,234`, `AdminActivity.kt:253,255` |
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

## 🟡 Status: VERIFIED ON A15 THROUGH A PRIVATE RELAY, NOT RUN ON PRODUCTION OR TWO PHONES (Nov6.4)

Version Nov6.4 delivers these changes (ISSUE-NOV6.3-O32, ISSUE-NOV6.2-O31, ISSUE-NOV5.2-O12):
1. **No forgotten gaps**: Missing ranges are read from the stored trail, so a range that could not be requested or was never answered is requested again.
2. **Limit delays, never loses**: At most 10 requests are in flight, newest gaps first; the rest wait for a free slot.
3. **Released requests**: An unanswered request is released after 30 seconds without a chunk.
4. **No dropped bursts**: The signaling client holds a full reconnect burst instead of silently dropping updates.

**Resumption Token**: NOV6.4-QUEUE-O6-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.0-O6

1. Fix O6 and O15, which need no product decision.
2. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule (O11). Group the decisions by feature, and include O29 and O30. Fix O13 afterwards, since it depends on whether `SentinelLogic` is wired in.
3. When the user allows the S21FE: repeat the outage on two phones through the production relay (second half of 125.7), then continue the interrupted field test: a real drive for false MOVING (O13) and relay cold start (O9), and `TESTS\device_audit.cmd` for each phone. Decide on the nested repository changes (O19).
4. Fix O23, O21, O22, O24, O27, O28, O33, and O34.
5. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov6.4)
- **Version Increment**: ✅ **VERIFIED** (`Nov6.4`, `versionCode` 38 monitor, 40 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (177/177; instrumented tests pinned to A15).
- **Deliberate Break**: ✅ **PASSED**: limiting the scan to the newest gap made two recovery tests fail, then the fix was restored.
- **Outage On Hardware (125.7)**: ✅ **PASSED through a private relay**: one gap of 195 rows refilled; 3,011 rows and no holes afterwards.
- **More Holes Than The Limit, Live**: ⚪ **NOT OBSERVED**: that outage produced one hole; automated proof only.
- **Unanswered Request Timeout, Live**: ⚪ **NOT OBSERVED**: automated proof only.
- **Production Relay And Two Phones**: ❌ **NOT RUN for Nov6.4**: the S21FE was unplugged.
- **10s Battery Rate**: ❌ **NOT RUN**: the outage ran at the 10Hz charging rate.
- **Stress Protocol**: ⚪ **NOT RUN**: last result is in `TESTS/test_results.md` under Nov6.2.
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss (Nov6.0)**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **Physical Chapters**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
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
- `app-sensor` has no unit tests; all 35 of its tests are instrumented.
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

## ✅ Verified Milestones (Nov6.4)
1. **Automated Suite**: 177/177 on the final code: `:core-shared` 46, `:core-network` 18, `:core-ui` 14, `:app-monitor` unit 30, `:app-monitor` on A15 34, `:app-sensor` on A15 35.
2. **Hardware**: A 60-second Sensor outage on the A15 through a private relay left one gap of 195 rows, refilled about 2 seconds after reconnecting; the table then held 3,011 rows, sequences 1 to 3011, with no holes. The Sensor and Monitor processes logged 0 Davey, StackLog, or fatal entries.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov6.4/`; tag `Nov6.4` is applied by the Git block above.

**Handover complete. System state locked at Nov6.4.**
