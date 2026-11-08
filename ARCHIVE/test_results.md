# Test Results - Nov7.3 (Monitor Alarms And Siren)

## Nov7.3 Summary
Executed Nov 7, 2026 on the A15 (`R58X40GV2AR`, API 36), the only phone attached, with Sensor and Monitor on the same phone through the production relay. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **222/222** | `:core-shared` 46, `:core-network` 18, `:core-ui` 29, `:app-monitor` unit 43, `:app-monitor` on A15 44, `:app-sensor` on A15 42. Unit tasks were forced with `--rerun-tasks`; fresh result files were counted, with 0 failures and 0 errors. Both instrumented suites ran on the final code at `Nov7.3`. |
| **First Monitor run** | 🟠 **FIXED** | `MapJitterAuditTest` crashed the process with `CannotPostForegroundServiceNotificationException`: under the test application `MonitorApp` never runs, so the new session channel did not exist. `MonitorSessionService.onCreate` now calls the single channel definition. `theSirenPlaysInALoopUntilStopped` failed only because of that crash; it passed in the rerun. |
| **129.1 to 129.3 Alarm logic** | 🟢 **PASSED (automated)** | `MonitorAlarmsTest`, 15 tests, including the book flow with only the paired tracker, no evaluation before settings, acknowledgement, and a new pairing. |
| **129.4 to 129.6 Siren, silence, channels** | 🟢 **PASSED (automated)** | `SirenSoundTest` 5, `SensorStealthGuardTest` 3, `MonitorAlarmDeviceTest` 5 on the A15, playing at gain 0. |
| **Deliberate breaks** | 🟢 **PASSED** | Dropping an unacknowledged alarm when it cleared failed `anUnacknowledgedAlarmStaysAfterItsConditionClears`; restoring the maximum instead of the saved volume failed `aForcedMaximumVolumeIsRestoredWhenTheSirenStops` (expected 7, was 15). Both were restored. |
| **Test side effect** | 🟠 **FIXED** | `MonitorAlarmDeviceTest` changed the A15 alarm volume, which `dumpsys audio` showed at 1 before the first run. It was set back to 1, and the test now saves and restores the phone's alarm volume; after the final run it was still 1. |
| **129.7 Alarms on hardware** | 🟢 **PASSED** | User decision: low volume, so `Force Maximum` was turned off in the Monitor and the alarm stream stayed at 1 of 15. Unplug at 15:04:10: `POWER_DISCONNECTED` at 15:04:11.3, siren on `USAGE_ALARM` in `dumpsys audio`, full-screen `Tracker T power disconnected since 15:04:11`, notification 2001 on `monitor_alarm` (importance 4, category alarm, full-screen intent). Acknowledged at 15:04:47: siren stopped and notification removed. Re-plugged, sent to the background, unplugged at 15:05:33: raised again with the launcher in front; the phone showed a heads-up, and tapping it in the shade opened `AlarmOverlayActivity`, where acknowledging closed it. Sensor force-stopped at 15:07:17: `TRACKER_OFFLINE` at 15:07:53; after the Sensor was restarted it showed `(cleared)` with the siren on until acknowledged. Level 15 and temperature 470: both alarms at 15:09:25, `(cleared)` after `dumpsys battery reset`, silenced at 15:10:25. `TEST SIREN AUDIO` started and stopped the siren. Battery state reset and checked afterwards. |
| **Channels on hardware** | 🟢 **PASSED** | `monitor_alarm` importance 4, `monitor_session` importance 2; the Monitor's lowered channel was removed. `mBypassDnd=false`, because the app has no Do Not Disturb access (ISSUE-NOV7.3-O39). |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd` at 15:09:57, Sensor and Monitor processes, log cleared at about 15:01: 0 Davey over 500ms, 0 StackLog, 0 fatal. |
| **Not run** | ⚪ | The full-screen start over a locked screen, the siren at maximum volume on hardware, and two phones. |

