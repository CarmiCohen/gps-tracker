git add .
git commit -m "Nov8.11 - TRAIL PERSISTENCE INTEGRITY & PENDING IDENTITY COLLECTION (ISSUE-NOV4.3-O48, ISSUE-NOV4.3-O49)

- ISSUE-NOV4.3-O48: Updated TrailEntity primary key composite [\"trackerId\", \"timestamp\", \"sequenceId\"] in MonitorDatabase.kt (version 4) to prevent equal-timestamp telemetry replacement overwrite, ensuring distinct sequence IDs with identical timestamps are fully retained in Room database.
- ISSUE-NOV4.3-O49: Refactored MonitorViewModel local trail collection flow to filter valid selfId first (selfId.filter { it != \"LOADING...\" && it.isNotBlank() }) and flatMapLatest into localFix, ensuring zero initial local fixes are dropped during cold start identity loading.
- TESTS: All 322 automated unit tests passing across all subprojects (app-monitor, app-sensor, core-network, core-shared, core-ui), including new TrailPersistenceIntegrityTest (Chapter 154) and VersionSourceTest updated for Nov8.11, assembleDebug --warning-mode all build clean.
- DOCS: issues.md (item 41 done, QA upgraded to 82), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.392, 466 rules), TEST_PROCEDURE.md (Chapter 154 added), CHAPTER_TRIAGE.md (82 of 144 active chapters verified), Handover.md, baseline STATUS/BASELINE/Nov8.11/.
- VERSION: Nov8.11, versionCode 62."
git tag Nov8.11
git push origin main --tags

# Project Handover - Version Nov8.11 (Trail Persistence Integrity & Pending Identity Collection)

## ▶️ Resumption State (Read First)
- **Version**: Nov8.11 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov8.11 commit and tag exist. Previous commits: `Nov8.10`, `Nov8.8`, `Nov8.7`, `Nov8.5`, `Nov8.2`, `Nov8.1`, `Nov8.0`, `Nov7.38`, `Nov7.36`, `Nov7.34`, `Nov7.32`, `Nov7.30`, `Nov7.28`, `Nov7.26`, `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image)**.
- **User decisions so far (2026-11-08)**: group 1, alarms and sirens: built in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6, DND and lock setup guide in Nov7.32. Sensor service automatic startup, live uptime counter, and protected stop control shipped in Nov7.34. Independent clean setup maintenance actions shipped in Nov7.36 (ISSUE-NOV7.1-O35). Local trail feature shipped in Nov7.38 (ISSUE-NOV6.2-O29). Unpaired tracker trail retention verified and tested in Nov8.0 (ISSUE-NOV6.2-O30). Comprehensive test coverage for chapters 67, 68, 83, 93, 145, 147, 148, and 149 shipped in Nov8.8 (ISSUE-NOV6.0-O23). Single source version management architecture shipped in Nov8.10 (SIMP-NOV7.0-S1). Trail persistence integrity and pending identity local trail collection shipped in Nov8.11 (ISSUE-NOV4.3-O48, ISSUE-NOV4.3-O49). Tracking documents synchronization shipped in Nov8.2 (ISSUE-NOV7.15-O40). Document integrity & session lifecycle rotation protection shipped in Nov8.5 (ISSUE-NOV8.2-O46, ISSUE-NOV8.5-O50). Async ForensicBuffer initialization & main-thread startup frame optimization shipped in Nov8.7 (ISSUE-NOV7.15-O41). Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov8.11.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell. Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions. When reading logcat, silence the `adbd` tag. Send `monkey` launch output to `>/dev/null 2>&1`. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). Copy SQLite database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O47 Partial or missing STATUS/BASELINE copies | Low | `STATUS/BASELINE/` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |

---

## 🟢 Status: PASSED (Nov8.11)

Version Nov8.11 delivers ISSUE-NOV4.3-O48 and ISSUE-NOV4.3-O49:
1. **Trail Persistence Integrity & Pending Identity Collection**:
   - Updated `TrailEntity` primary key composite `["trackerId", "timestamp", "sequenceId"]` in `MonitorDatabase.kt` (version 4) to prevent equal-timestamp telemetry replacement overwrite, ensuring distinct sequence IDs with identical timestamps are fully retained in Room database.
   - Refactored `MonitorViewModel` local trail collection flow to filter valid `selfId` first (`selfId.filter { it != "LOADING..." && it.isNotBlank() }`) and `flatMapLatest` into `localFix`, ensuring zero initial local fixes are dropped during cold start identity loading.
   - Verified by automated unit test suite `TrailPersistenceIntegrityTest` (Chapter 154) in `:app-monitor`.
2. **Tests & Build**: All 322 JVM unit tests passing across all subprojects (`app-monitor:test`, `app-sensor:test`, `core-network:test`, `core-shared:test`, `core-ui:test`), `assembleDebug --warning-mode all` build clean.
3. **Documents**: SOT rule 1.392 (R-ID 163), 466 rules; `RESOLUTION_ARCHIVE.md`; `TESTS/CHAPTER_TRIAGE.md` (82 of 144 active chapters verified); `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov8.11/`.

**Audit line**: Nov8.11: [SOT Count: 966 (Rules: 466), Open: H:1, M:1, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 322), QA: 82]

**Resumption Token**: NOV8.11-TRAILPERSISTENCE-O48O49-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.0-O26 (Work Queue item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).

---

## 📂 Key Implementation Files
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorDatabase.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorViewModel.kt`.
- `app-monitor/src/test/kotlin/com/jd/app/monitor/TrailPersistenceIntegrityTest.kt`.
- `core-shared/src/test/kotlin/com/jd/core/shared/VersionSourceTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/CHAPTER_TRIAGE.md`, `DOCS/issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `Handover.md`.

**Handover complete. System state locked at Nov8.11.**
