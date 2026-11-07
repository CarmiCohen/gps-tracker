git add .
git commit -m "Nov7.24 - SENSOR BATTERY SEEDING & UNKNOWN VALUE GUARDING (ISSUE-NOV7.21-O45, ISSUE-NOV7.2-O37)

- ISSUE-NOV7.21-O45: Sensor service start on a charging phone raised a false Power disconnected alarm because SensorService started with lastIsCharging = false before the battery broadcast arrived. SensorService now seeds its initial charging state, battery level, and temperature from the sticky ACTION_BATTERY_CHANGED broadcast intent returned by registerReceiver before the sensing loop starts.
- ISSUE-NOV7.2-O37: Unknown battery level (-1) and temperature (0f) are handled gracefully, and MonitorAlarms guards low battery (batteryLevel >= 0) and high temperature (tempCelsius > 0f) checks against unknown values.
- TESTS: 189/189 JVM unit tests passing (141 Android-module, 48 core-shared); ProductionReadinessAuditTest 3/3 on the A15; device audit 0 Davey, StackLog, fatal in both apps.
- DOCS: issues.md (queue item 37 done), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.377, 452 rules), TEST_PROCEDURE.md (Chapter 141, 131 of 141 active), CHAPTER_TRIAGE.md (QA 67 of 131), VERIFICATION_MANIFEST.md header, Handover.md, baseline STATUS/BASELINE/Nov7.24/.
- VERSION: Nov7.24, versionCode 52 (monitor) and 54 (sensor)."
git tag Nov7.24
git push origin main --tags

# Project Handover - Version Nov7.24 (Sensor Battery Seeding & Unknown Value Guarding)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.24 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.24 commit and tag exist. Previous commits: `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, Nov7.14, Nov7.16, Nov7.18, Nov7.20, Nov7.22, or Nov7.23 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV7.19-O44 (item 36, Low)**: re-check the Sensor permissions when `AdminActivity` resumes (`PermissionGuard` in `AdminActivity`). ISSUE-NOV6.0-O26 (item 18) is still blocked on a device below API 35. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen (O26 blocked). Nov7.19: O42 chosen. Nov7.21: O43 chosen. Nov7.24: O45 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.24.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached, running the Nov7.24 debug builds of both apps. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell. Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions. When reading logcat, silence the `adbd` tag. Send `monkey` launch output to `>/dev/null 2>&1`. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). Copy SQLite database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `TESTS/VERIFICATION_MANIFEST.md` |
| O44 Permissions not re-checked on resume | Low | `PermissionGuard` in `AdminActivity.kt` |
| O41 Start-up Davey | Low | First frame of `MonitorActivity` / `AdminActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard and Resolved Gaps |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟡 Status: PASSED (Nov7.24)

Version Nov7.24 delivers ISSUE-NOV7.21-O45 and ISSUE-NOV7.2-O37:
1. **Sensor Battery Seeding** (`app-sensor/.../SensorService.kt`): `registerBatteryReceiver()` now captures and parses the sticky `ACTION_BATTERY_CHANGED` intent returned by `registerReceiver` before `startSensingLoop()` starts. Initial `lastIsCharging`, `lastBatteryLevel`, and `lastTempCelsius` are correctly seeded, preventing false `Power disconnected` sirens when starting the Sensor service while the Monitor watches a charging phone.
2. **Unknown Value Guarding** (`app-sensor/.../SensorService.kt`, `app-monitor/.../MonitorAlarms.kt`): Unknown battery level (`-1`) and temperature (`0f`) are handled gracefully, and Monitor alarms (`LOW_BATTERY`, `HIGH_TEMPERATURE`) guard against unknown/uninitialized values (`batteryLevel >= 0`, `tempCelsius > 0f`).
3. **Tests**: 189/189 JVM unit tests passing (141 Android-module, 48 `core-shared`), `ProductionReadinessAuditTest` 3/3 on SM-A155F, device audit 0 Davey, StackLog, and fatal in both apps.
4. **Documents**: SOT rule 1.377 (R-ID 148), 452 rules; `RESOLUTION_ARCHIVE.md`; Chapter 141 in `TESTS/TEST_PROCEDURE.md` (131 of 141 active); `TESTS/CHAPTER_TRIAGE.md` (QA 67 of 131); `TESTS/VERIFICATION_MANIFEST.md` header; `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov7.24/`.

**Audit line**: Nov7.24: [SOT Count: 952 (Rules: 452), Open: H:1, M:3, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 192), QA: 67]

**Resumption Token**: NOV7.24-BATTERYSEED-O44-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV7.19-O44 (Work Queue item 36, Low): re-check the Sensor permissions when `AdminActivity` resumes (`PermissionGuard` in `AdminActivity`), with tests.

---

## 📂 Key Implementation Files
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorService.kt` (`registerBatteryReceiver`, `parseBatteryIntent`).
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorAlarms.kt` (battery and temperature range guards).
- `app-monitor/build.gradle.kts`, `app-sensor/build.gradle.kts` (versions Nov7.24, versionCode 52/54).
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.24.**
