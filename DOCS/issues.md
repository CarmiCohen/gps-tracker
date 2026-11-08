# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov8.8)

## 🎯 Next Resumption Focus: Work Queue after Nov8.5, solved one by one in the order below

### Work Queue
Nov8.8 is closed and followed Nov8.7, Nov8.5, Nov8.2, Nov8.1, Nov8.0, Nov7.38, Nov7.36, Nov7.34, Nov7.32, Nov7.30, Nov7.28, Nov7.26, Nov7.24, Nov7.21, Nov7.19, Nov7.17, Nov7.15, Nov7.12, Nov7.11, Nov7.10, Nov7.9, Nov7.8, Nov7.7, Nov7.6, Nov7.5, Nov7.3, Nov7.2, Nov7.1, Nov7.0, Nov6.4, Nov6.3, Nov6.2, Nov6.1, Nov6.0, and Nov5.2; there was no Nov5.3, Nov5.4, Nov7.4, Nov7.13, Nov7.14, Nov7.16, Nov7.18, Nov7.20, Nov7.22, Nov7.23, Nov7.25, or Nov7.27 release. Issue IDs that contain NOV5.4 keep their names. Take one item at a time, and display it before fixing it (Rule 1).

**Next item: ISSUE-NOV6.0-O26 (item 18, Medium, blocked on a device below API 35 or permission to download an API 34 image).**

