git add .
git commit -m "Nov6.3 - REAL RELAY STATUS TEXT

- ISSUE-NOV5.0-O5 ROOT CAUSE: The Monitor Setup dialog always said Relay Status: Connected / Active, even with SRV red, because the text was a fixed string.
- ISSUE-NOV5.0-O5 FIX: HudState.relayStatusText is built from isRelayConnected, the field that drives SRV, so the dialog and the LED cannot disagree. It shows Connected or Disconnected and updates while the dialog is open.
- ISSUE-NOV5.0-O5 LABEL: Connected Tracker ID became Tracker ID. It claimed a connection while showing only the stored pairing, and the requirement says Tracker ID.
- TESTS: Added HudStateTest.relayStatusTextFollowsTheSrvState and RelayStatusDialogTest, which renders the real MonitorScreen and opens the dialog. Both dialog tests failed when the old text was put back. 156 of 156 automated tests pass, including 60 on the A15.
- LIVE: On the A15 through the production relay, with the dialog open, the text changed to Disconnected within 5 seconds of cutting mobile data and back to Connected within 5 seconds of restoring it. The SRV LED colour was not read during that check. Sensor and Monitor logged 0 Davey, StackLog, or fatal entries. The S21FE was connected to the PC from about 14:53 but was not used.
- IDEA ADDED: SIMP-NOV6.3-S1, delete the unused HudViewState.
- DOCS: Added Chapter 124, Rule 1.360, Chapter 5.67, and baseline STATUS/BASELINE/Nov6.3/. Root Handover.md is synchronized. QA is 47 of 114 active chapters.
- VERSION: Nov6.3, versionCode 37 (monitor) and 39 (sensor)."
git tag Nov6.3
git push origin main --tags

# Project Handover - Version Nov6.3 (Real Relay Status Text)

