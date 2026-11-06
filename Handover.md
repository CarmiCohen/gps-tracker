git add .
git commit -m "Nov6.2 - BOUNDED TRAIL STORAGE

- ISSUE-NOV5.2-O14 ROW CAP: Each tracker now keeps its newest 50,000 trail rows, about 4.4MB, in place of unlimited growth. TrailDao.pruneOldestRows uses the primary key and needs no window functions, which minSdk 26 lacks.
- ISSUE-NOV5.2-O14 COUNT, NOT AGE: Remote rows are limited by count, so an idle tracker keeps its newest rows and getLastSequenceId cannot fall to 0 and request a full backfill.
- ISSUE-NOV5.2-O14 SCHEDULING: TrailPruningWorker runs about every 15 minutes as unique periodic work, scheduled from MonitorApp.
- ISSUE-NOV5.2-O14 WORKER FACTORY: The worker could not have run before. MonitorApp now implements Configuration.Provider with HiltWorkerFactory, and the manifest removes the default WorkManagerInitializer.
- OPEN GAPS ADDED: ISSUE-NOV6.2-O29, the Monitor's own trail is documented but never stored. ISSUE-NOV6.2-O30, rows of a tracker no longer paired stay stored, up to the cap each. ISSUE-NOV6.2-O31, unanswered backfill requests are never released.
- ISSUE-NOV6.0-O23 NOTE: Chapter 116.3 and rule 1.348 say VWR turns GREEN, but HudStatusBar shows it in cyan by design.
- ISSUE-NOV5.1-O9 NOTE: Recovery after a connection timeout was seen once on production, in 3 seconds.
- TESTS: Added TrailStorageBoundTest with 8 tests, including 50,100 rows at the real limit, and work-testing. 153 of 153 automated tests pass, including 58 on the A15. Run 1 failed 1 test on a wrong test assumption; it was corrected and the complete suite restarted.
- RECIPROCAL SIMULATION: One phone was attached, so a private relay copy was used through adb reverse. A simulated viewer tested the Sensor and a simulated tracker tested the Monitor. No simulated data reached the production relay.
- LIVE: The installed Monitor held 59,799 rows, and its periodic job removed exactly 9,799 at 14:39:03, 15 minutes after its first run, with no false backfill. Stress section 4 passed. Both apps then ran on the production relay with 0 Davey, StackLog, or fatal entries. The S21FE was not used.
- DOCS: Added Chapter 123, Rule 1.359, Chapter 5.66, idea SIMP-NOV6.2-S1, and baseline STATUS/BASELINE/Nov6.2/. Chapter 50 is now verified. Corrected the trail limits in MAP_FUNCTIONALITY_SPEC.md and 03_MONITOR_REQUIREMENTS.md. Root Handover.md is synchronized again. QA is 46 of 113 active chapters.
- VERSION: Nov6.2, versionCode 36 (monitor) and 38 (sensor)."
git tag Nov6.2
git push origin main --tags

# Project Handover - Version Nov6.2 (Bounded Trail Storage)

## ▶️ Resumption State (Read First)
- **Version**: Nov6.2 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov6.2 commit and tag exist. The previous commit is `205df74` (Nov6.1). There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. Continue the Work Queue in `DOCS/issues.md`, one item at a time, starting with ISSUE-NOV5.0-O5. Display each issue before fixing it (Rule 1).
- **Relay**: v6.044 is pushed as `b4ec94a` and production answers, but no endpoint reports the version, so v6.044 is not confirmed running. Backfill (120.7) still has to be verified on production (ISSUE-NOV5.2-O12). Relay pushes restart production, so the user runs them.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov6.2 debug Sensor on the default relay with its service started, and the Nov6.2 debug Monitor paired to Tracker `T`. Both were freshly installed after the simulation, so they hold no simulated rows and their forensic file is new. The old `com.jd.core.network.test` package was removed from the A15. It was the only phone attached on 2026-11-06. S21FE (`R5CRC14PG4F`) is a personal phone, last recorded on Nov5.1, and must not be touched until the user allows it.
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
| O5 False relay status text | Low | `MonitorActivity.kt:473` |
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

### Nov6.2 Fix Locations
`MonitorDatabase.kt`: `MAX_STORED_TRAIL_POINTS`, `getTrackerIds`, `pruneOldestRows`. `TrailPruningWorker.kt`: `doWork`, `schedule`. `MonitorApp.kt`: `workerFactory`, `workManagerConfiguration`, `onCreate`. `app-monitor/src/main/AndroidManifest.xml`: `InitializationProvider` entry.

### Nov6.1 Fix Locations
`HudState.kt`: `NO_VALUE`, `speedText`, `telemetryText`. `BatteryReading.kt`: `batteryReading`, `readBattery`. `MonitorHudValues.kt`: `LocalFix`, `motionState`, `withLocalReadings`. `MonitorViewModel.kt`: `localFix`, `localHudState`, `remoteHudState`. `MonitorLocationSource.kt`: `fixes`.

### Nov6.0 Fix Locations
`ForensicBuffer.kt`: `loadHeader`, `addChecksums`, `isHeaderConsistent`, `rebuildHeader`, `loadSlot`, `storeSlot`, `commitHeader`, `nextSequenceId`, `writeTrace`. Use only absolute `get` and `put` in that class: `position()` and the other calls that JDK 9+ redeclares are missing on older Android.

