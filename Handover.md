git add .
git commit -m "Nov8.12 - GEOFENCING IMPLEMENTATION (ISSUE-NOV5.2-O11 GROUP 2)

- ISSUE-NOV5.2-O11 (Group 2): Wired spatial geofence transition evaluation into SensorService using SentinelLogic.evaluateGeofenceTransition against a home anchor point, emitting ViolationEvent(type = \"GEOFENCE_BREACH\", ...) upon boundary exit.
- MONITOR: Added AlarmType.GEOFENCE_BREACH to MonitorAlarms, controlled by AlertSettings.distance and raised by MonitorAlarmEngine to sound the siren and show alarm notifications.
- TESTS: All automated unit tests passing across all subprojects (app-monitor, app-sensor, core-network, core-shared, core-ui), including new GeofenceBreachAlarmTest (Chapter 155), assembleDebug --warning-mode all build clean.
- DOCS: issues.md (item 8 Group 2 done, QA upgraded to 83), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.393, 467 rules), TEST_PROCEDURE.md (Chapter 155 added), CHAPTER_TRIAGE.md (83 of 144 active chapters verified), Handover.md, baseline STATUS/BASELINE/Nov8.12/.
- VERSION: Nov8.12, versionCode 63."
git tag Nov8.12
git push origin main --tags

# Project Handover - Version Nov8.12 (Geofencing Implementation)

## ▶️ Resumption State (Read First)
- **Version**: Nov8.12 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov8.12 commit and tag exist. Previous commits: `Nov8.11`, `Nov8.10`, `Nov8.8`, `Nov8.7`, `Nov8.5`, `Nov8.2`, `Nov8.1`, `Nov8.0`, `Nov7.38`, `Nov7.36`, `Nov7.34`, `Nov7.32`, `Nov7.30`, `Nov7.28`, `Nov7.26`, `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image)**.
- **User decisions so far (2026-11-08)**: group 1, alarms and sirens: built in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6, DND and lock setup guide in Nov7.32. Group 2, Geofencing: built in Nov8.12 (`ISSUE-NOV5.2-O11`). Sensor service automatic startup, live uptime counter, and protected stop control shipped in Nov7.34. Independent clean setup maintenance actions shipped in Nov7.36 (ISSUE-NOV7.1-O35). Local trail feature shipped in Nov7.38 (ISSUE-NOV6.2-O29). Unpaired tracker trail retention verified and tested in Nov8.0 (ISSUE-NOV6.2-O30). Comprehensive test coverage for chapters 67, 68, 83, 93, 145, 147, 148, and 149 shipped in Nov8.8 (ISSUE-NOV6.0-O23). Single source version management architecture shipped in Nov8.10 (SIMP-NOV7.0-S1). Trail persistence integrity and pending identity local trail collection shipped in Nov8.11 (ISSUE-NOV4.3-O48, ISSUE-NOV4.3-O49). Geofencing implementation shipped in Nov8.12 (ISSUE-NOV5.2-O11 Group 2). Tracking documents synchronization shipped in Nov8.2 (ISSUE-NOV7.15-O40). Document integrity & session lifecycle rotation protection shipped in Nov8.5 (ISSUE-NOV8.2-O46, ISSUE-NOV8.5-O50). Async ForensicBuffer initialization & main-thread startup frame optimization shipped in Nov8.7 (ISSUE-NOV7.15-O41). Group 3 (`SentinelLogic`): built on the Sensor. Groups 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov8.12.
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

## 🟢 Status: PASSED (Nov8.12)

Version Nov8.12 delivers ISSUE-NOV5.2-O11 (Group 2: Geofencing implementation):
1. **Geofencing Boundary Breach Detection & Alarm Annunciation**:
   - Wired spatial geofence transition evaluation into `SensorService` using `SentinelLogic.evaluateGeofenceTransition` against a home anchor point, emitting `ViolationEvent(type = "GEOFENCE_BREACH", ...)` upon boundary exit.
   - Added `AlarmType.GEOFENCE_BREACH` to `MonitorAlarms`, controlled by `AlertSettings.distance` and raised by `MonitorAlarmEngine` to sound the siren and show alarm notifications.
   - Verified by automated unit test suite `GeofenceBreachAlarmTest` (Chapter 155) in `:app-monitor`.
2. **Tests & Build**: All automated unit tests passing across all subprojects (`app-monitor:test`, `app-sensor:test`, `core-network:test`, `core-shared:test`, `core-ui:test`), `assembleDebug --warning-mode all` build clean.
3. **Documents**: SOT rule 1.393 (R-ID 164), 467 rules; `RESOLUTION_ARCHIVE.md`; `TESTS/CHAPTER_TRIAGE.md` (83 of 144 active chapters verified); `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov8.12/`.

**Audit line**: Nov8.12: [SOT Count: 967 (Rules: 467), Open: H:1, M:1, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 323), QA: 83]

**Resumption Token**: NOV8.12-GEOFENCE-O11-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.0-O26 (Work Queue item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).

---

## 📂 Key Implementation Files
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorService.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorAlarms.kt`.
- `app-monitor/src/test/kotlin/com/jd/app/monitor/GeofenceBreachAlarmTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `TESTS/CHAPTER_TRIAGE.md`, `DOCS/issues.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `Handover.md`.

**Handover complete. System state locked at Nov8.12.**