**Run history:** one fixed failure in the first Monitor run, and the two intended failures above. Every other run passed.

---

# Nov7.2 Results (Real Sensor HUD Values)

## Nov7.2 Summary
Executed Nov 7, 2026 on the A15 (`R58X40GV2AR`, API 36), the only phone attached, with Sensor and Monitor on the same phone through the production relay and real positions. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **194/194** | `:core-shared` 46, `:core-network` 18, `:core-ui` 26, `:app-monitor` unit 23, `:app-monitor` on A15 39, `:app-sensor` on A15 42. `MonitorHudValuesTest` (7) moved to `core-ui` as `FixReadingTest` (9). Unit tasks were forced with `--rerun-tasks`; fresh result files were counted, with 0 failures and 0 errors. Both instrumented suites ran on the final code at `Nov7.2`, after the deliberate break was reverted. |
| **128.1 Shared HUD rule** | 🟢 **PASSED (automated)** | `FixReadingTest`, 9 tests, including a fix without accuracy and a line keeping its connection fields. |
| **128.2, 128.3 Sensor HUD** | 🟢 **PASSED (automated)** | `SensorHudValuesTest`, 5 tests on the A15 against the real `SensorViewModel` and `SensorFixStore`. A match needs a battery reading, so the initial `stateIn` value cannot pass a test. |
| **Deliberate break** | 🟢 **PASSED** | With the hardcoded `STATIONARY`, `0f`, `5.0f`, and `0L` put back over the real values, 3 of 5 failed: `noFixShowsNoPositionValues`, `showsThePublishedFix`, and `clearingTheFixRemovesItsValues`. The two `Location` conversion tests passed because they do not use the ViewModel. The fix was then restored. |
| **128.4 Sensor HUD on hardware** | 🟢 **PASSED, stop step not run** | Before `START SENSOR SERVICE`: `--`, `-- km/h`, `B:100%⚡|T:28°C|A:--|AGE:--`. 15 seconds after start: `STATIONARY`, `1.3 km/h`, `A:±22.4m|AGE:2s`, while `dumpsys location` showed the fused fix at `hAcc=22.4` and `vel=0.3656` m/s, which is 1.3 km/h. Four more samples 2 seconds apart: `±13.8m`, `±13.5m`, `±12.8m`, `±12.8m` with `AGE` 0, 0, 0, and 4s. The stop step was not run: the app has no stop control, and `am stopservice` was refused because the service is not exported (ISSUE-NOV7.2-O36). |
| **Monitor after the move** | 🟢 **PASSED** | Paired with `T`: local line `A:±14.1m|AGE:15s`, sensor line `A:±13.5m|AGE:0s`. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd` at 11:45, Sensor and Monitor processes, log cleared before launch, Sensor service started at 11:42:47: 0 Davey over 500ms, 0 StackLog, 0 fatal. Sensor logged `onCreate called. Version: Nov7.2`. `ProductionReadinessAuditTest` ran in the Sensor suite. |

**Run history:** three intended failures, the deliberate break above. Every other test run passed the first time.

---

# Nov7.1 Results (Unpair Confirmation)

## Nov7.1 Summary
Executed Nov 7, 2026 on the A15 (`R58X40GV2AR`, API 36), the only phone attached, with Sensor and Monitor on the same phone through the production relay and real positions. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **187/187** | `:core-shared` 46, `:core-network` 18, `:core-ui` 17, `:app-monitor` unit 30, `:app-monitor` on A15 39, `:app-sensor` on A15 37. Unit tasks were forced with `--rerun`; fresh result files were counted, with 0 failures and 0 errors. The Monitor suite ran on the final code after the deliberate break was reverted. |
| **127.1, 127.2 Unpair controls** | 🟢 **PASSED (automated)** | `UnpairConfirmationTest`, 5 tests on the real `MonitorScreen` on the A15: gear, `Disconnect`, the three `CLEAN` actions, `Cancel`, and `Unpair` once. |
| **Deliberate break** | 🟢 **PASSED** | With `MonitorMenuBar` wired straight to `onReset`, 4 of 5 failed: `gearAsksBeforeUnpairing` and `disconnectAsksBeforeUnpairing` (no dialog), `cancelKeepsThePairing` and `confirmUnpairsOnce` (nothing to tap). `cleanActionsAskBeforeUnpairing` passed because its path was not changed. The fix was then restored. |
| **127.3 Unpair on hardware** | 🟢 **PASSED** | Both apps at `Nov7.1` (`versionCode` 42 Sensor, 40 Monitor), paired with `T`. Gear: dialog `Unpair from tracker T?` with `Cancel` and `Unpair`. `Cancel`: still paired, sensor line `AGE:0s`, `MonitorSessionService` running. `Disconnect`: the same dialog; `Unpair`: `Welcome to Watcher` pairing screen, service stopped. Paired again with `T`. Screens read with `uiautomator dump`. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd` at 11:21, Sensor and Monitor processes, log cleared at 11:19 before launch, about 2 minutes of runtime: 0 Davey over 500ms, 0 StackLog, 0 fatal. Sensor logged `onCreate called. Version: Nov7.1`. `ProductionReadinessAuditTest` ran in the Sensor suite. |