## 🟡 Status: VERIFIED ON A15, NOT RUN BELOW API 35 (Nov6.2)

Version Nov6.2 delivers these changes (ISSUE-NOV5.2-O14):
1. **Row cap**: Each tracker keeps its newest 50,000 trail rows, about 4.4MB.
2. **Scheduling**: A background job enforces the cap about every 15 minutes.
3. **Worker creation**: `MonitorApp` supplies the Hilt worker factory, so `@HiltWorker` classes can now run.

**Behavior change**: older trail rows are now deleted. At the 10Hz charging rate 50,000 rows hold about 1.4 hours of history; at the 10s rate, about 5.8 days. The map still shows the newest 1000 points. A tracker can exceed the cap by about 9,000 rows between runs at 10Hz. A first run on a phone with a very large table deletes everything over the cap in one transaction; that case was tested at 59,799 rows only.

**Resumption Token**: NOV6.2-QUEUE-O5-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.0-O5

1. Fix O5, O6, and O15, which need no product decision.
2. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule (O11). Group the decisions by feature, and include O29 and O30. Fix O13 afterwards, since it depends on whether `SentinelLogic` is wired in.
3. Confirm backfill on the production relay with a real Sensor and Monitor pair (O12), and decide on the nested repository changes (O19).
4. Fix O23, O21, O22, O24, O27, O28, and O31.
5. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov6.2)
- **Version Increment**: ✅ **VERIFIED** (`Nov6.2`, `versionCode` 36 monitor, 38 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (153/153; instrumented tests pinned to A15; Run 2 after one test correction).
- **Reciprocal Simulation**: ✅ **PASSED** in both directions through a private relay.
- **A15 Pruning**: ✅ **PASSED**: 9,799 of 59,799 rows removed by the periodic job, with no false backfill.
- **Stress Protocol**: 🟡 **PARTIAL**: section 4 rotation passed at a 0.5Hz stream; sections 1, 2, and 3 have no controls (D), and section 5 needs a person (H).
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss (Nov6.0)**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **S21FE**: ❌ **NOT USED**; it still runs Nov5.1.
- **Physical Chapters**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
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
- Instrumented runs uninstall the Sensor. Reinstall with `adb -s R58X40GV2AR install -r -g app-sensor\build\outputs\apk\debug\app-sensor-debug.apk`, open `AdminActivity`, tap to pass `System Idle`, and tap `START SENSOR SERVICE`. Read its bounds first, because the layout shifts with URL length. The Sensor package is `com.jd.app.sensor`.
- A new periodic WorkManager job runs at once, and `cmd jobscheduler run -f` does not run it again before its period ends. To see a second run, wait the 15 minutes.
- Reciprocal simulation: copy `relay-server/index.js` to a temp folder, run it with `NODE_PATH` set to `relay-server\node_modules`, and use `adb reverse tcp:3000 tcp:3000` with relay URL `http://127.0.0.1:3000`. This keeps `device_cache.json` and production clean. Node 22 has a built-in `WebSocket`, so no client package is needed. Uninstall the Monitor afterwards to remove simulated rows.
- S21FE safety: IDE `ui_state`, `take_screenshot`, `adb_shell_input`, and `deploy` may target it. For A15 work use `adb -s R58X40GV2AR` only. Run `adb devices` first, and check the foreground package before any capture or input.
- A15 baseline: font_scale 1.3, accelerometer_rotation 1, user_rotation 0, night mode on, Wi-Fi off, mobile_data 1, stay_on_while_plugged_in 15. The lock is swipe only. If `NotificationShade` has focus, run `cmd statusbar collapse`.
- Shell: use `shell=cmd` with `pty=false`, and run `.\gradlew.bat`, not `gradlew.bat`. In `cmd`, join commands with `&`, not `;`. Avoid `<` and `>` inside quoted `adb shell` commands: `cmd` treats them as redirects and can leave stray files in the repository. Foreground waits over about 60 seconds can time out, and a background start can report a timeout yet still run, so check before retrying.
- Sensor log tags are `REV_<tag>`, such as `REV_ForensicBuffer` and `REV_SensorService`. The Monitor uses the same form, such as `REV_Pruning`.
- `relay-server` is a separate Git repository. Changes reach production only after a push; Render then auto-deploys.
- Never commit `relay-server/device_cache.json`, and do not send simulated positions to the production relay.
- After a reboot the Sensor restarts by itself, but this can take a few minutes.

---

## ✅ Verified Milestones (Nov6.2)
1. **Automated Suite**: 153/153 on the final code: `:core-shared` 46, `:core-network` 16, `:core-ui` 13, `:app-monitor` unit 20, `:app-monitor` on A15 23, `:app-sensor` on A15 35.
2. **Hardware**: The installed Monitor pruned 9,799 of 59,799 rows on its own schedule, and the Sensor and Monitor processes logged 0 Davey, StackLog, or fatal entries.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov6.2/`; tag `Nov6.2` is applied by the Git block above.

**Handover complete. System state locked at Nov6.2.**
