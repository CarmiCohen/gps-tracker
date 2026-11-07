git add .
git commit -m "Nov7.26 - SENSOR PERMISSIONGUARD RE-CHECK ON RESUME (ISSUE-NOV7.19-O44)

- ISSUE-NOV7.19-O44: PermissionGuard in AdminActivity (app-sensor) evaluated permission check only once when entering composition. Returning to AdminActivity after granting permissions in system Settings or external permission flows left the UI stuck on Permissions Required until Grant Permissions button was clicked again. PermissionGuard now observes Lifecycle.Event.ON_RESUME using LocalLifecycleOwner.current and DisposableEffect, re-evaluating required permissions on activity resume. Extracted getRequiredSensorPermissions(sdkInt) and checkSensorPermissions(context, permissions, checker) helper functions.
- TESTS: 194/194 JVM unit tests passing across all modules (5 new unit tests in PermissionGuardTest); assembleDebug --warning-mode all build clean.
- DOCS: issues.md (item 36 done), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.378, 453 rules), TEST_PROCEDURE.md (Chapter 142, 132 of 142 active), CHAPTER_TRIAGE.md (QA 68 of 132), VERIFICATION_MANIFEST.md header, Handover.md, baseline STATUS/BASELINE/Nov7.26/.
- VERSION: Nov7.26, versionCode 53 (monitor) and 55 (sensor)."
git tag Nov7.26
git push origin main --tags

# Project Handover - Version Nov7.26 (Sensor PermissionGuard Re-check On Resume)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.26 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.26 commit and tag exist. Previous commits: `Nov7.24`, `Nov7.21`, `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, Nov7.14, Nov7.16, Nov7.18, Nov7.20, Nov7.22, or Nov7.23 release.
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image)**.
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen (O26 blocked). Nov7.19: O42 chosen. Nov7.21: O43 chosen. Nov7.24: O45 chosen. Nov7.26: O44 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.26.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell. Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions. When reading logcat, silence the `adbd` tag. Send `monkey` launch output to `>/dev/null 2>&1`. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). Copy SQLite database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db`.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `TESTS/VERIFICATION_MANIFEST.md` |
| O41 Start-up Davey | Low | First frame of `MonitorActivity` / `AdminActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard and Resolved Gaps |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟢 Status: PASSED (Nov7.26)

Version Nov7.26 delivers ISSUE-NOV7.19-O44:
1. **PermissionGuard Re-check On Resume** (`app-sensor/.../AdminActivity.kt`): Refactored `PermissionGuard` to observe `Lifecycle.Event.ON_RESUME` using `LocalLifecycleOwner.current` and `DisposableEffect`. When returning to `AdminActivity` after granting permissions in system Settings or external permission flows, permissions are re-evaluated automatically and `allGranted` is updated immediately.
2. **Helper Functions & Test Coverage**: Extracted `getRequiredSensorPermissions` and `checkSensorPermissions`. Added 5 new unit tests in `PermissionGuardTest.kt` verifying permissions across API 26, 33, and 34, and testing permission checking logic.
3. **Tests & Build**: 194/194 JVM unit tests passing across all subprojects, `assembleDebug --warning-mode all` build clean.
4. **Documents**: SOT rule 1.378 (R-ID 149), 453 rules; `RESOLUTION_ARCHIVE.md`; Chapter 142 in `TESTS/TEST_PROCEDURE.md` (132 of 142 active); `TESTS/CHAPTER_TRIAGE.md` (QA 68 of 132); `TESTS/VERIFICATION_MANIFEST.md` header; `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov7.26/`.

**Audit line**: Nov7.26: [SOT Count: 953 (Rules: 453), Open: H:1, M:2, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 194), QA: 68]

**Resumption Token**: NOV7.26-PERMGUARD-O26-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV6.0-O26 (Work Queue item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).

---

## 📂 Key Implementation Files
- `app-sensor/src/main/kotlin/com/jd/app/sensor/AdminActivity.kt` (`PermissionGuard`, `getRequiredSensorPermissions`, `checkSensorPermissions`).
- `app-sensor/src/test/kotlin/com/jd/app/sensor/PermissionGuardTest.kt` (unit tests).
- `app-monitor/build.gradle.kts`, `app-sensor/build.gradle.kts` (versions Nov7.26, versionCode 53/55).
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.26.**
