git add .
git commit -m "Nov6.1 - REAL HUD VALUES

- ISSUE-NOV5.0-O2 LOCAL HUD: The Monitor local line now shows the phone's own battery, charging state, temperature, fix accuracy, fix age, speed, and motion state instead of hardcoded values.
- ISSUE-NOV5.0-O2 SENSOR LINE: Removed the 100% and 30C fallbacks. Before any telemetry the line shows -- and no number. Speed needs a known position.
- ISSUE-NOV5.0-O2 UNKNOWN VALUES: HudState uses null for an unknown battery, temperature, or speed and owns the HUD text, so HudStateTest checks production formatting instead of a copy.
- ISSUE-NOV5.0-O2 OLD FIX: Local speed and state are dropped once the fix is 35 seconds old, because the 10m update filter sends no fix after the Monitor stops. Accuracy and age stay.
- ISSUE-NOV5.0-O2 SHARED CODE: BatteryReading in core-ui replaces the private battery code and invented fallback in SensorViewModel. MonitorLocationSource now emits LocalFix. motionState gives both lines one rule.
- OPEN GAPS ADDED: ISSUE-NOV6.1-O27, the Sensor's own HUD still hardcodes speed, accuracy, age, and state. ISSUE-NOV6.1-O28, a coarse network fix can replace a better GPS fix on a stationary Monitor.
- TESTS: Added BatteryReadingTest and MonitorHudValuesTest; rewrote the HudStateTest format test. 145 of 145 automated tests pass, including 50 on the A15.
- LIVE: Sensor and Monitor on the A15 through the production relay; the local line matched dumpsys battery and location. 0 Davey, StackLog, or fatal entries from either process. The -- states and the 35 second limit have automated coverage only. The S21FE was not used.
- DOCS: Added Chapter 122, Rule 1.358, Chapter 5.65, idea SIMP-NOV6.1-S1, and baseline STATUS/BASELINE/Nov6.1/. QA is 44 of 112 active chapters.
- VERSION: Nov6.1, versionCode 35 (monitor) and 37 (sensor)."
git tag Nov6.1
git push origin main --tags

# Project Handover - Version Nov6.1 (Real HUD Values)