**Run history:** four intended failures, the deliberate break above. Every other test run passed the first time.

---

# Nov7.0 Results (Real Version Strings)

## Nov7.0 Summary
Executed Nov 7, 2026 on the A15 (`R58X40GV2AR`, API 36), the only phone attached, with Sensor and Monitor on the same phone through the production relay and real positions. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **182/182** | `:core-shared` 46, `:core-network` 18, `:core-ui` 17, `:app-monitor` unit 30, `:app-monitor` on A15 34, `:app-sensor` on A15 37. Unit tasks were forced with `--rerun`; result files were counted, with 0 failed and 0 skipped. |
| **126.1 No version in code** | 🟢 **PASSED (automated)** | `VersionLiteralGuardTest` scans all five modules. |
| **126.2 Version from the package** | 🟢 **PASSED (automated)** | `AppVersionTest` on the A15. The `--` case was not observed on an installed app. |
| **Deliberate break** | 🟢 **PASSED** | With `Nov1.7 (Final)` and the `Nov4.27` fallback put back, `noProductionStringHoldsAVersion` failed and named both (1 of 3), and `anUnknownPackageShowsNoVersion` failed on the A15 (1 of 2). The fix was then restored. |
| **126.3 Version on hardware** | 🟢 **PASSED** | `dumpsys`: `versionName` Nov7.0, `versionCode` 41 Sensor, 39 Monitor. Sensor first screen and Monitor pairing screen: `Version: Nov7.0`. Both Setup dialogs: `App Version: Nov7.0`. Log at 10:27:52: `REV_SensorService: onCreate called. Version: Nov7.0`. No log line held `Nov1.7` or `Nov4.27`. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd` at 10:30, Sensor and Monitor processes, log cleared at install, about 2 minutes of runtime: 0 Davey over 500ms, 0 StackLog, 0 fatal. `ProductionReadinessAuditTest` ran in the Sensor suite. |

**Run history:** two intended failures, the deliberate break above. Every other test run passed the first time. One foreground command with a 60-second wait timed out and was repeated as short commands.

---

# Nov6.4 Results (Backfill Gap Recovery)

## Nov6.4 Summary
Executed Nov 6 and 7, 2026 on the A15 (`R58X40GV2AR`, API 36). The S21FE stayed attached during the first Monitor suite, took no part, and kept its Monitor; it was then unplugged, so no two-phone run was possible. The live check used a private copy of `relay-server/index.js`; no test traffic reached the production relay, and both apps used real positions. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **177/177** | `:core-shared` 46, `:core-network` 18, `:core-ui` 14, `:app-monitor` unit 30, `:app-monitor` on A15 34, `:app-sensor` on A15 35. Unit tasks were forced with `--rerun`; result file times were checked. |
| **125.1 to 125.6** | 🟢 **PASSED (automated)** | `BackfillLedgerTest`, `BackfillGapRecoveryTest` with real Room on the A15, and two burst tests in `SignalingBatchTest`. |
| **Burst before the fix** | 🟢 **Defect shown** | On the unchanged client, with the collector fully busy, 64 of 600 telemetry updates and 8 of 200 backfill chunks arrived. This is a worst case: it shows the mechanism, not the loss rate in the field test. |
| **Deliberate break** | 🟢 **PASSED** | With the scan limited to the newest gap, `everyHoleIsRefilledWhenThereAreMoreThanTheLimit` and `aFailedRequestIsNotForgotten` failed (2 of 9). The fix was then restored. |
| **125.7 Outage on hardware** | 🟡 **PASSED, private relay only** | Sensor at 10Hz through a test proxy, Monitor direct. 08:40:35 data discarded for 20s, then connections refused for 40s. 08:41:38 Sensor reconnected; 08:41:40 `1 gaps stored. Requesting 1.`, `Requested 1921 to 2115` (195 rows), 4 chunks. Pulled table: 3,011 rows, sequences 1 to 3011, 0 holes. |
| **More holes than the limit, live** | ⚪ **NOT OBSERVED** | That outage produced one hole, so this case has automated proof only. |
| **Production relay and two phones** | ❌ **NOT RUN for Nov6.4** | The second half of 125.7 is outstanding. The production round trip was seen on Nov 6 with Nov6.3 (ISSUE-NOV5.2-O12). |
| **Unanswered request timeout, live** | ⚪ **NOT OBSERVED** | Covered by `BackfillLedgerTest`. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`, Sensor and Monitor processes, log cleared at the cut: 0 Davey over 500ms, 0 StackLog, 0 fatal. `ProductionReadinessAuditTest` ran in the Sensor suite. |

