git add .
git commit -m "Nov8.10 - SINGLE SOURCE VERSION MANAGEMENT ARCHITECTURE & GRADLE BUILD SYNCHRONIZATION (SIMP-NOV7.0-S1)

- SIMP-NOV7.0-S1: Centralized application version properties (appVersionName=Nov8.10, appVersionCode=60) in gradle.properties as the single source of truth across all application subprojects. Refactored app-monitor/build.gradle.kts and app-sensor/build.gradle.kts to dynamically load version properties via providers.gradleProperty(), eliminating version code drift and redundant manual build script declarations across releases.
- TESTS: All 320 automated unit tests passing across all subprojects (app-monitor, app-sensor, core-network, core-shared, core-ui), including new VersionSourceTest (Chapter 153) and TrackingDocumentIntegrityTest (Chapter 150), assembleDebug --warning-mode all build clean.
- DOCS: issues.md (item 40 SIMP-NOV7.0-S1 done, QA upgraded to 81), RESOLUTION_ARCHIVE.md (full historical records restored), SOT_MASTER_REQUIREMENTS.md (rule 1.391, 465 rules), TEST_PROCEDURE.md (Chapter 153 added), CHAPTER_TRIAGE.md (81 of 143 active chapters verified), Handover.md, baseline STATUS/BASELINE/Nov8.10/.
- VERSION: Nov8.10, versionCode 60 (unified single source across all subprojects)."
git tag Nov8.10
git push origin main --tags

# Project Handover - Version Nov8.10 (Single Source Version Management Architecture & Gradle Build Synchronization)

## ▶️ Resumption State (Read First)
- **Version**: Nov8.10 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov8.10 commit and tag exist. Previous commits: `Nov8.8`, `Nov8.7`, `Nov8.5`, `Nov8.2`, `Nov8.1`, `Nov8.0`, `Nov7.38`, `Nov7.36`, `Nov7.34`, `Nov7.32`, `Nov7.30`, `Nov7.28`, `Nov7.26`, `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image)**.
- **User decisions so far (2026-11-08)**: group 1, alarms and sirens: built in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6, DND and lock setup guide in Nov7.32. Sensor service automatic startup, live uptime counter, and protected stop control shipped in Nov7.34. Independent clean setup maintenance actions shipped in Nov7.36 (ISSUE-NOV7.1-O35). Local trail feature shipped in Nov7.38 (ISSUE-NOV6.2-O29). Unpaired tracker trail retention verified and tested in Nov8.0 (ISSUE-NOV6.2-O30). Comprehensive test coverage for chapters 67, 68, 83, 93, 145, 147, 148, and 149 shipped in Nov8.8 (ISSUE-NOV6.0-O23). Single source version management architecture shipped in Nov8.10 (SIMP-NOV7.0-S1). Tracking documents synchronization shipped in Nov8.2 (ISSUE-NOV7.15-O40). Document integrity & session lifecycle rotation protection shipped in Nov8.5 (ISSUE-NOV8.2-O46, ISSUE-NOV8.5-O50). Async ForensicBuffer initialization & main-thread startup frame optimization shipped in Nov8.7 (ISSUE-NOV7.15-O41). Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov8.10.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell. Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions. When reading logcat, silence the `adbd` tag. Send `monkey` launch output to `>/dev/null 2>&1`. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). Copy SQLite database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O47 Partial or missing STATUS/BASELINE copies | Low | `STATUS/BASELINE/` |
| O48 Equal timestamp room replacement | Low | `app-monitor/.../TrailDao.kt` |
| O49 Pending identity local trail row | Low | `app-monitor/.../MonitorViewModel.kt` |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |

---

## 🟢 Status: PASSED (Nov8.10)

Version Nov8.10 delivers SIMP-NOV7.0-S1:
1. **Single Source Version Management Architecture & Gradle Build Synchronization**:
   - Centralized application `versionName` ("Nov8.10") and `versionCode` (60) in `gradle.properties` as the single source of truth across all subprojects.
   - Refactored `app-monitor/build.gradle.kts` and `app-sensor/build.gradle.kts` to dynamically pull version properties via `providers.gradleProperty()`, eliminating version code drift and redundant manual build script declarations.
   - Verified by automated unit test suite `VersionSourceTest` (Chapter 153) in `:core-shared`.
2. **Tests & Build**: All 320 JVM unit tests passing across all subprojects (`app-monitor:test`, `app-sensor:test`, `core-network:test`, `core-shared:test`, `core-ui:test`), `assembleDebug --warning-mode all` build clean.
3. **Documents**: SOT rule 1.391 (R-ID 162), 465 rules; `RESOLUTION_ARCHIVE.md` (full historical records restored); `TESTS/CHAPTER_TRIAGE.md` (81 of 143 active chapters verified); `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov8.10/`.

**Audit line**: Nov8.10: [SOT Count: 965 (Rules: 465), Open: H:1, M:1, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 320), QA: 81]

**Resumption Token**: NOV8.10-SINGLESOURCEVERSION-SIMP7.0S1-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.0-O26 (Work Queue item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).

---

## 📂 Key Implementation Files
- `gradle.properties`.
- `app-monitor/build.gradle.kts`.
- `app-sensor/build.gradle.kts`.
- `core-shared/build.gradle.kts`.
- `core-shared/src/test/kotlin/com/jd/core/shared/VersionSourceTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/CHAPTER_TRIAGE.md`, `DOCS/issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `Handover.md`.

**Handover complete. System state locked at Nov8.10.**
