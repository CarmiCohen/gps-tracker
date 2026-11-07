git add .
git commit -m "Nov7.17 - REAL SENSOR SERVICE STATUS IN SETUP DIALOG (ISSUE-NOV6.4-O34)

- ISSUE-NOV6.4-O34: The Sensor Setup dialog no longer shows the fixed text 'Sensor Service Status: Active / Ready'. New @Singleton SensorServiceStatus (app-sensor SensorServiceStatus.kt) holds isRunning; SensorService calls markRunning() at the end of initializeServiceComponents and markStopped() in onDestroy. SensorViewModel.isServiceRunning feeds SensorAdminConsole, and the dialog shows 'Sensor Service: Running/Stopped' (serviceStatusText) and 'Relay Connection: Connected/Disconnected' (relayStatusText, from hudState.isRelayConnected). Same dialog: Close was squeezed into a vertical strip because both buttons shared one Row in confirmButton; Close is now confirmButton and CHANGE RELAY URL dismissButton. Added SensorServiceStatusTest (5 tests); SensorHudValuesTest updated for the new ViewModel parameter.
- ISSUE-NOV7.17-O42 (new, Medium, open): the Monitor raises a false RELAY_DISCONNECTED alarm with the siren at full volume on every cold start, 18 ms after 'Connecting to relay' and before 'Connected' (1.3 s later); GPS_SIGNAL_LOSS and GPS_JUMP follow in the same start. Also recorded: PermissionGuard does not re-check permissions on resume. Not caused by O34.
- ISSUE-NOV7.15-O41 (updated): the Nov7.17 audit had the Sensor clean but one first-frame Monitor Davey over 500 ms in 2 of 2 runs (924 ms, 825 ms).
- TESTS: 162/162 JVM unit tests (116 Android-module, 46 core-shared); 8/8 instrumented on the A15 (ProductionReadinessAuditTest 3, SensorHudValuesTest 5); live A15 check of both dialog states; device audit Sensor clean, Monitor see O41 and O42.
- DOCS: issues.md (queue items 26 and 34, Open Gaps, Resolved Gaps, SIMP-NOV7.17-S1, dashboard), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.374, 449 rules), TEST_PROCEDURE.md (Chapter 138, 128 of 138 active), CHAPTER_TRIAGE.md (QA 64 of 128), VERIFICATION_MANIFEST.md header, Handover.md, baseline STATUS/BASELINE/Nov7.17/.
- VERSION: Nov7.17, versionCode 49 (monitor) and 51 (sensor). There was no Nov7.16 release."
git tag Nov7.17
git push origin main --tags