**Run history:** the first Monitor suite was 33 of 34: `everyHoleIsRefilledWhenThereAreMoreThanTheLimit` timed out because `FakeSignalingClient` still had a buffer of 8 and dropped 2 of 10 responses. Raising the fake to the production size fixed it. A first outage attempt was void: removing the `adb reverse` entry left the open socket running, so nothing was cut, and the test proxy replaced it.

---

# Nov6.3 Results (Real Relay Status Text)

## Nov6.3 Summary
Executed Nov 6, 2026 on the A15 (`R58X40GV2AR`, API 36), with Sensor and Monitor on the same phone through the production relay. The S21FE was attached at 14:51:41, during the Monitor suite, and took no part: tooling read its properties, and its adbd log shows no install, uninstall, or test. Every test ran on the A15. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **156/156** | `:core-shared` 46, `:core-network` 16, `:core-ui` 14, `:app-monitor` unit 20, `:app-monitor` on A15 25, `:app-sensor` on A15 35. Unit tasks were forced with `--rerun`. |
| **124.1 Status follows the relay** | 🟢 **PASSED (automated)** | `HudStateTest.relayStatusTextFollowsTheSrvState` and `RelayStatusDialogTest` on the A15, which renders the real `MonitorScreen` and opens the dialog. |
| **Deliberate break** | 🟢 **PASSED** | With the old hardcoded text put back, both `RelayStatusDialogTest` tests failed on `assertExists`. The fix was then restored. |
| **124.2 Relay lost on hardware** | 🟢 **PASSED** | Dialog open on the A15: `Connected`; mobile data off, `Disconnected` within 5s; mobile data on, `Connected` within 5s. Polled every 3s, so times are upper limits. The `SRV` LED colour was not read during this; both use the same field. |
| **Dialog content** | 🟢 **PASSED** | `Tracker ID: T`, `Monitor ID: V`, `App Version: Nov6.3`, `Relay Status: Connected`. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`, Sensor and Monitor processes: 0 Davey over 500ms, 0 StackLog, 0 fatal. |

**Run history:** one intended failure, the deliberate break above. Every other run passed the first time.

---

# Nov6.2 Results (Bounded Trail Storage)

## Nov6.2 Summary
Executed Nov 6, 2026 on the A15 (`R58X40GV2AR`, API 36), the only phone attached, so the reciprocal simulation applied. The simulation used a private copy of `relay-server/index.js` on the workstation through `adb reverse`; no simulated data reached the production relay. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **153/153** | `:core-shared` 46, `:core-network` 16, `:core-ui` 13, `:app-monitor` unit 20, `:app-monitor` on A15 23, `:app-sensor` on A15 35. Unit tasks were forced with `--rerun`. |
| **123.1 to 123.3 Row cap, sequence, local rows** | 🟢 **PASSED (automated)** | `TrailStorageBoundTest` on the A15, including 50,100 rows at the real limit. No code writes local rows yet (ISSUE-NOV6.2-O29). |
| **123.4 Scheduling** | 🟢 **PASSED** | One job in `dumpsys jobscheduler`; `Worker result SUCCESS` from the installed app, including after `install -r` over a paired Nov6.1. |
| **123.5 Pruning on hardware** | 🟢 **PASSED** | 59,799 rows; `Rows removed: 9799` at 14:39:03; `monitor_db` 5,185,536 to 4,440,064 bytes. No backfill request after pruning; sensor line stayed at `AGE:0s`. |
| **50.1 Periodic cleanup** | 🟢 **PASSED** | Ran at 14:24:03 and 14:39:03, exactly 15 minutes apart. This covers the trail worker only. |
| **Simulated viewer tests Sensor** | 🟢 **PASSED** | 286 real updates in 30s; `VWR` active while present and red after it left; backfill `243 to 262` answered with 20 rows in about 50ms. |
| **Simulated tracker tests Monitor** | 🟢 **PASSED** | `TRK` active; sensor line `B:73%\|T:32°C\|A:±6.5m\|AGE:0s`, `1.4 km/h`, matching the values sent; gap request `622 to 59999` answered in 60 chunks. |
| **Stress 4 Rapid rotation** | 🟢 **PASSED** | 10 rotations in about 10s on the Monitor with live telemetry and about 59,000 rows: same process, 0 ANR, 0 fatal. Run with a 0.5Hz simulated stream, not the 100Hz stream, which has no control. |
| **Other stress sections** | ⚪ **NOT RUN** | Sections 1, 2, and 3 have no controls in the apps (D); section 5 needs physical shielding (H). |
| **Live pair on production** | 🟢 **PASSED** | Sensor and Monitor on the A15 with real telemetry after the simulation was removed. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`, Sensor and Monitor processes: 0 Davey over 500ms, 0 StackLog, 0 fatal. |