## ▶️ Resumption State (Read First)
- **Version**: Nov6.3 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov6.3 commit and tag exist. The previous commit is `e4e4985` (Nov6.2). There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. Continue the Work Queue in `DOCS/issues.md`, one item at a time, starting with ISSUE-NOV5.0-O6. Display each issue before fixing it (Rule 1).
- **Relay**: v6.044 is pushed as `b4ec94a` and production answers, but no endpoint reports the version, so v6.044 is not confirmed running. Backfill (120.7) still has to be verified on production (ISSUE-NOV5.2-O12). Relay pushes restart production, so the user runs them.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov6.3 debug Sensor on the default relay with its service started, and the Nov6.3 debug Monitor paired to Tracker `T`. S21FE (`R5CRC14PG4F`) is a personal phone, last recorded on Nov5.1, and must not be touched until the user allows it. It was connected to the PC from about 14:53 on 2026-11-06, so run `adb devices` first and name the A15 in every command.
- **Pending user actions**: make the O11, O19, O29, and O30 decisions when the queue reaches them; allow S21FE use for O12; decide later on a device or emulator below API 35 for O26 (**do not download one until the user says so**); optionally add the shell permission rule to `.claude/settings.local.json`, which Claude cannot write itself; optionally run `setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O12 Relay v6.044 not verified on production | Medium | `relay-server/index.js:123-124` |
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
| O31 Unanswered backfill requests kept | Low | `MonitorRepository.activeRequests`, `triggerBackfill` |

Line numbers come from Nov5.2; recheck them before editing.

### Nov6.3 Fix Locations
`HudState.kt`: `relayStatusText`. `MonitorActivity.kt`: `MonitorScreen`, Setup dialog `Tracker ID` and `relayStatusText` lines.

### Nov6.2 Fix Locations
`MonitorDatabase.kt`: `MAX_STORED_TRAIL_POINTS`, `getTrackerIds`, `pruneOldestRows`. `TrailPruningWorker.kt`: `doWork`, `schedule`. `MonitorApp.kt`: `workerFactory`, `workManagerConfiguration`, `onCreate`. `app-monitor/src/main/AndroidManifest.xml`: `InitializationProvider` entry.

### Nov6.1 Fix Locations
`HudState.kt`: `NO_VALUE`, `speedText`, `telemetryText`. `BatteryReading.kt`: `batteryReading`, `readBattery`. `MonitorHudValues.kt`: `LocalFix`, `motionState`, `withLocalReadings`. `MonitorViewModel.kt`: `localFix`, `localHudState`, `remoteHudState`. `MonitorLocationSource.kt`: `fixes`.

### Nov6.0 Fix Locations
`ForensicBuffer.kt`: `loadHeader`, `addChecksums`, `isHeaderConsistent`, `rebuildHeader`, `loadSlot`, `storeSlot`, `commitHeader`, `nextSequenceId`, `writeTrace`. Use only absolute `get` and `put` in that class: `position()` and the other calls that JDK 9+ redeclares are missing on older Android.

## 🟡 Status: VERIFIED ON A15, NOT RUN BELOW API 35 (Nov6.3)

Version Nov6.3 delivers these changes (ISSUE-NOV5.0-O5):
1. **Real status**: The Monitor Setup dialog shows `Relay Status: Connected` or `Relay Status: Disconnected`, from the same state as the `SRV` LED, and updates while open.
2. **Honest label**: `Connected Tracker ID:` is now `Tracker ID:`, since it shows the stored pairing and not a connection.

**Resumption Token**: NOV6.3-QUEUE-O6-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.0-O6

1. Fix O6 and O15, which need no product decision.
2. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule (O11). Group the decisions by feature, and include O29 and O30. Fix O13 afterwards, since it depends on whether `SentinelLogic` is wired in.
3. Confirm backfill on the production relay with a real Sensor and Monitor pair (O12), and decide on the nested repository changes (O19).
4. Fix O23, O21, O22, O24, O27, O28, and O31.
5. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov6.3)
- **Version Increment**: ✅ **VERIFIED** (`Nov6.3`, `versionCode` 37 monitor, 39 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (156/156; instrumented tests pinned to A15).
- **Deliberate Break**: ✅ **PASSED**: both `RelayStatusDialogTest` tests failed with the old text, then the fix was restored.
- **A15 Live Check**: ✅ **PASSED**: with the dialog open, the status followed a real mobile data loss and recovery, each within 5 seconds. The `SRV` LED colour was not read during this check.
- **Reciprocal Simulation**: ⚪ **NOT RUN**: this change sends no telemetry, so both apps ran on the production relay instead.
- **Stress Protocol**: ⚪ **NOT RUN**: this change touches no load path. Last result is in `TESTS/test_results.md` under Nov6.2.
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss (Nov6.0)**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **S21FE**: ❌ **NOT USED**; it still runs Nov5.1 as far as recorded.
- **Physical Chapters**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
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
- S21FE safety: IDE `ui_state`, `take_screenshot`, `adb_shell_input`, and `deploy` may target it. For A15 work use `adb -s R58X40GV2AR` only, including `uiautomator dump` to read the screen. Run `adb devices` first, and check the foreground package before any capture or input.
- A15 baseline: font_scale 1.3, accelerometer_rotation 1, user_rotation 0, night mode on, Wi-Fi off, mobile_data 1, stay_on_while_plugged_in 15. The lock is swipe only. If `NotificationShade` has focus, run `cmd statusbar collapse`.
- To cut the A15 network use `svc data disable`, and restore it in the same command with `svc data enable`. Its Wi-Fi is already off.
- Shell: use `shell=cmd` with `pty=false`, and run `.\gradlew.bat`, not `gradlew.bat`. In `cmd`, join commands with `&`, not `;`. Avoid `<` and `>` inside quoted `adb shell` commands: `cmd` treats them as redirects and can leave stray files in the repository. Foreground waits over about 60 seconds can time out, and a background start can report a timeout yet still run, so check before retrying. `echo %ERRORLEVEL%` on the same line as the command is expanded before it runs, so read the log or result files instead. `timeout` does not work in this shell; use `Start-Sleep` in PowerShell.
- Sensor log tags are `REV_<tag>`, such as `REV_ForensicBuffer` and `REV_SensorService`. The Monitor uses the same form, such as `REV_Pruning`.
- `relay-server` is a separate Git repository. Changes reach production only after a push; Render then auto-deploys.
- Never commit `relay-server/device_cache.json`, and do not send simulated positions to the production relay.
- After a reboot the Sensor restarts by itself, but this can take a few minutes.

---

## ✅ Verified Milestones (Nov6.3)
1. **Automated Suite**: 156/156 on the final code: `:core-shared` 46, `:core-network` 16, `:core-ui` 14, `:app-monitor` unit 20, `:app-monitor` on A15 25, `:app-sensor` on A15 35.
2. **Hardware**: The Setup dialog followed a real network loss and recovery on the A15, and the Sensor and Monitor processes logged 0 Davey, StackLog, or fatal entries.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov6.3/`; tag `Nov6.3` is applied by the Git block above.

**Handover complete. System state locked at Nov6.3.**