## ▶️ Resumption State (Read First)
- **Version**: Nov6.1 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov6.1 commit and tag exist. The previous commit is `a33bb46` (Nov6.0). There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. Continue the Work Queue in `DOCS/issues.md`, one item at a time, starting with ISSUE-NOV5.2-O14. Display each issue before fixing it (Rule 1).
- **Relay**: v6.044 is pushed as `b4ec94a` and production answers HTTP 200, but no endpoint reports the version, so v6.044 is not confirmed running. Backfill (120.7) still has to be verified on production (ISSUE-NOV5.2-O12). Relay pushes restart production, so the user runs them.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov6.1 debug Sensor on the default relay with its service started, and the Nov6.1 debug Monitor paired to Tracker `T`. Its forensic file is new because instrumented runs uninstall the app. It was the only phone attached on 2026-11-06. S21FE (`R5CRC14PG4F`) is a personal phone, last recorded on Nov5.1, and must not be touched until the user allows it.
- **Pending user actions**: make the O11 decisions and the O19 decision when the queue reaches them; allow S21FE use for O12; decide later on a device or emulator below API 35 for O26 (**do not download one until the user says so**); optionally add the shell permission rule to `.claude/settings.local.json`, which Claude cannot write itself; optionally run `setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O12 Relay v6.044 not verified on production | Medium | `relay-server/index.js:123-124` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | T rows in `TESTS/CHAPTER_TRIAGE.md`, `HydrationStaggeringAuditTest.kt`, `WatchdogRecoveryTest.kt` |
| O27 Sensor HUD hardcoded values | Medium | `SensorViewModel.kt` `hudState` |
| O4 Remaining H and S chapters | Medium | `TESTS/CHAPTER_TRIAGE.md` |
| O13 False MOVING | Medium | `MonitorViewModel.kt:164` |
| O14 Trail storage unbounded | Medium | `TrailPruningWorker.kt` never scheduled |
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

Line numbers come from Nov5.2; recheck them before editing.

### Nov6.1 Fix Locations
`HudState.kt`: `NO_VALUE`, `speedText`, `telemetryText`. `BatteryReading.kt`: `batteryReading`, `readBattery`. `MonitorHudValues.kt`: `LocalFix`, `motionState`, `withLocalReadings`. `MonitorViewModel.kt`: `localFix`, `localHudState`, `remoteHudState`. `MonitorLocationSource.kt`: `fixes`.

### Nov6.0 Fix Locations
`ForensicBuffer.kt`: `loadHeader`, `addChecksums`, `isHeaderConsistent`, `rebuildHeader`, `loadSlot`, `storeSlot`, `commitHeader`, `nextSequenceId`, `writeTrace`. Use only absolute `get` and `put` in that class: `position()` and the other calls that JDK 9+ redeclares are missing on older Android.

## 🟡 Status: VERIFIED ON A15, NOT RUN BELOW API 35 (Nov6.1)

Version Nov6.1 delivers these changes (ISSUE-NOV5.0-O2):
1. **Local HUD line**: It shows the Monitor phone's own battery, temperature, accuracy, age, speed, and motion state.
2. **Unknown values**: Both Monitor lines show `--` instead of an invented number.
3. **Old fixes**: Local speed and state are dropped once the fix is 35 seconds old.

**Behavior change**: on a stationary Monitor the local line often shows speed `--`, because the newest fix may be a network fix with no speed (ISSUE-NOV6.1-O28). A Sensor that has sent no telemetry now shows `--`, not 100% and 30°C.

**Resumption Token**: NOV6.1-QUEUE-O14-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.2-O14

1. Fix O14, O5, O6, and O15, which need no product decision.
2. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule (O11). Group the decisions by feature. Fix O13 afterwards, since it depends on whether `SentinelLogic` is wired in.
3. Confirm backfill on the production relay with a real Sensor and Monitor pair (O12), and decide on the nested repository changes (O19).
4. Fix O23, O21, O22, O24, O27, and O28.
5. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov6.1)
- **Version Increment**: ✅ **VERIFIED** (`Nov6.1`, `versionCode` 35 monitor, 37 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (145/145; instrumented tests pinned to A15).
- **A15 Live Pass**: ✅ **PASSED**: Sensor and Monitor through the production relay, local line matched `dumpsys`, clean audit for both processes. The `--` states and the 35-second limit were not observed live, and the Monitor did not move.
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss (Nov6.0)**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **S21FE**: ❌ **NOT USED**; it still runs Nov5.1.
- **Physical Chapters & Stress Protocol**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
- `core-ui/.../HudState.kt`, `BatteryReading.kt`, `app-monitor/.../MonitorHudValues.kt`: real HUD values and their text.
- `core-ui/src/test/.../HudStateTest.kt`, `BatteryReadingTest.kt`, `app-monitor/src/test/.../MonitorHudValuesTest.kt`: Chapter 122.
- `core-shared/.../ForensicBuffer.kt`: entry checksum, sequence reservation, header rebuild, version upgrade.
- `core-shared/src/test/.../ForensicEntryIntegrityTest.kt`, `ForensicBufferApiLevelTest.kt`, `SequenceContinuityTest.kt`: Chapter 121 and 120.1.
- `app-sensor/.../SensorService.kt`: `nextSequenceId` seed, `writeTrace` before `sendTelemetry`, backfill through `readRange`.
- `TESTS/CHAPTER_TRIAGE.md`, `TESTS/RETIRED_CHAPTERS.md`, `TESTS/device_audit.cmd`: triage, retired wording, audit tool.

---

## ⚠️ Operational Notes
- `TESTS/device_audit.cmd <serial> <package>` is the real Step 2 audit; `ProductionReadinessAuditTest` only checks math.
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- Run the Monitor and Sensor `connectedDebugAndroidTest` tasks in separate Gradle calls, in the background. In one call they failed with `Starting 0 tests` and install errors.
- Instrumented runs uninstall the Sensor. Reinstall with `adb -s R58X40GV2AR install -r -g app-sensor\build\outputs\apk\debug\app-sensor-debug.apk`, open `AdminActivity`, tap to pass `System Idle`, and tap `START SENSOR SERVICE`. Read its bounds first, because the layout shifts with URL length. The Sensor package is `com.jd.app.sensor`.
- S21FE safety: IDE `ui_state`, `take_screenshot`, `adb_shell_input`, and `deploy` may target it. For A15 work use `adb -s R58X40GV2AR` only. Run `adb devices` first, and check the foreground package before any capture or input.
- A15 baseline: font_scale 1.3, accelerometer_rotation 1, user_rotation 0, night mode on, Wi-Fi off, mobile_data 1, stay_on_while_plugged_in 15. The lock is swipe only. If `NotificationShade` has focus, run `cmd statusbar collapse`.
- Shell: use `shell=cmd` with `pty=false`, and run `.\gradlew.bat`, not `gradlew.bat`. Avoid `<` and `>` inside quoted `adb shell` commands: `cmd` treats them as redirects and can leave stray files in the repository.
- Sensor log tags are `REV_<tag>`, such as `REV_ForensicBuffer` and `REV_SensorService`.
- `relay-server` is a separate Git repository. Changes reach production only after a push; Render then auto-deploys.
- Never commit `relay-server/device_cache.json`, and do not send simulated positions to the production relay.
- After a reboot the Sensor restarts by itself, but this can take a few minutes.

---

## ✅ Verified Milestones (Nov6.1)
1. **Automated Suite**: 145/145 on the final code: `:core-shared` 46, `:core-network` 16, `:core-ui` 13, `:app-monitor` unit 20, `:app-monitor` on A15 15, `:app-sensor` on A15 35.
2. **Hardware**: Local HUD line matched `dumpsys` on the A15, with 0 Davey, StackLog, or fatal entries from the Sensor and Monitor processes.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov6.1/`; tag `Nov6.1` is applied by the Git block above.

**Handover complete. System state locked at Nov6.1.**
