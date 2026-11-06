git add .
git commit -m "Nov6.0 - FORENSIC ENTRY INTEGRITY & CHAPTER REVIEW

- ISSUE-NOV6.0-O20 FORENSIC INTEGRITY: Each ForensicBuffer entry now carries a CRC32 and damaged entries are skipped on read. The whole mapping is forced to storage, not only the header. A broken header is rebuilt from the entries, and a reset header is now written to the file.
- ISSUE-NOV6.0-O20 SEQUENCE: The header reserves sequence numbers, so none are reused after power loss. A restart can skip up to 100 numbers, which costs one empty backfill.
- ISSUE-NOV6.0-O20 UPGRADE: Buffer file version 3. Version 2 files upgrade in place and keep their entries; verified on a real full file on the A15.
- ISSUE-NOV6.0-O25 OLDER ANDROID: Removed force(int, int) and position(int), which were expected to crash the Sensor on its first write below API 35 while minSdk is 26. Reviewed every JDK method called by core-shared against API 26.
- ISSUE-NOV5.4-O17 CHAPTER REVIEW: Compared each A chapter with its test and the production code. Chapters 53, 66, 67, 68, 83, 88, and 93 lost their verified status, and chapter 87 stays active as D.
- ISSUE-NOV5.4-O18 RETIREMENT: Retired chapters 10, 47, 71, 78, 80, 84, 89, 92, 94, 117 and items 2.1 and 2.2, preserved in TESTS/RETIRED_CHAPTERS.md.
- OPEN GAPS ADDED: ISSUE-NOV6.0-O21 to O24 and O26. O26: the Sensor has never been run below API 35; by user decision no older emulator image is downloaded yet.
- TESTS: Added ForensicEntryIntegrityTest and ForensicBufferApiLevelTest; updated SequenceContinuityTest. 130 of 130 automated tests pass, including 50 on the A15.
- LIVE: A15 pass with a real file upgrade and three kill -9 recoveries; 0 Davey, StackLog, or fatal entries from the Sensor process. Interrupted writes are simulated; physical power loss was not run.
- DOCS: Added Chapter 121, Rule 1.357, Chapter 5.64, idea SIMP-NOV6.0-S1, and baseline STATUS/BASELINE/Nov6.0/. QA is 43 of 111 active chapters.
- VERSION: Nov6.0, versionCode 34 (monitor) and 36 (sensor)."
git tag Nov6.0
git push origin main --tags

# Project Handover - Version Nov6.0 (Forensic Entry Integrity & Chapter Review)