**Run history:** Run 1 failed 1 of 23 Monitor instrumented tests: `schedulingTwiceKeepsOnePeriodicJob` assumed the state `ENQUEUED`, but a new periodic job starts at once. The test was wrong, not the product. It was corrected and the complete suite restarted; Run 2 passed 153 of 153. Only test code changed between the runs. `cleanTest` does not reset Android unit test tasks, so `--rerun` was used to force them.

---

# Nov6.1 Results (Real HUD Values)

## Nov6.1 Summary
Executed Nov 6, 2026 on the A15 (`R58X40GV2AR`, API 36), with Sensor and Monitor on the same phone through the production relay. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **145/145** | `:core-shared` 46, `:core-network` 16, `:core-ui` 13, `:app-monitor` unit 20, `:app-monitor` on A15 15, `:app-sensor` on A15 35. The A15 runs came before the version number change; no code changed after them. |
| **122.1 Local HUD line** | 🟢 **PASSED** | Local line `B:100%⚡\|T:29°C\|A:±77.6m\|AGE:23s`, then `AGE:38s`. `dumpsys battery`: level 100, status 5 (full), 29.1 to 29.3°C. |
| **122.2 Battery reading** | 🟢 **PASSED** | Automated; the Sensor's own HUD also showed the real `B:100%⚡\|T:29°C` on the A15. |
| **122.3 Unknown values** | 🟡 **AUTOMATED, PARTLY LIVE** | Local speed and state showed `--` with a network fix. A missing fix and missing telemetry were not observed live. |
| **122.4 Old fix** | 🟡 **AUTOMATED ONLY** | The Monitor was stationary throughout. |
| **Sensor line** | 🟢 **PASSED** | `STATIONARY 0.1 km/h`, `A:±11.2m`, `AGE:0s`, against GPS accuracy of about 11m. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`, Sensor and Monitor processes: 0 Davey over 500ms, 0 StackLog, 0 fatal. |

**Run history:** a foreground run was stopped by the tool timeout. Running both instrumented modules in one Gradle call then failed with `Starting 0 tests` and install errors, before any test ran. Each module passed when run alone.

---

# Nov6.0 Results (Forensic Entry Integrity & Chapter Review)

## Nov6.0 Summary
Executed Nov 6, 2026 on the A15 (`R58X40GV2AR`, API 36). The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **130/130** | `:core-shared` 46, `:core-network` 16, `:core-ui` 5, `:app-monitor` unit 13, `:app-monitor` on A15 15, `:app-sensor` on A15 35. |
| **121.1 to 121.4 Entry integrity** | 🟢 **PASSED (automated)** | Interrupted states are simulated in the file. A deliberate break of the code failed 8 tests. Physical power loss was not run. |
| **121.5 Upgrade** | 🟢 **PASSED** | Full version 2 file upgraded in place on the A15: 10000 entries kept, numbering continued from 638988. |
| **121.6 Older Android** | 🟡 **AUTOMATED ONLY** | Compiled class checked; not run below API 35 (ISSUE-NOV6.0-O26). |
| **121.7 Process kill** | 🟢 **PASSED** | Three `kill -9` during 10Hz writing; restarted by itself each time; no damaged entries. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`, Sensor process: 0 Davey over 500ms, 0 StackLog, 0 fatal. |
| **120.7 Backfill** | 🟡 **LOCAL RELAY ONLY** | Still not verified on production (ISSUE-NOV5.2-O12). |

