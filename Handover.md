git add .
git commit -m "Nov8.5 - TRACKING DOCUMENT INTEGRITY & ACTIVITY ROTATION SESSION LIFECYCLE (ISSUE-NOV8.2-O46, ISSUE-NOV8.5-O50)

- ISSUE-NOV8.2-O46: Restored truncated tracking documentation (issues.md Resolved Gaps Nov4.32 to Nov7.34, Ideas, Dashboard; RESOLUTION_ARCHIVE.md Nov4.32 to Nov7.12; SOT chapters 5.52 to 5.59 and baseline entries Nov7.36, Nov7.6, Nov6.0) guarded by automated unit tests (TrackingDocumentIntegrityTest, Chapter 150); removed placebo tests and updated CHAPTER_TRIAGE.
- ISSUE-NOV8.5-O50: Fixed MonitorSession Activity rotation lifecycle defect where activity recreation re-started service and reset alarm states, introducing SessionCommand state machine (SessionCommandTest, Chapter 151).
- TESTS: All 212 JVM unit tests and 90 Android instrumented tests passing across all subprojects (302/302), assembleDebug --warning-mode all build clean, device audit 0 Davey events >500ms on A15, reciprocal simulation verified.
- DOCS: issues.md (item 38 done, item 40/O23 updated), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.388, 462 rules), TEST_PROCEDURE.md (Chapter 5.95 active audit, chapters 150, 151), Handover.md, baseline STATUS/BASELINE/Nov8.5/.
- VERSION: Nov8.5, versionCode 55 (monitor) and 57 (sensor)."
git tag Nov8.5
git push origin main --tags

# Project Handover - Version Nov8.5 (Tracking Document Integrity & Activity Rotation Session Lifecycle)

## ▶️ Resumption State (Read First)
- **Version**: Nov8.5 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov8.5 commit and tag exist. Previous commits: `Nov8.2`, `Nov8.1`, `Nov8.0`, `Nov7.38`, `Nov7.36`, `Nov7.34`, `Nov7.32`, `Nov7.30`, `Nov7.28`, `Nov7.26`, `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image)**.
- **User decisions so far (2026-11-08)**: group 1, alarms and sirens: built in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6, DND and lock setup guide in Nov7.32. Sensor service automatic startup, live uptime counter, and protected stop control shipped in Nov7.34. Independent clean setup maintenance actions shipped in Nov7.36 (ISSUE-NOV7.1-O35). Local trail feature shipped in Nov7.38 (ISSUE-NOV6.2-O29). Unpaired tracker trail retention verified and tested in Nov8.0 (ISSUE-NOV6.2-O30). Comprehensive test coverage for chapters 67, 68, 83, 93 shipped in Nov8.1 (ISSUE-NOV6.0-O23). Tracking documents synchronization shipped in Nov8.2 (ISSUE-NOV7.15-O40). Document integrity & session lifecycle rotation protection shipped in Nov8.5 (ISSUE-NOV8.2-O46, ISSUE-NOV8.5-O50). Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen. Nov7.19: O42 chosen. Nov7.21: O43 chosen. Nov7.24: O45 chosen. Nov7.26: O44 chosen. Nov7.28: O38 chosen. Nov7.30: O37 chosen. Nov7.32: O39 chosen. Nov7.34: O36 chosen. Nov7.36: O35 chosen. Nov7.38: O29 chosen. Nov8.0: O30 chosen. Nov8.1: O23 chosen. Nov8.2: O40 chosen. Nov8.5: O46 & O50 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov8.5.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell. Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions. When reading logcat, silence the `adbd` tag. Send `monkey` launch output to `>/dev/null 2>&1`. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). Copy SQLite database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O41 Start-up Davey | Low | First frame of `MonitorActivity` / `AdminActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O47 Partial or missing STATUS/BASELINE copies | Low | `STATUS/BASELINE/` |
| O48 Equal timestamp room replacement | Low | `app-monitor/.../TrailDao.kt` |
| O49 Pending identity local trail row | Low | `app-monitor/.../MonitorViewModel.kt` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |

---

## 🟢 Status: PASSED (Nov8.5)

Version Nov8.5 delivers ISSUE-NOV8.2-O46 and ISSUE-NOV8.5-O50:
1. **Tracking Document Integrity Guard & Truncation Restoration** (`DOCS/issues.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `TrackingDocumentIntegrityTest.kt`):
   - Restored truncated historical documentation across all tracking files and added automated integrity guard tests (Chapter 150). Removed placebo tests and re-aligned CHAPTER_TRIAGE.
2. **MonitorSession Activity Rotation Lifecycle Protection** (`MonitorSession.kt`, `MonitorViewModel.kt`, `MonitorActivity.kt`, `MonitorSessionService.kt`, `SessionCommandTest.kt`):
   - Introduced `SessionCommand` state machine ensuring activity recreation during screen rotation does not re-instantiate `MonitorSessionService` or reset active alarm/acknowledgement states (Chapter 151).
3. **Tests & Build**: All 302 automated tests passing (212 JVM unit tests + 90 Android instrumented tests on A15), `assembleDebug --warning-mode all` build clean, device audit 0 Davey events >500ms on A15, reciprocal simulation verified.
4. **Documents**: SOT rule 1.388 (R-ID 159), 462 rules; `RESOLUTION_ARCHIVE.md`; Chapter 5.95 audit; `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov8.5/`.

**Audit line**: Nov8.5: [SOT Count: 962 (Rules: 462), Open: H:1, M:2, L:14, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 302), QA: 74]

**Resumption Token**: NOV8.5-INTEGRITY-O46-SESSION-O50-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.0-O26 (Work Queue item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).

---

## 📂 Key Implementation Files
- `core-shared/src/test/kotlin/com/jd/core/shared/TrackingDocumentIntegrityTest.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorSession.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorViewModel.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorActivity.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorSessionService.kt`.
- `app-monitor/src/test/kotlin/com/jd/app/monitor/SessionCommandTest.kt`.
- `DOCS/issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `TESTS/TEST_PROCEDURE.md`, `Handover.md`.

**Handover complete. System state locked at Nov8.5.**
