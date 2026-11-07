git add .
git commit -m "Nov7.21 - THE SENSOR REPORTS A REAL GPS LOSS (ISSUE-NOV7.19-O43, SIMP-NOV7.19-S1)

- ISSUE-NOV7.19-O43: SensorService.lastLocation was never aged, so after a real signal loss the Sensor kept sending its last fix as fresh and the Monitor's Tracker GPS signal loss could only fire before the first fix. New app-sensor FixFreshness.kt (FIX_STALE_AFTER_MS 15 s, isFixFresh on elapsedRealtimeNanos): with no fix, or a fix at least 15 s old, each tick is sent as NO_FIX with no position, and the service logs each change ('No current GPS fix ... reporting NO_FIX' / 'GPS fix current again').
- SIMP-NOV7.19-S1 (implemented): LocationUpdate.isCritical (cleared by reset(), not sent on the wire) replaces the four status != VALID checks in SocketioSignalingClient.sendTelemetry and SignalingQueue (unbatched send, dedup, pushFront, drop-oldest), so NO_FIX is batched and droppable like any reading. The Sensor sets it only for JUMP / OUTLIER updates and passes it to ForensicBuffer.writeTrace, which commits them at once.
- Found live on the A15 and fixed: after a GPS loss the first fix was a 700 m cell fix 3352 m off (false JUMP 97.5 m/s), then the GPS fix after it gave a false OUTLIER 488 m/s on a still phone. MovementCheck no longer judges or keeps as its reference a fix without accuracy or coarser than MOVEMENT_CHECK_MAX_ACCURACY_M (100 m).
- ISSUE-NOV7.21-O45 (new, Medium, open): a Sensor service start while the Monitor watches sounds a false Power disconnected siren on a charging phone (SensorService starts with lastIsCharging false before the sticky battery broadcast arrives; queued ticks flushed on connect). Fix with ISSUE-NOV7.2-O37.
- ISSUE-NOV7.15-O41 (updated): Nov7.21 audit 0 Davey in both apps.
- TESTS: 189/189 JVM unit tests (141 Android-module, 48 core-shared; new: FixFreshnessTest 5, SignalingQueueTest +4, SignalingBatchTest +1, MovementCheckTest +3); ProductionReadinessAuditTest 3/3 on the A15; device audit 0 Davey, StackLog, fatal in both apps; live A15 through the production relay: NO_FIX 15 s after the last fix with location off, 126 contiguous NO_FIX rows without position stored by the Monitor, no violation after recovery, no alarm at a Monitor cold start.
- DOCS: issues.md (queue items 35 and 37, Open Gaps O45 and O41, Resolved Gaps Nov7.21, SIMP-NOV7.21-S1, dashboard), RESOLUTION_ARCHIVE.md, SOT_MASTER_REQUIREMENTS.md (rule 1.376, 451 rules), TEST_PROCEDURE.md (Chapter 140, 130 of 140 active), CHAPTER_TRIAGE.md (QA 66 of 130), VERIFICATION_MANIFEST.md header, Handover.md (also the logcat adbd test tip), baseline STATUS/BASELINE/Nov7.21/.
- VERSION: Nov7.21, versionCode 51 (monitor) and 53 (sensor). There was no Nov7.20 release."
git tag Nov7.21
git push origin main --tags

# Project Handover - Version Nov7.21 (The Sensor Reports A Real GPS Loss)

