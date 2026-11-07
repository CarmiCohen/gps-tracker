git add .
git commit -m "Nov7.36 - INDEPENDENT CLEAN SETUP MAINTENANCE ACTIONS (ISSUE-NOV7.1-O35)

- ISSUE-NOV7.1-O35: CleanSetupOverlay buttons previously all triggered tracker unpairing (requestUnpair). Built distinct, independent maintenance actions for all three options: Clear Home Points & Geofence (clears geofence violation settings and acknowledges alarms on Monitor), Reset Operational Statistics (clears trail database via TrailDao.deleteAll(), clears latest telemetry, acknowledges alarms, and resets service uptime timer), and Trigger Full System Re-Initialization (unpairs tracker connection, resets AlertSettings, sirenType, and relayUrl to default defaults, clears trail database, and resets service status).
- TESTS: All 284 JVM unit tests passing across all subprojects, assembleDebug --warning-mode all build clean.
- DOCS: issues.md (item 27 done), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.383, 457 rules), TEST_PROCEDURE.md (Chapter 147, 137 of 147 active), Handover.md, baseline STATUS/BASELINE/Nov7.36/.
- VERSION: Nov7.36, versionCode 54 (monitor) and 56 (sensor)."
git tag Nov7.36
git push origin main --tags

# Project Handover - Version Nov7.36 (Independent Clean Setup Maintenance Actions)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.36 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.36 commit and tag exist. Previous commits: `Nov7.34`, `Nov7.32`, `Nov7.30`, `Nov7.28`, `Nov7.26`, `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image)**.
- **User decisions so far (2026-11-08)**: group 1, alarms and sirens: built in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6, DND and lock setup guide in Nov7.32. Sensor service automatic startup, live uptime counter, and protected stop control shipped in Nov7.34. Independent clean setup maintenance actions shipped in Nov7.36 (ISSUE-NOV7.1-O35). Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen. Nov7.19: O42 chosen. Nov7.21: O43 chosen. Nov7.24: O45 chosen. Nov7.26: O44 chosen. Nov7.28: O38 chosen. Nov7.30: O37 chosen. Nov7.32: O39 chosen. Nov7.34: O36 chosen. Nov7.36: O35 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.36.
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

## 🟢 Status: PASSED (Nov7.36)

Version Nov7.36 delivers ISSUE-NOV7.1-O35:
1. **Independent Clean Setup Maintenance Actions** (`CleanSetupOverlay`, `MonitorActivity`, `MonitorViewModel`, `AdminActivity`, `SensorViewModel`, `TrailDao`, `MonitorRepository`):
   - `Clear Home Points & Geofence` (`onClearHomePoints`): Disables spatial violation alert settings (`distance`, `jammerDetection`, `tiltAlert`, `liftAlert`, `tamperAlert`) and acknowledges active alarms in `MonitorAlarmEngine` without unpairing tracker.
   - `Reset Operational Statistics` (`onResetStats`): Clears stored trail history from Room DB via `MonitorRepository.clearTrail()` (`TrailDao.deleteAll()`), clears latest telemetry in `MonitorViewModel`, acknowledges alarms, and resets service uptime counter in `SensorServiceStatus` / `SensorViewModel`.
   - `Trigger Full System Re-Initialization` (`onFullInit`): Unpairs target tracker pairing ID, resets `AlertSettings` to default defaults, resets `sirenType` to `"Siren"`, resets `relayUrl` to `RelayConfig.DEFAULT_URL`, clears stored trail history, and resets service status on both applications.
2. **Tests & Build**: All 284 JVM unit tests passing across all subprojects, `assembleDebug --warning-mode all` build clean.
3. **Documents**: SOT rule 1.383 (R-ID 154), 457 rules; `RESOLUTION_ARCHIVE.md`; Chapter 147 in `TESTS/TEST_PROCEDURE.md` (137 of 147 active); `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov7.36/`.

**Audit line**: Nov7.36: [SOT Count: 957 (Rules: 457), Open: H:1, M:2, L:12, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 284), QA: 70]

**Resumption Token**: NOV7.36-CLEANACTIONS-O35-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.0-O26 (Work Queue item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).

---

## 📂 Key Implementation Files
- `core-ui/src/main/kotlin/com/jd/core/ui/SetupOverlays.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorDatabase.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorRepository.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorViewModel.kt`.
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorActivity.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorServiceStatus.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorViewModel.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/AdminActivity.kt`.
- `app-monitor/src/test/kotlin/com/jd/app/monitor/CleanSetupActionsTest.kt`.
- `TESTS/TEST_PROCEDURE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.36.**