## ▶️ Resumption State (Read First)
- **Version**: Nov6.0 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov6.0 commit and tag exist. The previous commit is `0d6a348` (Nov5.2). There was no Nov5.3 or Nov5.4 release, and issue IDs containing NOV5.4 keep their names.
- **Next work**: the user names the next version. Continue the Work Queue in `DOCS/issues.md`, one item at a time, starting with ISSUE-NOV5.0-O2. Display each issue before fixing it (Rule 1).
- **Relay**: v6.044 is pushed as `b4ec94a` and production answers HTTP 200, but no endpoint reports the version, so v6.044 is not confirmed running. Backfill (120.7) still has to be verified on production (ISSUE-NOV5.2-O12). Relay pushes restart production, so the user runs them.
- **Phones**: A15 (`R58X40GV2AR`, API 36) runs the Nov6.0 debug Sensor on the default relay with its service started. Its forensic file is new because instrumented runs uninstall the app. The test copy of the Monitor is not installed. It was the only phone attached on 2026-11-06. S21FE (`R5CRC14PG4F`) is a personal phone, last recorded on Nov5.1, and must not be touched until the user allows it.
- **Pending user actions**: make the O11 decisions and the O19 decision when the queue reaches them; allow S21FE use for O12; decide later on a device or emulator below API 35 for O26 (**do not download one until the user says so**); optionally add the shell permission rule to `.claude/settings.local.json`, which Claude cannot write itself; optionally run `setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O12 Relay v6.044 not verified on production | Medium | `relay-server/index.js:123-124` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests and documents overclaim | Medium | T rows in `TESTS/CHAPTER_TRIAGE.md`, `HydrationStaggeringAuditTest.kt`, `WatchdogRecoveryTest.kt` |
| O2 Hardcoded local HUD | Medium | `MonitorViewModel.kt:120` |
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

Line numbers come from Nov5.2; recheck them before editing.

### Nov6.0 Fix Locations
`ForensicBuffer.kt`: `loadHeader`, `addChecksums`, `isHeaderConsistent`, `rebuildHeader`, `loadSlot`, `storeSlot`, `commitHeader`, `nextSequenceId`, `writeTrace`. Use only absolute `get` and `put` in that class: `position()` and the other calls that JDK 9+ redeclares are missing on older Android.

## 🟡 Status: VERIFIED ON A15, NOT RUN BELOW API 35 (Nov6.0)

Version Nov6.0 delivers these changes:
1. **Forensic integrity**: Damaged entries are detected and skipped; a broken header is rebuilt; sequence numbers are not reused after power loss (ISSUE-NOV6.0-O20).
2. **Older Android**: Calls missing below API 35 are removed (ISSUE-NOV6.0-O25).
3. **Upgrade**: Version 2 buffer files upgrade in place and keep their entries.
4. **Chapter review**: QA is measured against what the tests prove: 43 of 111 active chapters (ISSUE-NOV5.4-O17, O18).

**Behavior change**: a Sensor restart can skip up to 100 sequence numbers. The Monitor sends one backfill request for the gap and receives an empty reply.

**Resumption Token**: NOV6.0-QUEUE-O2-NEXT

---

## 🎯 Next Resumption Focus: Work Queue in `DOCS/issues.md`, one item at a time, next is ISSUE-NOV5.0-O2

1. Fix O2, O14, O5, O6, and O15, which need no product decision.
2. For each D row in `TESTS/CHAPTER_TRIAGE.md`, decide: build it, or retire the chapter and rule (O11). Group the decisions by feature. Fix O13 afterwards, since it depends on whether `SentinelLogic` is wired in.
3. Confirm backfill on the production relay with a real Sensor and Monitor pair (O12), and decide on the nested repository changes (O19).
4. Fix O23, O21, O22, and O24.
5. Run the H chapters with a person, and the S chapters when the S21FE is free. Run chapter 121.6 below API 35 when the user allows it (O26).

### 🛡️ Implementation Status (Nov6.0)
- **Version Increment**: ✅ **VERIFIED** (`Nov6.0`, `versionCode` 34 monitor, 36 sensor; debug and release built; no `platinum` variant exists).
- **Automated Suite**: ✅ **PASSED** (130/130; instrumented tests pinned to A15).
- **A15 Live Pass**: ✅ **PASSED**: real file upgrade, three `kill -9` recoveries, clean audit. The Monitor was not run live; it has no code change beyond the version.
- **Below API 35**: ❌ **NOT EXECUTED** (ISSUE-NOV6.0-O26).
- **Physical Power Loss**: ❌ **NOT EXECUTED**; interrupted states are simulated in `ForensicEntryIntegrityTest`.
- **S21FE**: ❌ **NOT USED**; it still runs Nov5.1.
- **Physical Chapters & Stress Protocol**: ❌ **NOT EXECUTED** (H rows in `TESTS/CHAPTER_TRIAGE.md`).

---

## 📂 Key Implementation Files
- `core-shared/.../ForensicBuffer.kt`: entry checksum, sequence reservation, header rebuild, version upgrade.
- `core-shared/src/test/.../ForensicEntryIntegrityTest.kt`, `ForensicBufferApiLevelTest.kt`, `SequenceContinuityTest.kt`: Chapter 121 and 120.1.
- `app-sensor/.../SensorService.kt`: `nextSequenceId` seed, `writeTrace` before `sendTelemetry`, backfill through `readRange`.
- `TESTS/CHAPTER_TRIAGE.md`, `TESTS/RETIRED_CHAPTERS.md`, `TESTS/device_audit.cmd`: triage, retired wording, audit tool.

---

## ⚠️ Operational Notes
- `TESTS/device_audit.cmd <serial> <package>` is the real Step 2 audit; `ProductionReadinessAuditTest` only checks math.
- With two phones attached, set `ANDROID_SERIAL` before running `connectedDebugAndroidTest`: it runs on every device and uninstalls the apps it tested.
- Instrumented runs uninstall the Sensor. Reinstall with `adb -s R58X40GV2AR install -r -g app-sensor\build\outputs\apk\debug\app-sensor-debug.apk`, open `AdminActivity`, tap to pass `System Idle`, and tap `START SENSOR SERVICE`. Read its bounds first, because the layout shifts with URL length. The Sensor package is `com.jd.app.sensor`.
- S21FE safety: IDE `ui_state`, `take_screenshot`, `adb_shell_input`, and `deploy` may target it. For A15 work use `adb -s R58X40GV2AR` only. Run `adb devices` first, and check the foreground package before any capture or input.
- A15 baseline: font_scale 1.3, accelerometer_rotation 1, user_rotation 0, night mode on, Wi-Fi off, mobile_data 1, stay_on_while_plugged_in 15. The lock is swipe only. If `NotificationShade` has focus, run `cmd statusbar collapse`.
- Shell: use `shell=cmd` with `pty=false`, and run `.\gradlew.bat`, not `gradlew.bat`. Avoid `<` and `>` inside quoted `adb shell` commands: `cmd` treats them as redirects and can leave stray files in the repository.
- Sensor log tags are `REV_<tag>`, such as `REV_ForensicBuffer` and `REV_SensorService`.
- `relay-server` is a separate Git repository. Changes reach production only after a push; Render then auto-deploys.
- Never commit `relay-server/device_cache.json`, and do not send simulated positions to the production relay.
- After a reboot the Sensor restarts by itself, but this can take a few minutes.

---

## ✅ Verified Milestones (Nov6.0)
1. **Automated Suite**: 130/130 on the final code: `:core-shared` 46, `:core-network` 16, `:core-ui` 5, `:app-monitor` unit 13, `:app-monitor` on A15 15, `:app-sensor` on A15 35.
2. **Hardware**: Real upgrade, process kill recovery, and 0 Davey, StackLog, or fatal entries from the Sensor process on the A15.
3. **Release Integrity**: Baseline stored in `STATUS/BASELINE/Nov6.0/`; tag `Nov6.0` is applied by the Git block above.

**Handover complete. System state locked at Nov6.0.**