---

# Nov5.2 Results (Chapter Triage & Runtime Resilience)

## Nov5.2 Summary
Executed Nov 5, 2026 on the A15 (`R58X40GV2AR`), running both Sensor and Monitor on the production relay unless noted. The S21FE was not used. Status of every chapter: `TESTS/CHAPTER_TRIAGE.md`.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **118/118** | `:core-shared` 34, `:core-network` 16, `:core-ui` 5, `:app-monitor` unit 13, `:app-monitor` on A15 15, `:app-sensor` on A15 35. |
| **54.1, 65.1, 120.1 Crash recovery** | 🟢 **PASSED** | Restarted in under 10s; sequence continued (518 to 600). |
| **70.1 Monitor crash** | 🟢 **PASSED** | Restarted and reconnected. |
| **16.1 Network loss** | 🟢 **PASSED** | 40s outage; reconnected in about 7s. |
| **23.1, 120.2 Doze** | 🟢 **PASSED** | 12m47s forced deep idle; wake lock held, telemetry continued every 10s. |
| **49.1 Charging rate** | 🟢 **PASSED** | 100ms charging, 10000ms on battery. |
| **73.1 Permission revoke** | 🟢 **PASSED** | Sensor stopped cleanly, no crash loop. |
| **0.4 Back button** | 🟢 **PASSED** | Service kept running. |
| **14.1, 42.1 Rotation** | 🟢 **PASSED** | No overlap in landscape. |
| **41.1 Dark mode** | 🟢 **PASSED** | App stayed up; legibility not separately measured. |
| **32.1, 120.8 Largest font** | 🟢 **PASSED** | Failed first; speed and labels now visible at 200%. |
| **38.1, 120.5 Bad relay URL** | 🟢 **PASSED** | Could not be saved; no crash. |
| **0.8, 120.9 ID isolation** | 🟢 **PASSED** | Failed first; no values carry over to a new pairing. |
| **5.2 Trail persistence** | 🟢 **PASSED** | Trail present immediately after force stop. |
| **113, 120.6 Menu** | 🟢 **PASSED** | All eight actions positioned on screen. |
| **118.8 Relay hot switch** | 🟢 **PASSED** | Sensor switched while connected in under 40ms. |
| **120.7 Backfill** | 🟡 **LOCAL RELAY ONLY** | 1 request, 7 chunks through local v6.044. Production relay is still v6.043. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | `device_audit.cmd`: 0 Davey over 500ms, 0 StackLog, 0 fatal on both processes. |
| **20.1 Reboot** | 🟢 **PASSED** | Sensor restarted by itself after boot, within about 3 minutes; sequence continued (about 2646 to 3054). |

