git add .
git commit -m "Nov7.34 - AUTOMATIC SENSOR SERVICE STARTUP, UPTIME TIMER & PROTECTED STOP CONTROL (ISSUE-NOV7.2-O36)

- ISSUE-NOV7.2-O36: AdminActivity automatically starts SensorService on app launch when permissions are verified. SensorServiceStatus tracks service start time and SensorViewModel exposes live uptime formatted as HH:mm:ss. AdminActivity displays a live Service Status card with uptime counter and a protected STOP SENSOR SERVICE control presenting a confirmation dialog before sending ACTION_STOP_SERVICE to SensorService.
- TESTS: All 284 JVM unit tests passing across all subprojects, assembleDebug --warning-mode all build clean.
- DOCS: issues.md (item 28 done), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.382, 456 rules), TEST_PROCEDURE.md (Chapter 146, 136 of 146 active), Handover.md, baseline STATUS/BASELINE/Nov7.34/.
- VERSION: Nov7.34, versionCode 54 (monitor) and 56 (sensor)."
git tag Nov7.34
git push origin main --tags

# Project Handover - Version Nov7.34 (Automatic Sensor Service Startup & Protected Stop Control)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.34 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.34 commit and tag exist. Previous commits: `Nov7.32`, `Nov7.30`, `Nov7.28`, `Nov7.26`, `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image)**.
- **User decisions so far (2026-11-08)**: group 1, alarms and sirens: built in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6, DND and lock setup guide in Nov7.32. Sensor service automatic startup, live uptime counter, and protected stop control shipped in Nov7.34 (ISSUE-NOV7.2-O36). Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen. Nov7.19: O42 chosen. Nov7.21: O43 chosen. Nov7.24: O45 chosen. Nov7.26: O44 chosen. Nov7.28: O38 chosen. Nov7.30: O37 chosen. Nov7.32: O39 chosen. Nov7.34: O36 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.34.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell. Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions. When reading logcat, silence the `adbd` tag. Send `monkey` launch output to `>/dev/null 2>&1`. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). Copy SQLite database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `VERIFICATION_MANIFEST.md` |
| O41 Start-up Davey | Low | First frame of `MonitorActivity` / `AdminActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard and Resolved Gaps |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |

---

## 🟢 Status: PASSED (Nov7.34)

Version Nov7.34 delivers ISSUE-NOV7.2-O36:
1. **Automatic Sensor Service Startup, Live Uptime Timer & Protected Stop Control** (`AdminActivity`, `SensorService`, `SensorServiceStatus`, `SensorViewModel`):
   - `AdminActivity` automatically launches `SensorService` on app launch when permissions are verified.
   - `SensorServiceStatus` tracks service start timestamp (`startTimeMillis`) and `SensorViewModel` exposes live uptime formatted as `HH:mm:ss` (`serviceUptimeText`).
   - `AdminActivity` displays a live Service Status card with uptime counter and a protected `STOP SENSOR SERVICE` action button that presents a confirmation dialog (`"Are you sure you want to stop background sensing?"`) before sending `ACTION_STOP_SERVICE` to `SensorService`.
   - `SensorService.onStartCommand` cleanly intercepts `ACTION_STOP_SERVICE` and calls `stopSelf()`.
2. **Tests & Build**: All 284 JVM unit tests passing across all subprojects, `assembleDebug --warning-mode all` build clean.
3. **Documents**: SOT rule 1.382 (R-ID 153), 456 rules; `RESOLUTION_ARCHIVE.md`; Chapter 146 in `TESTS/TEST_PROCEDURE.md` (136 of 146 active); `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov7.34/`.

**Audit line**: Nov7.34: [SOT Count: 956 (Rules: 456), Open: H:1, M:2, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 284), QA: 69]

**Resumption Token**: NOV7.34-AUTOSTART-O26-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.0-O26 (Work Queue item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).

---

## 📂 Key Implementation Files
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorServiceStatus.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorService.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorViewModel.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/AdminActivity.kt`.
- `app-sensor/src/test/kotlin/com/jd/app/sensor/SensorServiceStatusTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.34.**