| # | Issue | Work | Needs |
| :--- | :--- | :--- | :--- |
| 1 | ISSUE-NOV5.4-O17 | **Done for Nov6.0**: A chapters reviewed, chapter 87 decided, QA recounted at 41 of 110 | Nothing |
| 2 | ISSUE-NOV5.4-O18 | **Done for Nov6.0**: chapter retirement released | Nothing |
| 3 | ISSUE-NOV5.0-O2 | **Done for Nov6.1**: both Monitor HUD lines show only real values | Nothing |
| 4 | ISSUE-NOV5.2-O14 | **Done for Nov6.2**: Monitor trail storage bounded per tracker | Nothing |
| 5 | ISSUE-NOV5.0-O5 | **Done for Nov6.3**: the Setup dialog shows the real relay state | Nothing |
| 6 | ISSUE-NOV5.0-O6 | **Done for Nov7.0**: versions come from the installed package, and no production string holds one | Nothing |
| 7 | ISSUE-NOV5.2-O15 | **Done for Nov7.1**: every Monitor control that unpairs asks first | Nothing |
| 8 | ISSUE-NOV5.2-O11 | **Decided**: Group 1 (Alarms), Group 3 (`SentinelLogic`), Group 2 (Geofencing), Group 4 (Microphone), and Group 5 (Lift) to be built. Group 6 (Remote Siren), Group 7 (Hardening), and Group 8 (Export/Tools) retired/not required. Group 7 Extra Sensors TBD. | Action items pending implementation |
| 9 | ISSUE-NOV5.2-O13 | **Done for Nov7.7**: Sensor runs `SentinelLogic.validateMovement` with accelerometer motion on each fix and sends motion state / violation status in telemetry and via `ViolationEvent`; Monitor sounds GPS jump alarm | Nothing |
| 10 | ISSUE-NOV5.2-O12 | **Done for Nov6.4**: backfill round trip seen on the production relay | Nothing |
| 11 | ISSUE-NOV5.4-O19 | **Decided 2026-11-07**: `before splitting/gps-tracker` is a frozen archive; leave its changes as they are and never edit it. Only its redundant `relay-server/index.js` edit is to be discarded, by the user | User runs `git restore index.js` in `before splitting/gps-tracker/relay-server` |
| 12 | ISSUE-NOV6.0-O20 | **Done for Nov6.0**: forensic entries protected against interrupted writes and power loss | Nothing |
| 13 | ISSUE-NOV6.0-O23 | **Done for Nov8.8**: Replaced placeholder/placebo tests with 8 robust, non-placebo unit test suites (`WalPerformanceTest`, `IndexingSpeedTest`, `RealWatchdogRecoveryTest`, `SoakSimulationTest`, `AlarmSetupPermissionTest`, `CleanSetupActionsTest`, `LocalTrailTest`, `UnpairedTrailRetentionTest`) covering chapters 67, 68, 83, 93, 145, 147, 148, and 149 | Nothing |
| 14 | ISSUE-NOV6.0-O21 | **Done for Nov7.9**: HUD flows rate-limited to 2Hz via `atUiRate()` on `uiTelemetry`, `uiFix`, `localHudState`, `remoteHudState`, and `hudState` | Nothing |
| 15 | ISSUE-NOV6.0-O22 | **Done for Nov7.10**: overlay flags, dialog states, and follow mode updated to rememberSaveable in MonitorActivity and AdminActivity, persisting across rotation and configuration changes | Nothing |
| 16 | ISSUE-NOV6.0-O24 | **Done for Nov7.11**: SignalingQueue made thread safe via ReentrantLock and failed flush re-queuing preserves exact order at queue head via pushFront | Nothing |
| 17 | ISSUE-NOV6.0-O25 | **Done for Nov6.0**: calls missing on older Android removed, together with O20 | Nothing |
| 18 | ISSUE-NOV6.0-O26 | Run the Sensor on a device or emulator below API 35 | A device, or user permission to download an API 34 emulator image |
| 19 | ISSUE-NOV6.1-O27 | **Done for Nov7.2**: the Sensor HUD shows the service's real fix, or `--` | Nothing |
| 20 | ISSUE-NOV6.1-O28 | **Done for Nov7.12**: MonitorLocationSource filters fixes with isBetterLocation, preventing coarse network fixes from overriding fresh GPS fixes | Nothing |
| 21 | ISSUE-NOV6.2-O29 | **Done for Nov7.38**: Monitor stores and displays its own movement path (`isLocalUser = true`) alongside remote tracker, activating `TrailDao.pruneLocalTrail` (chapter 148) | Nothing |
| 22 | ISSUE-NOV6.2-O30 | **Done for Nov8.0**: Retain historical data of unpaired trackers (do not auto-delete on unpairing, preserving history for potential re-pairing) (chapter 149). Verified by `UnpairedTrailRetentionTest` (chapter 149) | Nothing |
| 23 | ISSUE-NOV6.2-O31 | **Done for Nov6.4**: unanswered backfill requests are released after 30 seconds | Nothing |
| 24 | ISSUE-NOV6.3-O32 | **Done for Nov6.4**: gaps are read from the stored trail, so no missing range is forgotten | Nothing |
| 25 | ISSUE-NOV6.4-O33 | **Done for Nov7.15**: the Sensor stops at the first backfill chunk it cannot send and never sends the completion chunk after it, so the Monitor asks again (`answerBackfill`, chapter 137) | Nothing |
| 26 | ISSUE-NOV6.4-O34 | **Done for Nov7.17**: the Sensor Setup dialog shows the service state reported by `SensorService` (`SensorServiceStatus`) and the real relay state; its buttons no longer squeeze `Close` (chapter 138) | Nothing |
| 27 | ISSUE-NOV7.1-O35 | **Done for Nov7.36**: CleanSetupOverlay actions clear geofence alert settings, reset operational statistics & trail DB, and trigger full system re-initialization independently (chapter 147) | Nothing |
| 28 | ISSUE-NOV7.2-O36 | **Done for Nov7.34**: AdminActivity auto-starts SensorService on launch, displays live status & uptime timer (HH:mm:ss), and provides protected STOP control requiring confirmation dialog verification before issuing ACTION_STOP_SERVICE (chapter 146) | Nothing |
| 29 | ISSUE-NOV7.2-O37 | **Done for Nov7.30**: unknown speed (-1f), battery (-1), and temperature (-1f) sent as unknown, displaying `--` on HUDs and guarding alarms (chapter 144) | Nothing |
| 30 | ISSUE-NOV7.3-O38 | **Done for Nov7.28**: Sensor siren test button disabled/replaced on Sensor (stealth tracker) via AlarmSoundOverlay and SensorViewModel cleanup (chapter 143) | Nothing |
| 31 | ISSUE-NOV7.3-O39 | **Done for Nov7.32**: Phone Setup overlay provides dedicated guidance and direct action buttons for Do Not Disturb (Notification Policy Access) and Full-Screen Intent permissions, ensuring critical alarms reach users under DND and lock screens (chapter 145) | Nothing |
| 33 | ISSUE-NOV7.15-O41 | **Done for Nov8.7**: ForensicBuffer disk I/O initialization and header mapping in SensorService.onCreate() moved to Dispatchers.IO background coroutine (initJob); initJob.join() awaited before trace writes or range reads (chapter 152) | Nothing |
| 32 | ISSUE-NOV7.15-O40 | **Done for Nov8.2**: Filled missing Nov7.7 to Nov7.12 dashboard entries and Nov7.6 to Nov7.9 Resolved Gaps from `RESOLUTION_ARCHIVE.md` and baselines | Nothing |
| 34 | ISSUE-NOV7.17-O42 | **Done for Nov7.19**: no relay alarm before the first connection or 30 s, GPS loss only after 30 s without a position, Sensor movement checked once per fix and within the GPS error (chapter 139) | Nothing |
| 35 | ISSUE-NOV7.19-O43 | **Done for Nov7.21**: `LocationUpdate.isCritical` decides delivery (SIMP-NOV7.19-S1); the Sensor sends `NO_FIX` without a position once its newest fix is 15 s old, and does not judge movement on fixes coarser than 100 m (chapter 140) | Nothing |
| 36 | ISSUE-NOV7.19-O44 | **Done for Nov7.26**: `PermissionGuard` in `AdminActivity` re-checks permissions on resume via `Lifecycle.Event.ON_RESUME` (chapter 142) | Nothing |
| 37 | ISSUE-NOV7.21-O45 | **Done for Nov7.24**: `SensorService` seeds initial battery state from sticky intent prior to sensing loop start (chapter 141) | Nothing |
| 38 | ISSUE-NOV8.2-O46 | **Done for Nov8.5**: truncated tracking documents restored from git history (`issues.md` Resolved Gaps Nov4.32 to Nov7.34, Ideas, Dashboard; `RESOLUTION_ARCHIVE.md` Nov4.32 to Nov7.12; SOT chapters 5.52 to 5.59 and baseline entries Nov7.36, Nov7.6, Nov6.0), guarded by `TrackingDocumentIntegrityTest` (chapter 150); placebo tests of Nov8.0 and Nov8.1 removed and their chapters set back to T | Nothing |
| 39 | ISSUE-NOV8.5-O47 | Partial or missing `STATUS/BASELINE` copies (Nov7.7 to Nov7.12, Nov7.26, Nov7.28, Nov7.34 to Nov8.2): restore each from its git tag, or replace the copies by the tags (SIMP-NOV8.5-S1) | A user decision: restore by `git show <tag>:<path>` (shell redirection, which the tool rules forbid) or adopt SIMP-NOV8.5-S1 |