**Run history:** Run 1 passed; font and backfill failures found live, fixed; Run 2 passed; isolation failure found live, fixed; Run 3 passed on the final code.

---

# Nov5.1 Results (Default Relay Correction & Production Verification)

## Nov5.1 Summary
Executed Nov 5, 2026 on the final build, with Sensor on A15 and Monitor on S21FE through the production relay `https://gps-survival-relay.onrender.com` (v6.043).

| Check | Status | Notes |
| :--- | :--- | :--- |
| **Automated suite** | 🟢 **103/103** | `:core-shared` 27, `:core-network` 15, `:core-ui` 5, `:app-monitor` unit 9, `:app-monitor` on A15 12, `:app-sensor` on A15 35. |
| **119.1 Single default relay** | 🟢 **PASSED** | Automated by `RelayConfigTest`; both apps show the new default. |
| **119.2 Production relay link** | 🟡 **PARTIAL** | `SRV` green on both phones, and they reconnected about 4s after the relay restart. Recovery from an idle cold start over 20s was not observed (ISSUE-NOV5.1-O9). |
| **119.3 Production telemetry** | 🟢 **PASSED** | `TRK` and `VWR` active, `B:79%` matching the A15, live `AGE`, sensor marker at the real position. |
| **118.8 Relay hot switch** | 🟢 **PASSED** | S21FE moved from the old URL to the new one without an app restart; the timeouts stopped. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | Zero Davey, StackLog, or fatal entries from either app process, and no relay timeouts after the URL change. |

The first Nov5.1 live attempt showed `TRK` and `VWR` red because the deployed relay predated the apps' protocol. Publishing relay v6.043 resolved it. Section 3 below still applies: manual physical chapters were not executed in Nov5.1 either.

---

# Nov5.0 Results (HUD Direction, Real-Position Centering & Telemetry Integrity)

Executed Nov 5, 2026. Sensor on A15 (`R58X40GV2AR`), Monitor on S21FE (`R5CRC14PG4F`), both on system language `he-IL`.

## 1. Automated Suite (Final Code, Run 5)
All instrumented tests were pinned to the A15. Unit results cover debug and release variants where the module has both.

| Suite | Result |
| :--- | :--- |
| `:core-shared` unit (including `ForensicBufferStressTest`, `KinematicCalibrationTest`, `AlarmStealthTest`) | 🟢 24/24 |
| `:core-network` unit | 🟢 15/15 |
| `:core-ui` unit | 🟢 5/5 |
| `:app-monitor` unit | 🟢 9/9 |
| `:app-monitor` instrumented on A15 | 🟢 12/12 |
| `:app-sensor` instrumented on A15 (including `SensingEngineStressTest`, `ForensicStressAuditTest`, `HardeningAuditTest`, `PipelineSoakTest`, `ProductionReadinessAuditTest`) | 🟢 35/35 |
| **Total** | **🟢 100/100** |

