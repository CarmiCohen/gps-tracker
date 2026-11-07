git add .
git commit -m "Nov7.19 - NO FALSE ALARMS AT START (ISSUE-NOV7.17-O42)

- ISSUE-NOV7.17-O42: every Monitor cold start raised RELAY_DISCONNECTED with the siren at full volume, and a start of both apps also raised GPS_SIGNAL_LOSS and GPS_JUMP. Three root causes, confirmed in the A15 log of 2026-11-07 19:46. (1) Relay: SocketioSignalingClient.isConnected starts false and alarmBooks raised the alarm on it 18 ms after 'Connecting to relay'. MonitorAlarms.kt now raises RELAY_DISCONNECTED only after the relay was connected once in this watch (WatchState.hasRelayConnected), or RELAY_FIRST_CONNECT_GRACE_MS (30 s) after watching starts without a connection; a loss after a connection still alarms at once. (2) GPS loss: the Sensor sends (0,0) until its first fix; GPS_SIGNAL_LOSS now needs GPS_SIGNAL_LOSS_AFTER_MS (30 s) without a position (WatchState.noGpsSignalSinceMs). (3) GPS jump: SensorService ran SentinelLogic.validateMovement on every loop tick (100 ms while charging) against the previous tick, dividing the drift between two fixes by one tick (the 34.5 m network-to-GPS correction became OUTLIER 346 m/s). New app-sensor MovementCheck.kt checks once per new fix, timed by Location.elapsedRealtimeNanos (fixForMovementCheck). A still A15 then showed JUMP 14.3 m/s, 23 m in 1.6 s at 68 m and 59 m accuracy, so SentinelLogic.validateMovement treats a move smaller than the larger accuracy as VALID ('Within GPS error'), replacing the narrower 'Suppressed Accuracy Snap' rule, and JUMP reasons now carry speed, distance, interval and accuracies.
- Also fixed in SensorService: the ViolationEvent was built inside serviceScope.launch after the pooled update was released and zeroed, so violations carried an empty location; it is now built before the launch. lastLocation is @Volatile.
- ISSUE-NOV7.19-O43 (new, Medium, open): the Sensor never reports a real GPS loss (lastLocation never ages); blocked on a critical flag, since every status other than VALID is sent as critical (SIMP-NOV7.19-S1).
- ISSUE-NOV7.19-O44 (new, Low, open): PermissionGuard in AdminActivity does not re-check permissions on resume (split out of O42).
- ISSUE-NOV7.15-O41 (updated): Nov7.19 audit Sensor 0 Davey, Monitor 1 first-frame Davey (834 ms).
- TESTS: 176/176 JVM unit tests (128 Android-module, 48 core-shared; new: MonitorAlarmsTest +6, MovementCheckTest 6, SentinelLogicTest +2); ProductionReadinessAuditTest 3/3 on the A15; live A15 through the production relay: Monitor cold start and a start of both apps with no alarm, siren or violation (48 s and 85 s), 0 violations in 2 minutes on a still phone.
- DOCS: issues.md (queue items 34 to 36, Open Gaps O43 O44 O41, Resolved Gaps Nov7.19, SIMP-NOV7.19-S1, dashboard), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.375, rules 1.366 and 1.368 amended, 450 rules), TEST_PROCEDURE.md (Chapter 139, 129 of 139 active), CHAPTER_TRIAGE.md (QA 65 of 129), VERIFICATION_MANIFEST.md header, Handover.md, baseline STATUS/BASELINE/Nov7.19/.
- VERSION: Nov7.19, versionCode 50 (monitor) and 52 (sensor). There was no Nov7.18 release."
git tag Nov7.19
git push origin main --tags