# Project Handover - Version Nov7.17 (Real Sensor Service Status In Setup Dialog)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.17 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.17 commit and tag exist. Previous commits: `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, Nov7.14, or Nov7.16 release (the user chose Nov7.17 directly after Nov7.15).
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV7.17-O42 (item 34, Medium)**: the Monitor's false `RELAY_DISCONNECTED` alarm and full-volume siren on every cold start. It needs nothing from the user. ISSUE-NOV6.0-O26 (item 18) is still blocked on a device below API 35. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen (O26 blocked).
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.17.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached, running the Nov7.17 debug builds of both apps, Sensor service started, location and notification permissions granted. S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test caution**: every Monitor cold start on the A15 currently sounds the siren at full volume for 30 s (O42) until acknowledged.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O42 False relay alarm at Monitor start | Medium | `MonitorAlarms.kt` / `MonitorAlarmEngine.kt` relay condition on the initial `signalingClient.isConnected`; also `PermissionGuard` in `AdminActivity.kt` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `TESTS/VERIFICATION_MANIFEST.md` |
| O41 Start-up Davey (Sensor in Nov7.15, Monitor in Nov7.17) | Low | First frame of `AdminActivity` / `MonitorActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard (Nov7.7 to Nov7.12) and Resolved Gaps (Nov7.6 to Nov7.9) |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟡 Status: PASSED WITH RECORDED AUDIT FINDINGS (Nov7.17)

Version Nov7.17 delivers ISSUE-NOV6.4-O34 (Real Sensor Service Status In Setup Dialog):
1. **Fix**: `app-sensor/.../SensorServiceStatus.kt` (new) adds the `@Singleton` `SensorServiceStatus` (`isRunning`, `markRunning`, `markStopped`) and the dialog text functions `serviceStatusText` and `relayStatusText`. `SensorService` injects it, calls `markRunning()` right after `isInitialized = true` in `initializeServiceComponents`, and `markStopped()` in `onDestroy` next to `fixStore.clear()`. `SensorViewModel` takes it as a constructor parameter and exposes `isServiceRunning`. `AdminActivity` collects it and passes it to `SensorAdminConsole(isServiceRunning = ...)`, whose dialog shows the service and relay lines. The fixed `Active / Ready` string is removed.
2. **Dialog buttons**: `Close` was squeezed into a vertical strip on the A15 because `CHANGE RELAY URL` and `Close` shared one `Row` inside `confirmButton`. `Close` is now `confirmButton`, `CHANGE RELAY URL` is `dismissButton`.
3. **Tests**: `app-sensor/src/test/.../SensorServiceStatusTest.kt` (5 tests). `SensorHudValuesTest` passes the new ViewModel parameter. `gradlew test assembleDebug assembleRelease --warning-mode all`: BUILD SUCCESSFUL, 162/162 unit tests (116 Android-module, 46 `core-shared`), only the existing Gradle deprecation warnings. There is no `platinum` variant. `connectedDebugAndroidTest` with `ANDROID_SERIAL=R58X40GV2AR` for `ProductionReadinessAuditTest` and `SensorHudValuesTest`: 8/8 on SM-A155F.
4. **Live (A15 only)**: Sensor service started by `BootReceiver`, giving `Running` / `Connected`. With location revoked before launch (0 service records), and again after `install -r`, the dialog showed `Stopped` / `Disconnected`. After `START SENSOR SERVICE` it showed `Running` / `Connected`. The buttons now show their full labels.
5. **Device audit** (`TESTS/device_audit.cmd`, 2 runs of about 2 minutes): the Sensor had 0 Davey, StackLog, or fatal entries in both. The Monitor had 0 StackLog and 0 fatal entries, but one first-frame Davey in both runs (924 ms, 825 ms; O41). During these runs the false relay alarm was found (O42). A third run was skipped because of the siren.
6. **Documents**: SOT rule 1.374 (R-ID 145), 449 rules; `RESOLUTION_ARCHIVE.md`; Chapter 138 in `TESTS/TEST_PROCEDURE.md` (128 of 138 active); `TESTS/CHAPTER_TRIAGE.md` (QA 64 of 128); `TESTS/VERIFICATION_MANIFEST.md` header; `DOCS/issues.md` (queue, O42, O41 update, Resolved Gaps, SIMP-NOV7.17-S1, dashboard); baseline `STATUS/BASELINE/Nov7.17/`.

**Audit line**: Nov7.17: [SOT Count: 949 (Rules: 449), Open: H:1, M:4, L:14, Ideas: H:0, M:3, L:12, Testing: PASSED (Sub-items: 170), QA: 64]

**Resumption Token**: NOV7.17-SERVICESTATUS-O42-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV7.17-O42 (Work Queue item 34): stop the Monitor's false `RELAY_DISCONNECTED` alarm at start, and check the `GPS_SIGNAL_LOSS` and `GPS_JUMP` alarms raised right after it.

---

## 📂 Key Implementation Files
- `app-sensor/src/main/kotlin/com/jd/app/sensor/SensorServiceStatus.kt` (new), `SensorService.kt`, `SensorViewModel.kt`, `AdminActivity.kt`.
- `app-sensor/src/test/kotlin/com/jd/app/sensor/SensorServiceStatusTest.kt` (new), `app-sensor/src/androidTest/kotlin/com/jd/app/sensor/SensorHudValuesTest.kt`.
- `app-monitor/build.gradle.kts`, `app-sensor/build.gradle.kts` (versions).
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.17.**