`:app-sensor` has no unit tests.

### Run History
| Run | Result | Remediation |
| :--- | :--- | :--- |
| Pre-run | 🔴 Monitor instrumented suite did not compile | Added `viewerStatusFlow` to `FakeSignalingClient` |
| 1 | 🔴 `compactHudIsShorterThanStandardHud` failed | The test harness lacked the app theme; wrapped it in `RevolutionTheme`, threshold unchanged |
| 2 | 🟢 Passed | Live test then exposed zeroed telemetry; fixed |
| 3 | 🟢 Passed | Live test then exposed the `(0,0)` trail line; fixed |
| 4 | 🟢 Passed | Live test then exposed the ignored relay URL change; fixed |
| 5 | 🟢 Passed | Final code |

## 2. Two-Device Live Run (Sensor A15, Monitor S21FE)
**Constraint:** the configured relay did not respond because the apps used the wrong host (ISSUE-NOV5.0-O1, corrected in Nov5.1), so this run used the same `relay-server/index.js` on the workstation through `adb reverse`. Real internet relay traffic was not tested. The live run used the build before the final `connect()` change (Run 4 code); that change has automated coverage only.

| Check | Status | Notes |
| :--- | :--- | :--- |
| **118.1 HUD left-to-right** | 🟢 **PASSED** | Both phones, `he-IL`: `INT` left of `WDG`, row 2 in correct order. |
| **118.2 Real-position centering** | 🟢 **PASSED** | Monitor on the real region and not California; sensor marker at the real position. |
| **118.3 Compact HUD** | 🟢 **PASSED** | Four lines in 202px on S21FE, no clipping. |
| **118.5 VWR presence** | 🟢 **PASSED** | Sensor `VWR` active with the Monitor connected. |
| **118.6 Telemetry integrity** | 🟢 **PASSED** | Relay carried real sequence and coordinates; Monitor showed `B:79%`, matching the A15, with a live `AGE`. |
| **118.7 No-fix filtering** | 🟢 **PASSED** | The line to `(0,0)` is gone. |
| **118.8 Relay hot switch** | 🟡 **AUTOMATED ONLY** | Unit test passes; not verified on hardware. |
| **106 / 116 LEDs** | 🟢 **PASSED** | `SRV`, `GPS`, `TRK`, `VWR`, `DAT`, `WDG` active with the link up; `SRV` and `TRK` red during the relay outage. |
| **0.1 Logcat anomalies** | 🟢 **PASSED** | 75s soak on both phones plus Run 5 logs: zero Davey and zero StackLog from the app processes. Lines seen came from other apps. No `FATAL`, ANR, `SQLiteDatabaseLocked`, or `NaNG`. |
| **Event log** | 🟢 **PASSED** | Sensor live event log and Monitor logcat showed service start and relay connection. |

## 3. Not Executed
The reciprocal simulation did not run, because the S21FE was connected and the two-device procedure applies instead.

**`TEST_PROCEDURE.md`:** chapters beyond those listed above were not individually executed as manual steps. Some logic is covered by the automated suites, but these need physical action or setup that was not performed, including GNSS shielding (5), stress and thermal triggers (6), sirens and alarms (8, 9, 26, 34), power and reboot (15, 20), network switching (16), multiple viewers (19, 40), TalkBack, font scaling, and orientation (31, 32, 42), sunlight and sensors (37, 43, 44), and long soaks (53, 93).

**`TEST_FORENSIC_STRESS_PROTOCOL.md`:** sections 1 to 5 are manual procedures and were not executed on the device UI. Related automated suites passed, but they do not replace these steps.

**Chapter 0:** 0.2, 0.4, 0.5, 0.7, 0.8, and 0.9 were not individually verified. Under 0.6, only the `VWR` badge was verified, not geofence sync.

---
*Verified on physical devices R58X40GV2AR (A15) and R5CRC14PG4F (S21FE) - Baseline Nov5.1*