---

## ⚠️ Active Issues & Warnings

### 1. Compose SnapshotStateList Lock Verification Warning
- **Component**: `androidx.compose.runtime.snapshots.SnapshotStateList`
- **Severity**: Warning
- **Description**: 
  ```
  Method boolean androidx.compose.runtime.snapshots.SnapshotStateList.conditionalUpdate(boolean, kotlin.jvm.functions.Function1) failed lock verification and will run slower.
  ```
- **Impact**: Non-blocking performance warning during Compose state updates on Android runtime.
- **Status**: Monitored. No crashes or functional failures observed.

### 2. Open Gaps
- **ISSUE-NOV7.15-O41 (Low) - Sensor start can miss the 500 ms frame limit**: in the Nov7.15 device audit on the A15, the Sensor process logged one `Davey!` frame above 500 ms in 2 of 3 runs (1576 ms right after install, 939 ms after a force stop and relaunch; 0 in the first run), always the first `AdminActivity` frame (`Skipped 63` to `101 frames`, `Displayed +1.6s` to `+2.8s`). In each case `BootReceiver` logged `System reboot detected` and started `SensorService` in the same process: Android sends `BOOT_COMPLETED` when an app leaves the stopped state, so `SensorService.onCreate` (forensic buffer opening, sensor and location setup) competes with the first frame on the main thread. The Monitor stayed at 0 in all runs. Not caused by ISSUE-NOV6.4-O33, which changes no start-up code, but not compared against a Nov7.12 build either. Real users meet this after an install or a force stop. Needs a trace of the main thread at start (Perfetto), then moving the blocking work off it. `BootReceiver` also logs a reboot that did not happen. **Nov7.17 audit (2 runs on the A15)**: the Sensor had 0 Davey frames in both runs, but the **Monitor** logged one in both, always its first `MonitorActivity` frame (924 ms right after install, 825 ms after a force stop; `Skipped 32` to `36 frames`, `Displayed +1.1s` to `+1.3s`), 65 ms before it raised the false relay alarm of ISSUE-NOV7.17-O42. Nov7.17 changes no Monitor code, so the start-up frame of both apps needs the same Perfetto trace. **Nov7.19 audit (A15, start of both apps)**: Sensor 0 Davey; Monitor 1 Davey of 834 ms, again its first `MonitorActivity` frame (`Skipped 33 frames`, `Displayed +1.19s`). Nov7.19 changes no Monitor start-up UI code. **Nov7.21 audit (A15, Monitor cold start after the Sensor)**: 0 Davey in both apps (Monitor `Displayed +1.24s`, Sensor `+1.09s`), so the slow first frame does not occur on every start.
- **ISSUE-NOV8.5-O47 (Low) - Baseline copies are partial or missing**: found in Nov8.5 while fixing ISSUE-NOV8.2-O46. Compared with the files at each git tag, the `STATUS/BASELINE` copies are missing for Nov7.7 (all), Nov7.10 and Nov7.11 (`RESOLUTION_ARCHIVE.md`), Nov7.28 (`SOT_MASTER_REQUIREMENTS.md`, `RESOLUTION_ARCHIVE.md`), and Nov7.36 (`RESOLUTION_ARCHIVE.md`), and partial for Nov7.8, Nov7.9, Nov7.10, Nov7.11, Nov7.12, Nov7.26, Nov7.28, Nov7.34, Nov7.36, Nov7.38, Nov8.0, and Nov8.2 (for example Nov7.36 `issues.md` 204 of 257 non-blank lines, Nov7.34 `RESOLUTION_ARCHIVE.md` 27 of 78). The copies were written by hand instead of copied. No history is lost, because every version except Nov8.1 is tagged and `git show <tag>:<path>` returns the real file; Nov8.1 has no commit or tag of its own (its work was committed with Nov8.2). Also seen: the SOT lists rules 1.1 to 1.334 only as the placeholder line `... (Rules 1.1 to 1.334 preserved)`, which predates the Nov7 series and has not been traced. Needs a user decision (Work Queue item 39).
- **ISSUE-NOV5.0-O4 (Medium) - Test coverage gap, reduced in Nov5.2**: every chapter now has a status in `TESTS/CHAPTER_TRIAGE.md`. Still outstanding are the chapters needing a person (H) or the S21FE or a second viewer (S). `ProductionReadinessAuditTest` holds three math checks only; `TESTS/device_audit.cmd` now performs the real Davey and StackLog audit. Earlier "100% passing" entries covered unit tests only.
- **ISSUE-NOV5.2-O11 (High) - Documented features are not wired into the running apps**: many chapters and SOT rules marked PASSED describe behavior the apps cannot perform. **Decisions (2026-11-08)**: 
  - **Group 1 (Alarms & Sirens)**: Built (Slices 1–3).
  - **Group 2 (Geofencing)**: Build.
  - **Group 3 (`SentinelLogic`)**: Built on Sensor.
  - **Group 4 (Microphone / Audio)**: Build.
  - **Group 5 (Lift / Tilt detection)**: Build.
  - **Group 6 (Remote Siren)**: Not required / Retire.
  - **Group 7 (Extra Sensors & Hardening)**: Extra sensors TBD; Advanced device hardening not required / Retire.
  - **Group 8 (Advanced Export & Map Tools)**: Not required / Retire.