# Project Handover - Version Nov7.19 (No False Alarms At Start)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.19 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.19 commit and tag exist. Previous commits: `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, Nov7.14, Nov7.16, or Nov7.18 release (the user chose Nov7.19 directly after Nov7.17).
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV7.19-O43 (item 35, Medium)**: let the Sensor report a real GPS loss. Do SIMP-NOV7.19-S1 first (an explicit critical flag in `LocationUpdate`), because `SocketioSignalingClient.sendTelemetry` and `SignalingQueue` treat every status other than `VALID` as critical. ISSUE-NOV6.0-O26 (item 18) is still blocked on a device below API 35. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen (O26 blocked). Nov7.19: O42 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.19.
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached, running the Nov7.19 debug builds of both apps. The last instrumented run reinstalled the Sensor: its permissions were granted again with `pm grant`, but its service must be started with `START SENSOR SERVICE` (tap `System Idle` first). S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell (PowerShell mangles its arguments). Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O43 Sensor never reports a real GPS loss | Medium | `SensorService.lastLocation` (never aged); `status != "VALID"` checks in `SocketioSignalingClient.sendTelemetry` and `SignalingQueue` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `TESTS/VERIFICATION_MANIFEST.md` |
| O44 Permissions not re-checked on resume | Low | `PermissionGuard` in `AdminActivity.kt` |
| O41 Start-up Davey (Monitor first frame in Nov7.17 and Nov7.19) | Low | First frame of `MonitorActivity` / `AdminActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard (Nov7.7 to Nov7.12) and Resolved Gaps (Nov7.6 to Nov7.9) |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |

---

## 🟡 Status: PASSED WITH RECORDED AUDIT FINDING (Nov7.19)

Version Nov7.19 delivers ISSUE-NOV7.17-O42 (No False Alarms At Start):
1. **Relay alarm** (`app-monitor/.../MonitorAlarms.kt`): `RELAY_FIRST_CONNECT_GRACE_MS` = 30 s. `alarmConditions` takes `hasRelayConnected`; `WatchState.hasRelayConnected` latches on the first `true` from `isRelayConnected`. Before it, a disconnected relay counts as lost only 30 s after `watchingSinceMs` (longer than the 20 s socket timeout and the relay cold start of ISSUE-NOV5.1-O9).
2. **GPS loss alarm** (same file): `GPS_SIGNAL_LOSS_AFTER_MS` = 30 s. `WatchState.noGpsSignalSinceMs` starts at the `receivedAtMs` of the first reading without a position and is cleared by a reading with one; `alarmConditions` takes it.
3. **GPS jump** (`app-sensor/.../MovementCheck.kt`, new; `SensorService.startSensingLoop`): `MovementCheck.check` returns null for a fix already checked, `VALID` for the first fix, else `SentinelLogic.validateMovement(previousFix, fix)`; `fixForMovementCheck` stamps the fix with `elapsedRealtimeNanos / 1_000_000`. `lastValidationLocation` is removed; only the tick carrying a new fix can get a non-`VALID` status.
4. **GPS error rule** (`core-shared/.../SentinelLogic.kt`): `dist < max(last.accuracy, current.accuracy)` returns `VALID` (`Within GPS error`) before the outlier check; the `Suppressed Accuracy Snap` rule is removed (it was a narrower case of this). `JUMP` reasons carry `speed m/s, distance m in interval s, accuracy a to b m`.
5. **Violation payload** (`SensorService`): the `ViolationEvent` is built before `serviceScope.launch`; it used to snapshot the pooled update after release, sending a zeroed location. `lastLocation` is `@Volatile`.
6. **Tests**: `MonitorAlarmsTest` +6 (with a `settle()` step, because merged sources are collected separately), `MovementCheckTest` (new, 6, using the real A15 fixes), `SentinelLogicTest` +2. `gradlew test assembleDebug assembleRelease --warning-mode all`: BUILD SUCCESSFUL, 176/176 unit tests (128 Android-module, 48 `core-shared`), only the existing Gradle deprecation warnings. There is no `platinum` variant. `ProductionReadinessAuditTest` 3/3 on SM-A155F (`ANDROID_SERIAL=R58X40GV2AR`). `MonitorAlarmDeviceTest` was not run: it plays the siren and covers code this version does not change.
7. **Live (A15, production relay)**: Monitor cold start: `Connecting` 20:15:28.916, `Connected` 20:15:30.281, no `Pending alarms` in 48 s. Start of both apps (Sensor service 20:16:25 without a fix, Monitor 20:16:37): no alarm, siren, or `Violation detected` in 85 s. Still phone: 0 violations in 2 minutes (Nov7.17: `OUTLIER 346 m/s` and 3 `JUMP` in 90 s).
8. **Device audit** (`TESTS/device_audit.cmd`): Sensor 0 Davey, StackLog, fatal. Monitor 0 StackLog, 0 fatal, 1 Davey of 834 ms on its first frame (ISSUE-NOV7.15-O41, as in Nov7.17).
9. **Documents**: SOT rule 1.375 (R-ID 146), rules 1.366 and 1.368 amended, 450 rules; `RESOLUTION_ARCHIVE.md`; Chapter 139 in `TESTS/TEST_PROCEDURE.md` (129 of 139 active); `TESTS/CHAPTER_TRIAGE.md` (QA 65 of 129); `TESTS/VERIFICATION_MANIFEST.md` header; `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov7.19/`.

**Audit line**: Nov7.19: [SOT Count: 950 (Rules: 450), Open: H:1, M:4, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 179), QA: 65]

**Resumption Token**: NOV7.19-NOFALSEALARMS-O43-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV7.19-O43 (Work Queue item 35): add an explicit critical flag to `LocationUpdate` (SIMP-NOV7.19-S1), then let the Sensor report `NO_FIX` when its newest fix is too old, with tests.

---

## 📂 Key Implementation Files
- `app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorAlarms.kt`, `app-monitor/src/test/kotlin/com/jd/app/monitor/MonitorAlarmsTest.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/MovementCheck.kt` (new), `SensorService.kt`, `app-sensor/src/test/kotlin/com/jd/app/sensor/MovementCheckTest.kt` (new).
- `core-shared/src/main/kotlin/com/jd/core/shared/SentinelLogic.kt`, `core-shared/src/test/kotlin/com/jd/core/shared/SentinelLogicTest.kt`.
- `app-monitor/build.gradle.kts`, `app-sensor/build.gradle.kts` (versions).
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.19.**