## ▶️ Resumption State (Read First)
- **Version**: Nov7.21 is closed. Run the Git block above if it has not been run: `git log -1` shows whether the Nov7.21 commit and tag exist. Previous commits: `Nov7.19`, `Nov7.17`, `Nov7.15`, `Nov7.12`, `Nov7.11`, `Nov7.10`, `Nov7.9`, `Nov7.8`, `Nov7.7`, `Nov7.6`, `Nov7.5`, `Nov7.3`, `Nov7.2`, `Nov7.1`, `Nov7.0`. There was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, Nov7.14, Nov7.16, Nov7.18, or Nov7.20 release (the user chose Nov7.21 directly after Nov7.19).
- **Next work**: the next Work Queue item in `DOCS/issues.md` is **ISSUE-NOV7.21-O45 (item 37, Medium)**: a Sensor start sounds a false `Power disconnected` siren on a charging phone. Seed the Sensor's battery readings from the Intent returned by `registerReceiver` before the sensing loop starts, and send unknown readings as unknown (with ISSUE-NOV7.2-O37). ISSUE-NOV6.0-O26 (item 18) is still blocked on a device below API 35. Display each issue before fixing it (Rule 1).
- **User decisions so far (2026-11-07)**: group 1, alarms and sirens: build, in slices; slices 1 to 3 shipped in Nov7.3, Nov7.5, Nov7.7, siren auto-stop in Nov7.6. Group 3 (`SentinelLogic`): built on the Sensor. Groups 2 and 4 to 8 still need user decisions. O19: `before splitting/gps-tracker` is a frozen archive; never edit it. Nov7.15: O33 chosen over O26. Nov7.17: O34 chosen (O26 blocked). Nov7.19: O42 chosen. Nov7.21: O43 chosen.
- **Relay**: v6.044 is on production. No relay code changed in Nov6.4 to Nov7.21 (the relay passes `status` through unchanged).
- **Phones**: A15 (`R58X40GV2AR`, API 36) attached, running the Nov7.21 debug builds of both apps. The last instrumented run uninstalled the Sensor: it was reinstalled, its permissions granted again with `pm grant`, and its service started with `START SENSOR SERVICE` (tap `System Idle` first). The Monitor was force-stopped at 20:45:29 to silence the false siren of ISSUE-NOV7.21-O45. Location is enabled (checked after every loss test). S21FE (`R5CRC14PG4F`) unplugged and in personal use; never run tests on it.
- **Test tips**: `TESTS/device_audit.cmd` must be run from the `cmd` shell (PowerShell mangles its arguments). Long `adb shell sleep` waits time out in the IDE tool; run them as background sessions, and when a test switches location off, switch it back on in the same `adb shell` call (a timed-out call left it off once in Nov7.21). When reading logcat, silence the `adbd` tag (`logcat -d adbd:S *:V | grep ...`, or append `| grep -v adbd`): `adbd` logs the text of every `adb shell` command, so a grep for a pattern also matches its own command line. This only filters what is read; `TESTS/device_audit.cmd` reads by app `--pid` and is not affected. Send `monkey` launch output to `>/dev/null 2>&1` to hide its argument echo. Run instrumented tests from PowerShell (`$env:ANDROID_SERIAL="R58X40GV2AR"; .\gradlew.bat :app-sensor:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=..."`). The phone has no `sqlite3`: copy the Monitor database with `adb exec-out run-as com.jd.app.monitor cat databases/monitor_db` (and `-wal`) from `cmd` into a new temporary folder, query it with the SDK's `platform-tools\sqlite3.exe` (table `trails`), and delete the copy afterwards.

### Open Issues With Locations
| Issue | Severity | Location |
| :--- | :--- | :--- |
| O11 Unwired features | High | D rows in `TESTS/CHAPTER_TRIAGE.md` |
| O45 False `Power disconnected` siren at a Sensor start | Medium | `SensorService` `lastIsCharging` / `lastBatteryLevel` / `lastTempCelsius` defaults, `registerBatteryReceiver`, `startSensingLoop` |
| O26 Sensor never run below API 35 | Medium | `minSdk` 26 in `app-sensor/build.gradle.kts`; chapter 121.6 |
| O23 Tests claim more than verified | Medium | Chapters 67, 68, 83, 93 (T rows); R494, R495 in `TESTS/VERIFICATION_MANIFEST.md` |
| O44 Permissions not re-checked on resume | Low | `PermissionGuard` in `AdminActivity.kt` |
| O41 Start-up Davey (Monitor first frame in Nov7.17 and Nov7.19; none in Nov7.21) | Low | First frame of `MonitorActivity` / `AdminActivity`; `BootReceiver.onReceive`, `SensorService.onCreate` |
| O40 Dashboard history gaps | Low | `DOCS/issues.md` dashboard (Nov7.7 to Nov7.12) and Resolved Gaps (Nov7.6 to Nov7.9) |
| O19 Nested repository changes | Low | `before splitting/gps-tracker` |
| O38 Sensor siren test can never sound | Low | `SensorViewModel.testSirenAudio`, `AdminActivity.kt` `AlarmSoundOverlay` call |
| O39 Alarm reach under DND and lock | Low | `createMonitorNotificationChannels` `setBypassDnd`, `MonitorAlarmEngine.showNotification` full-screen intent |
| O37 Unknown values sent as invented numbers | Low | `SensorService` `loc.speed` without `hasSpeed()`, `batteryReceiver` defaults |

---

## 🟡 Status: PASSED WITH RECORDED FINDING (Nov7.21)