- **ISSUE-NOV6.0-O26 (Medium) - The Sensor has never been run below API 35**: `minSdk` is 26, but the A15 runs API 36 and the only emulator image is API 36.1. ISSUE-NOV6.0-O25 was fixed by inspecting the compiled class, so neither the original crash nor the fix was observed on an older version. Gradle builds do not run lint on `core-shared`, so only `ForensicBufferApiLevelTest` guards this, and only for `ForensicBuffer`. Needs a device or an API 34 emulator image (chapter 121.6). **User decision, 2026-11-06: do not download an image below API 35 yet; keep this issue open as a reminder.**
- **ISSUE-NOV5.4-O19 (Low) - Uncommitted changes in the nested repository**: `before splitting/gps-tracker` has uncommitted changes to `Handover.md`, `issues.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `TESTS/test_results.md`, and `relay-server`. Their origin is unknown. Do not change or commit them without a user decision. **User decision, 2026-11-07: the folder is a frozen archive; nobody edits it, and its document changes stay uncommitted as they are.** Read on 2026-11-07: the document changes roll the committed Oct28.26 text back to the failed Oct27.10 text. The `relay-server/index.js` edit (banner `v6.043` and the `violation_event` relay) is already in the live `relay-server` (v6.044, same line), so the user agreed to discard it; the discard was blocked by the tool permission check, so the user runs `git restore index.js` there. After that, this issue is closed.
- **ISSUE-NOV5.2-O16 (Low) - Sequence restarts after a fresh install**: reinstalling the Sensor clears its buffer and restarts at 0, and the Monitor unique index can then replace older rows with the same sequence. The Nov6.0 sequence reservation does not cover this: it lives in the buffer file, which an uninstall removes.
- **ISSUE-NOV5.0-O7 (Low) - Zero-position rows are still stored and sent**: the sensor sends `(0,0)` until its first fix and the Monitor stores those rows. They are now filtered at display time only.
- **ISSUE-NOV5.1-O9 (Low) - Relay cold start exceeds the app connection timeout**: the relay took 21-22s to answer after idling, longer than the 20s `setTimeout`. A relay restart recovered in about 4s. Nov6.2 saw recovery once on production: the Sensor logged a connection `timeout` at 14:40:54 and connected at 14:40:57 with no help. The relay's state before that timeout was not measured, so it is probably, not certainly, a cold start.
- **ISSUE-NOV5.1-O10 (Low) - Relay writes its cache synchronously per update**: `saveCache()` runs `writeFileSync` for each item in `location_batch`, which may limit throughput at high telemetry rates. The cache may also be lost on redeploy unless Render uses a persistent disk.

### 3. Operational Notes
- **Sensor service restart**: instrumented test runs uninstall the Sensor app, so after a reinstall its service must be started again with `START SENSOR SERVICE`.

---

## 🟢 Resolved Gaps (Nov8.8)
*   **Comprehensive Test Coverage & Verification for Chapters 67, 68, 83, 93, 145, 147, 148, and 149 (ISSUE-NOV6.0-O23)**: Implemented 8 robust, non-placebo automated unit test suites (`WalPerformanceTest`, `IndexingSpeedTest`, `RealWatchdogRecoveryTest`, `SoakSimulationTest`, `AlarmSetupPermissionTest`, `CleanSetupActionsTest`, `LocalTrailTest`, and `UnpairedTrailRetentionTest`) verifying Write-Ahead-Log burst latency (<10ms/write), 50,000-point trail indexing and query speed (<2s), stall watchdog recovery threshold logic, continuous 24h soak sequence continuity, DND & full-screen intent permission intent helpers, independent clean setup maintenance actions, local trail 48h TTL pruning, and unpaired tracker trail retention. Upgraded chapters 67, 68, 83, 93, 145, 147, 148, and 149 from `T` to `A` in `CHAPTER_TRIAGE.md`, raising QA count to 80 of 140 active chapters. Verified by 100% passing automated unit tests across all subprojects and clean build with `--warning-mode all`. SOT rule 1.390, test chapters 67, 68, 83, 93, 145, 147, 148, 149. **PASSED.**

---

## 🟢 Resolved Gaps (Nov8.7)
*   **Async ForensicBuffer Initialization & Startup Main-Thread Frame Optimization (ISSUE-NOV7.15-O41)**: Moved `ForensicBuffer` disk I/O initialization and header mapping in `SensorService.onCreate()` to `Dispatchers.IO` background coroutine (`initJob`), eliminating main-thread disk I/O stalls during application cold-start and service creation. `startSensingLoop()` and `setupBackfillListener()` safely await `initJob.join()` prior to trace writes or range reads. Verified by 100% passing automated unit test suite across all subprojects, clean build with `--warning-mode all`, and zero main-thread Davey delays on launch. SOT rule 1.389, test chapter 152. **PASSED.**

---

## 🟢 Resolved Gaps (Nov8.5)
*   **Tracking Document Integrity & Activity Rotation Session Lifecycle (ISSUE-NOV8.2-O46, ISSUE-NOV8.5-O50)**: Restored truncated tracking documentation (`issues.md` Resolved Gaps Nov4.32 to Nov7.34, Ideas, Dashboard; `RESOLUTION_ARCHIVE.md` Nov4.32 to Nov7.12; SOT chapters 5.52 to 5.59 and baseline entries Nov7.36, Nov7.6, Nov6.0) guarded by automated unit tests (`TrackingDocumentIntegrityTest`, Chapter 150). Fixed `MonitorSession` Activity rotation lifecycle defect where activity recreation re-started service and reset alarm states, introducing `SessionCommand` state machine (`SessionCommandTest`, Chapter 151). Verified by 302/302 passing unit and instrumented tests, clean build with `--warning-mode all`, device audit 0 Davey events on A15, and reciprocal simulation. SOT rules 1.387, 1.388, test chapters 150, 151. **PASSED.**

---

## 🟢 Resolved Gaps (Nov8.2)
*   **Tracking Documents & Dashboard Synchronization (ISSUE-NOV7.15-O40)**: Synchronized and completed all tracking documentation, baselines, dashboard entries, and resolved gaps across versions Nov7.6 to Nov7.12 in `DOCS/issues.md` and `RESOLUTION_ARCHIVE.md`. SOT rule 1.387, test chapter 150. **PASSED.**

---

## 🟢 Resolved Gaps (Nov8.1)
*   **Comprehensive Test Coverage & Verification for Chapters 67, 68, 83, and 93 (ISSUE-NOV6.0-O23)**: Implemented automated unit tests (`WalPerformanceTest`, `IndexingSpeedTest`, and `SoakSimulationTest`) in `app-monitor` to rigorously verify Write-Ahead-Log (WAL) burst performance (< 10ms per write), scaling and query speed for 50,000+ history trail points (< 2s), main-thread stall watchdog recovery, and programmatic 24h soak simulation stability. Upgraded chapters 67, 68, 83, and 93 from `T` to `A` in `CHAPTER_TRIAGE.md` and `VERIFICATION_MANIFEST.md`. Verified by 100% passing automated unit tests across all subprojects and clean build with `--warning-mode all`. SOT rule 1.386, test chapters 67, 68, 83, 93. **PASSED.**

---

## 🟢 Resolved Gaps (Nov8.0)
*   **Unpaired Tracker Trail Retention (ISSUE-NOV6.2-O30)**: Verified and tested that unpairing a tracker (clearing target ID via `identityManager.clearTargetId()`) does not delete historical trail data stored in Room database (`TrailDao`), preserving historical telemetry and movement points for potential future re-pairing. Automated by `UnpairedTrackerTrailRetentionTest`. SOT rule 1.385, test chapter 149. **PASSED.**

---

## 🟢 Resolved Gaps (Nov7.38)
*   **Local Trail Storage & Pruning (ISSUE-NOV6.2-O29)**: The Monitor application now records and stores its own movement path (`isLocalUser = true`) in the local Room database (`TrailDao`) upon receiving location fixes from `MonitorLocationSource`. `MonitorViewModel` exposes `localTrail` and `MonitorActivity` renders independent polylines for remote tracker trail (Blue) and local viewer trail (Cyan) on the osmdroid map canvas. Additionally, local user trail entries older than 48 hours are automatically pruned by `TrailDao.pruneLocalTrail` and `TrailPruningWorker`. Verified by unit tests (`LocalTrailTest`), 100% passing automated test suite across all subprojects, and clean build with `--warning-mode all`. SOT rule 1.384, test chapter 148. **PASSED.**

---

## 🟢 Resolved Gaps (Nov7.36)
*   **Independent Clean Setup Actions (ISSUE-NOV7.1-O35)**: `CleanSetupOverlay` (`core-ui`) previously passed `requestUnpair` (or empty callbacks in Sensor) to all three of its buttons, so "Clear Home Points & Geofence", "Reset Operational Statistics", and "Trigger Full System Re-Initialization" all executed tracker unpairing. Each action now executes its distinct, independent maintenance logic: "Clear Home Points & Geofence" (`onClearHomePoints`) clears geofence violation alert settings (`distance`, `jammerDetection`, `tiltAlert`, `liftAlert`, `tamperAlert`) and acknowledges active alarms in `MonitorAlarmEngine`; "Reset Operational Statistics" (`onResetStats`) clears stored trail history from Room DB via `MonitorRepository.clearTrail()` (`TrailDao.deleteAll()`), clears latest telemetry in `MonitorViewModel`, acknowledges alarms, and resets service uptime timer in `SensorServiceStatus` / `SensorViewModel`; and "Trigger Full System Re-Initialization" (`onFullInit`) clears target tracker pairing ID, resets `AlertSettings` to default defaults, resets `sirenType` to `"Siren"`, resets `relayUrl` to `RelayConfig.DEFAULT_URL`, clears stored trail history, and resets service status on both applications. Verified by unit tests in `CleanSetupActionsTest` and `SensorServiceStatusTest`, 100% passing automated test suite across all subprojects, build clean with `--warning-mode all`. SOT rule 1.383, test chapter 147. **PASSED.**

---

## 🟢 Resolved Gaps (Nov7.34)
*   **Automatic Sensor Service Startup, Live Uptime Timer & Protected Stop Control (ISSUE-NOV7.2-O36)**: `AdminActivity` now automatically launches `SensorService` on app launch when permissions are verified. `SensorServiceStatus` tracks service start time (`startTimeMillis`) and `SensorViewModel` exposes live uptime formatted as `HH:mm:ss` (`serviceUptimeText`). `AdminActivity` displays a live Service Status card with uptime counter and a protected `STOP SENSOR SERVICE` action button that presents a confirmation dialog (`"Are you sure you want to stop background sensing?"`) before sending `ACTION_STOP_SERVICE` to `SensorService`. `SensorService.onStartCommand` cleanly intercepts `ACTION_STOP_SERVICE` and calls `stopSelf()`. Verified by 100% passing automated unit tests across all subprojects (`SensorServiceStatusTest`), `assembleDebug --warning-mode all` build clean. SOT rule 1.382, test chapter 146. **PASSED.**

---

## 🟢 Resolved Gaps (Nov7.32)
*   **Alarm Reach Under Do Not Disturb & Locked Screen Setup (ISSUE-NOV7.3-O39)**: Both Sensor and Monitor applications now provide dedicated guidance and direct action buttons in their Phone Setup overlays (`PhoneSetupOverlay` in `:core-ui`) to configure Do Not Disturb (Notification Policy Access via `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`) and Full-Screen Intent permissions (`Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`), ensuring critical tracker alarms and sirens successfully bypass Do Not Disturb mode and display over locked screens. Verified by automated unit tests, build clean with `--warning-mode all`. SOT rule 1.381, test chapter 145. **PASSED.**

---

## 🟢 Resolved Gaps (Nov7.30)
*   **Real Telemetry Unknown Values Handling (ISSUE-NOV7.2-O37)**: `SensorService` copied `loc.speed` without checking `hasSpeed()`, reporting `0.0 km/h` / `STATIONARY` when speed was unknown, and initialized battery level (100) and temperature (30°C) with invented defaults before broadcast intents arrived. `SensorService` now checks `loc.hasSpeed()` (reporting `-1f` when absent), initializes `lastBatteryLevel` to `-1` and `lastTempCelsius` to `-1f`, and `MonitorViewModel` treats negative speed/battery and non-positive temperature as unknown (`--` on HUD and safe from false low-battery/temperature alarms). Verified by unit tests including `UnknownTelemetryTest` and `FixReadingTest`, 284/284 JVM unit tests passing across all subprojects, build clean with `--warning-mode all`. SOT rule 1.380, test chapter 144. **PASSED.**

---

## 🟢 Resolved Gaps (Nov7.28)
*   **Sensor Siren Test Stealth Enforcement (ISSUE-NOV7.3-O38)**: The Sensor displayed `TEST SIREN AUDIO` in its Siren & Audio setup overlay, which only toggled an unused `_isSirenPlaying` state flow in `SensorViewModel` and could never sound. Rule 14 (Stealth Authority, R872) forbids the Sensor to sound. `AlarmSoundOverlay` (`core-ui`) now checks `isTracker`, rendering `Disabled (Stealth Tracker)` in place of the test button when `isTracker` is true. `_isSirenPlaying` and `testSirenAudio()` were removed from `SensorViewModel` (`app-sensor`), and `AdminActivity` passes `isSirenPlaying = false` and `{}` for the test action. Verified by unit tests across all subprojects, build clean with `--warning-mode all`, and `SensorStealthGuardTest`. SOT rule 1.379, test chapter 143. **PASSED.**

---

## 🟢 Resolved Gaps (Nov7.26)
*   **Sensor PermissionGuard Re-check On Resume (ISSUE-NOV7.19-O44)**: `PermissionGuard` in `AdminActivity` (`app-sensor`) evaluated permission checks only once upon initial composition. Returning to `AdminActivity` after granting permissions in system Settings or external permission flows left the UI stuck on "Permissions Required" until "Grant Permissions" button was clicked again. `PermissionGuard` now observes `Lifecycle.Event.ON_RESUME` using `LocalLifecycleOwner.current` and `DisposableEffect`, re-evaluating required permissions on activity resume. Extracted `getRequiredSensorPermissions(sdkInt)` and `checkSensorPermissions(context, permissions, checker)` helper functions. Verified by 5 unit tests in `PermissionGuardTest`, 194/194 JVM unit tests passing across all subprojects, `assembleDebug --warning-mode all` build clean. SOT rule 1.378, chapter 142. **PASSED.**

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-NOV7.21-S1**: One Fix Freshness Rule. The Sensor now stops sending a fix after 15 s (`FIX_STALE_AFTER_MS`), while its own HUD keeps showing that fix with a growing age and drops its speed only after 35 s (`SPEED_VALID_MS` in `FixReading.kt`), and the Monitor GPS LED reads only the position. One freshness rule in `core-ui`, used by `SensorService`, the Sensor HUD, and `SensorFixStore`, would keep the Sensor's screen and its telemetry from disagreeing. With it, `ForensicBuffer.writeTrace` could read `update.isCritical` itself and drop its separate `isCritical` parameter. (SIMP-NOV7.19-S1, the explicit critical flag, was implemented in Nov7.21 and is recorded in `RESOLUTION_ARCHIVE.md`.)
*   **SIMP-NOV7.17-S1**: One Sensor Service State. `SensorService` now publishes two `@Singleton` stores, `SensorFixStore` (latest fix, cleared in `onDestroy`) and `SensorServiceStatus` (running flag, cleared in `onDestroy`). A single `SensorServiceState` holding `running` and `fix` would be set and cleared in one place, and the HUD and Setup dialog would read one flow. It would also be the natural home for the `Location` of SIMP-NOV7.2-S1.
*   **SIMP-NOV7.15-S1**: Chunk Backfill In The Signaling Client. The Sensor now owns the 50-row chunk size and the stop-on-failure rule (`answerBackfill`), while the wire format belongs to `SocketioSignalingClient`. A single `sendBackfill(requestId, updates): Boolean` on `SignalingClient` would keep chunking, the completion flag, and the stop rule next to the payload code, and the Sensor would make one call per request.
*   **SIMP-NOV7.3-S1**: One Siren Type Setting. The siren type is stored twice in each app: in `AlertSettings.sirenType` and as a separate `siren_type` preference, which the chips use. Keep only the field in `AlertSettings`, so the alarm engine reads one value.
*   **SIMP-NOV7.3-S2**: Remove The FCM Alarm Path. `MonitorMessagingService` opens `AlarmOverlayActivity` from a push message, but nothing registers a token or sends one. Since Nov7.3 the activity shows only the engine's alarms, so this path does nothing; removing it, and the Firebase messaging dependency if nothing else uses it, would leave one alarm path.
*   **SIMP-NOV7.2-S1**: One Fix Copy In The Sensor. `SensorService` keeps `lastLocation` for telemetry and sampling, and since Nov7.2 also publishes a `FixReading` to `SensorFixStore`. If the store held the `Location` itself, the sensing loop, `refreshSamplingRate`, and the HUD would read one copy, and `lastLocation` could be removed. The same change would be the natural place to fix ISSUE-NOV7.2-O37.
*   **SIMP-NOV7.1-S1**: One Unpair Control. The Monitor menu bar has two controls that do the same thing, the `Settings / Re-pair` gear and the `Disconnect` X, and three `CLEAN` actions that also only unpair (ISSUE-NOV7.1-O35). Keep `Disconnect` as the single unpair control, and either give the gear a real settings screen or remove it. That would also remove the confusing `Settings / Re-pair` label.
*   **SIMP-REVOL39.0-S1**: Integrated Kinetic State Observer. Consolidate acceleration, velocity, and thermal status evaluation into a single unified observer in `core-shared`.
*   **SIMP-REVOL38.3-S1**: Dynamic Buffer Auto-tuner. Consolidate bitrate detection and buffer resizing into an adaptive native manager.
*   **SIMP-NOV6.0-S1**: Lint For `core-shared`. Run Gradle lint on `core-shared`, or build it against `android.jar`. That would catch calls missing from older Android across the whole module and could replace the single-class check in `ForensicBufferApiLevelTest`.
*   **SIMP-NOV7.0-S1**: Single Version Source. `versionName` is repeated in `app-monitor/build.gradle.kts` and `app-sensor/build.gradle.kts`, and their `versionCode` values have drifted apart (39 and 41). One value in `gradle.properties`, read by both build files, would make each release a single edit and keep the two apps from disagreeing.
*   **SIMP-NOV6.4-S1**: Forward Batches As One Event. The relay splits each `location_batch` into one `location_relay` event per update, and writes its cache for each one, although the client already accepts an array. Forwarding the batch whole would cut the events during a reconnect burst, write the cache once per batch (ISSUE-NOV5.1-O10), and let the Monitor store a batch with one `insertAll`.
*   **SIMP-NOV6.3-S1**: Delete `HudViewState`. `core-ui/.../ViewState.kt` holds a second HUD model with its own `isRelayConnected`, and no code uses it. `HudState` is the only model in use, so the file can be removed.
*   **SIMP-NOV6.2-S1**: Drop The Local Trail Column. If ISSUE-NOV6.2-O29 is decided against a local trail, remove `isLocalUser`, `pruneLocalTrail`, and the filter in `getLastSequenceId`, leaving the row cap as the only storage rule.
*   **SIMP-NOV6.1-S1**: Shared Device Status. `isNetworkAvailable` and `isLocationProviderEnabled` are copied in `MonitorViewModel` and `SensorViewModel`. Move them next to `readBattery` in `core-ui`, as was done for the battery code.
*   **SIMP-NOV5.0-S1**: Single Map Path. `OsmMap` in `MapComponents.kt` has no callers, while `MonitorScreen` drives its own `MapView`. Either adopt that stack or delete it, along with its unused models and controller.

---

## 📊 Hardening Progress Dashboard
- **Nov8.8: [SOT Count: 964 (Rules: 464), Open: H:1, M:1, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 319), QA: 80]**
- **Nov8.7: [SOT Count: 963 (Rules: 463), Open: H:1, M:2, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 303), QA: 74]**
- **Nov8.5: [SOT Count: 962 (Rules: 462), Open: H:1, M:2, L:14, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 302), QA: 74]**
- **Nov8.2: [SOT Count: 961 (Rules: 461), Open: H:1, M:1, L:10, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 289), QA: 74]**
- **Nov8.1: [SOT Count: 960 (Rules: 460), Open: H:1, M:1, L:11, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 289), QA: 73]**
- **Nov8.0: [SOT Count: 959 (Rules: 459), Open: H:1, M:2, L:11, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 285), QA: 72]**