Version Nov7.21 delivers ISSUE-NOV7.19-O43 (The Sensor Reports A Real GPS Loss) and SIMP-NOV7.19-S1:
1. **Critical flag** (`core-shared/.../Models.kt`): `LocationUpdate.isCritical`, default `false`, cleared by `reset()`, copied by `snapshot()`, not put on the wire. `SignalingQueue` uses `!isCritical` for deduplication, `pushFront`, and drop-oldest; `SocketioSignalingClient.sendTelemetry` sends unbatched only when `isCritical`. No production code reads `status` for delivery any more.
2. **Loss rule** (`app-sensor/.../FixFreshness.kt`, new; `SensorService.startSensingLoop`): `currentFix = lastLocation?.takeIf { isFixFresh(it.elapsedRealtimeNanos, SystemClock.elapsedRealtimeNanos()) }`; with none, the tick is sent as `NO_FIX_STATUS` with position 0. `FIX_STALE_AFTER_MS` = 15 s (three times the 5 s location request). `isReportingNoFix` logs each change once. The Monitor reads `NO_FIX` as no signal (`hasGpsSignal`) and raises `Tracker GPS signal loss` after 30 s of it, so about 45 s after the last fix.
3. **Violations**: a `JUMP` / `OUTLIER` tick sets `isCritical = true`; `forensicBuffer.writeTrace(update, isCritical = update.isCritical)` commits it at once.
4. **Coarse fixes** (`app-sensor/.../MovementCheck.kt`): `MOVEMENT_CHECK_MAX_ACCURACY_M` = 100 m; `check` returns null for a fix without accuracy or coarser, and does not keep it as the reference.
5. **Tests**: `FixFreshnessTest` (new, 5), `SignalingQueueTest` +4, `SignalingBatchTest` +1 (`testCriticalEventFlushesBatch` now sets `isCritical`), `MovementCheckTest` +3. `gradlew test assembleDebug assembleRelease --warning-mode all`: BUILD SUCCESSFUL, 189/189 unit tests (141 Android-module, 48 `core-shared`), only the existing Gradle deprecation warnings and the existing `AlarmStealthTest.kt:16` unused variable. There is no `platinum` variant. `ProductionReadinessAuditTest` 3/3 on SM-A155F.
6. **Live (A15, production relay)**: location off at 20:37:47: `No current GPS fix` 20:37:59 (last fix about 20:37:44), back on at 20:38:17: `GPS fix current again` 20:38:19, but a false `JUMP` (700 m cell fix) and `OUTLIER 488 m/s` followed, which led to item 4. After item 4: off about 20:40:56 to 20:41:33, `NO_FIX` 20:41:06, current again 20:41:35, no violation. With the Monitor running: off 20:43:03 to 20:43:25, `NO_FIX` 20:43:14.1 to 20:43:27.2, and the Monitor stored 126 `NO_FIX` rows (sequences 3314 to 3439, none missing) with no position, and raised no alarm. The 30 s Monitor alarm on `NO_FIX` was not sounded live; `MonitorAlarmsTest` covers it.
7. **Device audit** (`TESTS/device_audit.cmd`, Monitor cold start after the Sensor): Sensor and Monitor 0 Davey, 0 StackLog, 0 fatal; no alarm at the Monitor cold start.
8. **Finding (ISSUE-NOV7.21-O45, Medium, open)**: after the instrumented run, the Sensor service was started with the Monitor watching: `Connected to relay` 20:45:13.623, Monitor `Pending alarms: POWER_DISCONNECTED` 20:45:13.925, `Siren started` 20:45:14.369, on a phone plugged into USB. The Sensor's first ticks carry `isCharging = false` before its battery receiver has run.
9. **Documents**: SOT rule 1.376 (R-ID 147), 451 rules; `RESOLUTION_ARCHIVE.md`; Chapter 140 in `TESTS/TEST_PROCEDURE.md` (130 of 140 active); `TESTS/CHAPTER_TRIAGE.md` (QA 66 of 130); `TESTS/VERIFICATION_MANIFEST.md` header; `DOCS/issues.md`; baseline `STATUS/BASELINE/Nov7.21/`.

**Audit line**: Nov7.21: [SOT Count: 951 (Rules: 451), Open: H:1, M:4, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 192), QA: 66]

**Resumption Token**: NOV7.21-REALGPSLOSS-O45-NEXT

---

## 🎯 Next Resumption Focus
ISSUE-NOV7.21-O45 (Work Queue item 37): make the Sensor send its real charging state, battery, and temperature from its first tick (seed from the sticky `ACTION_BATTERY_CHANGED` Intent returned by `registerReceiver` before `startSensingLoop`), so a Sensor start raises no `Power disconnected` siren, with tests; consider closing ISSUE-NOV7.2-O37 in the same version.

---

## 📂 Key Implementation Files
- `core-shared/src/main/kotlin/com/jd/core/shared/Models.kt` (`LocationUpdate.isCritical`).
- `core-network/src/main/kotlin/com/jd/core/network/SignalingQueue.kt`, `SocketioSignalingClient.kt`; tests `core-network/src/test/kotlin/com/jd/core/network/SignalingQueueTest.kt`, `SignalingBatchTest.kt`.
- `app-sensor/src/main/kotlin/com/jd/app/sensor/FixFreshness.kt` (new), `MovementCheck.kt`, `SensorService.kt`; tests `app-sensor/src/test/kotlin/com/jd/app/sensor/FixFreshnessTest.kt` (new), `MovementCheckTest.kt`.
- `app-monitor/build.gradle.kts`, `app-sensor/build.gradle.kts` (versions).
- `TESTS/TEST_PROCEDURE.md`, `TESTS/VERIFICATION_MANIFEST.md`, `TESTS/CHAPTER_TRIAGE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `DOCS/issues.md`, `Handover.md`.

**Handover complete. System state locked at Nov7.21.**
