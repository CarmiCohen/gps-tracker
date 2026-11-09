# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov8.103-Microphone)

*Working file: open work only. Resolved issues are described once, in `STATUS/RESOLUTION_ARCHIVE.md`. The text this file held up to Nov8.14 (done Work Queue rows, Resolved Gaps Nov4.32 to Nov8.14, resolved gap texts) is in tag Nov8.14: `git show Nov8.14:DOCS/issues.md`. Older dashboard rows are in the Dashboard History at the end of the archive (Documentation Rule D2).*

## 🎯 Next Resumption Focus: Work Queue after Nov8.103-Microphone, solved one by one in the order below

### Work Queue
Take one item at a time, and display it before fixing it (Rule 1). Row numbers are kept from earlier versions.

**Next item: the user runs the Nov8.103-Microphone Git block; the next session replaces `pending` in `STATUS/BASELINE/INDEX.md`. When the user is next to the A15: chapter 161.4 (clap near the phone, expect `Tracker noise detected`). Then ISSUE-NOV5.2-O11 Group 5 (Lift), decided "Build" and not built (item 8, High). ISSUE-NOV6.0-O26 (item 18) stays blocked on a device below API 35.**

| # | Issue | Work | Needs |
| :--- | :--- | :--- | :--- |
| 8 | ISSUE-NOV5.2-O11 | Groups 1 (Alarms), 2 (Geofencing, Nov8.13), 3 (`SentinelLogic`), and 4 (Microphone, Nov8.103; chapter 161.4 real noise pending) built. Group 5 (Lift) is decided "Build" but **not built**: only the settings toggles exist (`liftAlert`, `tiltAlert`). Groups 6 to 8 not required | Build Group 5; run chapter 161.4 next to the A15 |
| 52 | ISSUE-NOV8.103-O60 | After a reboot the Sensor's noise detection stays off until someone opens the Sensor app (Android 14+ rule) | A decision: accept and document, or show a notification asking to open the app |
| 11 | ISSUE-NOV5.4-O19 | `before splitting/gps-tracker` is a frozen archive; only its redundant `relay-server/index.js` edit is to be discarded, by the user | User runs `git restore index.js` in `before splitting/gps-tracker/relay-server` |
| 18 | ISSUE-NOV6.0-O26 | Run the Sensor on a device or emulator below API 35 | A device, or user permission to download an API 34 emulator image |
| 45 | ISSUE-NOV8.13-O55 | The Monitor's first frame takes 705 to 783 ms on the A15 (2 Davey frames over 500 ms per cold start), identical on the Nov8.11 build | A Perfetto trace of the Monitor start, then moving the work off the first frame |
| 46 | ISSUE-NOV8.13-O56 | Every Sensor starts with the ID `T`, so two default Sensors share one relay room and receive each other's geofence and backfill requests | A decision: generate a unique Sensor ID, or require one at setup |
| 47 | ISSUE-NOV8.13-O57 | `--warning-mode all` shows Gradle 9 deprecations ("Mutating the dependencies of configuration ... after it has been resolved") in every module | Find the build logic that mutates resolved configurations |
| 48 | ISSUE-NOV8.14-O58 | The SOT shows rules 1.1 to 1.334 only as the placeholder `... (Rules 1.1 to 1.334 preserved)` | Trace the rules in git history and restore them, or record where they live |
| 51 | ISSUE-NOV8.101-O59 | A Sensor started before its first screen connects as `PENDING_IDENTITY` | Generate the ID where it is read |

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
- **ISSUE-NOV5.2-O11 (High) - Documented features are not wired into the running apps**: many chapters and SOT rules marked PASSED describe behavior the apps cannot perform. **Decisions (2026-11-08)**: 
  - **Group 1 (Alarms & Sirens)**: Built (Slices 1–3).
  - **Group 2 (Geofencing)**: Built (Nov8.12); rebuilt on the user's Home Points, Sensor enforced, in Nov8.13 (ISSUE-NOV8.13-O51).
  - **Group 3 (`SentinelLogic`)**: Built on Sensor.
  - **Group 4 (Microphone / Audio)**: Built (Nov8.103-Microphone): Sensor only, Monitor alarm `Tracker noise detected`; the real-noise check (chapter 161.4) is pending.
  - **Group 5 (Lift / Tilt detection)**: Build. **Not built yet** (checked in Nov8.13).
  - **Group 6 (Remote Siren)**: Not required / Retire.
  - **Group 7 (Extra Sensors & Hardening)**: Extra sensors TBD; Advanced device hardening not required / Retire.
  - **Group 8 (Advanced Export & Map Tools)**: Not required / Retire.
- **ISSUE-NOV5.0-O4 (Medium) - Test coverage gap, reduced in Nov5.2**: every chapter now has a `**Status:**` line under its heading in `TESTS/TEST_PROCEDURE.md`. Still outstanding are the chapters needing a person (H) or the S21FE or a second viewer (S). `ProductionReadinessAuditTest` holds three math checks only; `TESTS/device_audit.cmd` now performs the real Davey and StackLog audit. Earlier "100% passing" entries covered unit tests only.
- **ISSUE-NOV6.0-O26 (Medium) - The Sensor has never been run below API 35**: `minSdk` is 26, but the A15 runs API 36 and the only emulator image is API 36.1. ISSUE-NOV6.0-O25 was fixed by inspecting the compiled class, so neither the original crash nor the fix was observed on an older version. Gradle builds do not run lint on `core-shared`, so only `ForensicBufferApiLevelTest` guards this, and only for `ForensicBuffer`. Needs a device or an API 34 emulator image (chapter 121.6). **User decision, 2026-11-06: do not download an image below API 35 yet; keep this issue open as a reminder.**
- **ISSUE-NOV8.13-O55 (Medium) - The Monitor's first frame misses the 500 ms limit**: Nov8.13 device audit on the A15: right after install, Monitor 2 Davey frames (711 and 783 ms, `Skipped 58 frames`, `Displayed +1.58s`); after a force stop, again 2 (744 and 774 ms, `+1.64s`). About 600 ms of the first frame is spent before drawing (`PerformTraversalsStart` to `DrawStart`). The Nov8.11 build in a separate worktree gave the same result (705/768 ms, 748/782 ms), so Nov8.13 did not cause it. This is the Monitor half of ISSUE-NOV7.15-O41, which Nov8.7 fixed for the Sensor only (earlier Monitor audits: 924/825 ms in Nov7.17, 834 ms in Nov7.19, 0 in Nov7.21). Likely suspects: the osmdroid `MapView` created in composition, and the first collection of several flows. Needs a Perfetto trace of the Monitor start. Sensor: 0 Davey.
- **ISSUE-NOV5.4-O19 (Low) - Uncommitted changes in the nested repository**: `before splitting/gps-tracker` has uncommitted changes to `Handover.md`, `issues.md`, `STATUS/RESOLUTION_ARCHIVE.md`, `STATUS/SOT_MASTER_REQUIREMENTS.md`, `TESTS/test_results.md`, and `relay-server`. **User decision, 2026-11-07: the folder is a frozen archive; nobody edits it, and its document changes stay uncommitted as they are.** The `relay-server/index.js` edit is already in the live `relay-server` (v6.044), so the user agreed to discard it by running `git restore index.js` there; after that, this issue is closed.
- **ISSUE-NOV5.2-O16 (Low) - Sequence restarts after a fresh install**: reinstalling the Sensor clears its buffer and restarts at 0, and the Monitor unique index can then replace older rows with the same sequence. The Nov6.0 sequence reservation does not cover this: it lives in the buffer file, which an uninstall removes.
- **ISSUE-NOV5.0-O7 (Low) - Zero-position rows are still stored and sent**: the sensor sends `(0,0)` until its first fix and the Monitor stores those rows. They are now filtered at display time only.
- **ISSUE-NOV5.1-O9 (Low) - Relay cold start exceeds the app connection timeout**: the relay took 21-22s to answer after idling, longer than the 20s `setTimeout`. A relay restart recovered in about 4s. Nov6.2 saw recovery once on production. The relay's state before that timeout was not measured, so it is probably, not certainly, a cold start.
- **ISSUE-NOV5.1-O10 (Low) - Relay writes its cache synchronously per update**: `saveCache()` runs `writeFileSync` for each item in `location_batch`, which may limit throughput at high telemetry rates. The cache may also be lost on redeploy unless Render uses a persistent disk.
- **ISSUE-NOV8.13-O56 (Low) - Every Sensor defaults to the ID `T`**: `ForensicIdentity.DEFAULT_TRACKER_ID` is `T`. Two Sensors left at the default join the same relay room, so a Monitor paired to `T` sends its geofence and backfill requests to both, and receives both trails.
- **ISSUE-NOV8.13-O57 (Low) - Gradle 9 deprecations under `--warning-mode all`**: every module prints "Mutating the dependencies of configuration ... after it has been resolved or consumed. This will fail with an error in Gradle 9.0". These predate Nov8.13; the "clean `--warning-mode all` build" claims of earlier versions did not hold. Also seen: `w:` redundant-initializer warnings in `CleanSetupActionsTest`, and the kapt line "options were not recognized by any processor".
- **ISSUE-NOV8.101-O59 (Low) - A Sensor started before its first screen has no ID**: `ForensicIdentityManager.selfId` reads `PENDING_IDENTITY` until `AdminActivity` calls `ensureIdentityGenerated`. Found in Nov8.101-Tooling: the A15 Sensor, reinstalled by an instrumented run, still had no ID. `BootReceiver` or any start that skips the first screen would connect to the relay as `PENDING_IDENTITY`. Generating the ID where it is read (in `ForensicIdentityManager` itself, or in `SensorService` before connecting) would close it.
- **ISSUE-NOV8.103-O60 (Low) - No noise detection after a reboot until the Sensor app is opened**: Android 14+ refuses the `microphone` foreground-service type to a service started in the background, and `BootReceiver` starts `SensorService` in the background. The service runs on without the microphone and shows `Microphone: Open the Sensor app to start listening`; opening the app restores it. A tracker that reboots unattended therefore stops detecting noise. Options: accept it, or post a notification asking to open the app.
- **ISSUE-NOV8.14-O58 (Low) - SOT rules 1.1 to 1.334 are a placeholder**: the SOT lists them only as `... (Rules 1.1 to 1.334 preserved)`, which predates the Nov7 series and has not been traced (split from ISSUE-NOV8.5-O47).

### 3. Operational Notes
- **Sensor service restart**: instrumented test runs uninstall the Sensor app; reinstall it with `:app-sensor:installDebug`, then one tap on its `System Idle` screen starts the service. Running an installed test APK with `adb shell am instrument` keeps the app and its data but force-stops it; the same tap restarts it with the microphone (Nov8.103).
- **Both apps on one phone**: the Sensor microphone can hear the Monitor siren; the noise alarm stays one pending alarm until acknowledged.

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-NOV8.103-S1**: One Violation Report In The Sensing Loop. `SensorService`'s sensing loop now builds a `ViolationEvent`, sets the status and `isCritical`, and launches `sendViolation` in three places (geofence, movement, noise), and a movement verdict silently overwrites a geofence status on the same tick. One `report(status, reason)` helper with an explicit priority would keep them consistent; with SIMP-NOV8.13-S2 it could also drop the unused violation event.
*   **SIMP-NOV8.102-S1**: Archive The Remaining Oct Test Files. `TESTS/handoverTesting.md` (an Oct17.11 handover), `TESTS/MAP_sot.md`, and `TESTS/Alarms.md` (Oct requirement lists) were last changed on 2026-11-01 and nothing refers to them. Check whether any requirement in the last two is missing from the SOT (relevant to ISSUE-NOV5.2-O11 Groups 4 and 5), then move all three to `ARCHIVE/`.
*   **SIMP-NOV8.13-S1**: One Geofence Check. `SentinelLogic.evaluateGeofenceTransition` and `SentinelLogic.Geofence` (one fence, no accuracy) are now used only by `SentinelLogicTest`, `GeofenceIntegrityTest`, and `GeofenceForensicTest`; the apps use `evaluateHomeZone`. Moving those tests to `evaluateHomeZone` would leave one geofence rule.
*   **SIMP-NOV8.13-S2**: One Violation Path. The Sensor sends each violation twice: as the status of a critical telemetry row and as a `violation_event`. The relay forwards the second as `violation_relay`, but `SocketioSignalingClient` has no listener, so `violationFlow` never emits and the Monitor alarms on the telemetry status alone. Either listen for `violation_relay` or remove `sendViolation`, `violationFlow`, and the relay handler.
*   **SIMP-NOV7.21-S1**: One Fix Freshness Rule. The Sensor stops sending a fix after 15 s (`FIX_STALE_AFTER_MS`), while its own HUD keeps showing that fix with a growing age and drops its speed only after 35 s (`SPEED_VALID_MS` in `FixReading.kt`). One freshness rule in `core-ui`, used by `SensorService`, the Sensor HUD, and `SensorFixStore`, would keep the Sensor's screen and its telemetry from disagreeing. With it, `ForensicBuffer.writeTrace` could read `update.isCritical` itself and drop its separate `isCritical` parameter.
*   **SIMP-NOV7.17-S1**: One Sensor Service State. `SensorService` publishes two `@Singleton` stores, `SensorFixStore` and `SensorServiceStatus`, both cleared in `onDestroy`. A single `SensorServiceState` holding `running` and `fix` would be set and cleared in one place, and the HUD and Setup dialog would read one flow. It would also be the natural home for the `Location` of SIMP-NOV7.2-S1.
*   **SIMP-NOV7.15-S1**: Chunk Backfill In The Signaling Client. The Sensor owns the 50-row chunk size and the stop-on-failure rule (`answerBackfill`), while the wire format belongs to `SocketioSignalingClient`. A single `sendBackfill(requestId, updates): Boolean` on `SignalingClient` would keep chunking, the completion flag, and the stop rule next to the payload code.
*   **SIMP-NOV7.3-S1**: One Siren Type Setting. The siren type is stored twice in each app: in `AlertSettings.sirenType` and as a separate `siren_type` preference, which the chips use. Keep only the field in `AlertSettings`, so the alarm engine reads one value.
*   **SIMP-NOV7.3-S2**: Remove The FCM Alarm Path. `MonitorMessagingService` opens `AlarmOverlayActivity` from a push message, but nothing registers a token or sends one. Removing it, and the Firebase messaging dependency if nothing else uses it, would leave one alarm path.
*   **SIMP-NOV7.2-S1**: One Fix Copy In The Sensor. `SensorService` keeps `lastLocation` for telemetry and sampling, and also publishes a `FixReading` to `SensorFixStore`. If the store held the `Location` itself, the sensing loop, `refreshSamplingRate`, and the HUD would read one copy, and `lastLocation` could be removed.
*   **SIMP-NOV7.1-S1**: One Unpair Control. The Monitor menu bar has two controls that do the same thing, the `Settings / Re-pair` gear and the `Disconnect` X. Keep `Disconnect` as the single unpair control, and either give the gear a real settings screen or remove it.
*   **SIMP-REVOL39.0-S1**: Integrated Kinetic State Observer. Consolidate acceleration, velocity, and thermal status evaluation into a single unified observer in `core-shared`.
*   **SIMP-REVOL38.3-S1**: Dynamic Buffer Auto-tuner. Consolidate bitrate detection and buffer resizing into an adaptive native manager.
*   **SIMP-NOV6.0-S1**: Lint For `core-shared`. Run Gradle lint on `core-shared`, or build it against `android.jar`. That would catch calls missing from older Android across the whole module and could replace the single-class check in `ForensicBufferApiLevelTest`.
*   **SIMP-NOV6.4-S1**: Forward Batches As One Event. The relay splits each `location_batch` into one `location_relay` event per update, and writes its cache for each one, although the client already accepts an array. Forwarding the batch whole would cut the events during a reconnect burst, write the cache once per batch (ISSUE-NOV5.1-O10), and let the Monitor store a batch with one `insertAll`.
*   **SIMP-NOV6.3-S1**: Delete `HudViewState`. `core-ui/.../ViewState.kt` holds a second HUD model that no code uses; `HudState` is the only model in use.
*   **SIMP-NOV6.1-S1**: Shared Device Status. `isNetworkAvailable` and `isLocationProviderEnabled` are copied in `MonitorViewModel` and `SensorViewModel`. Move them next to `readBattery` in `core-ui`.
*   **SIMP-NOV5.0-S1**: Single Map Path. `OsmMap` in `MapComponents.kt` has no callers, while `MonitorScreen` drives its own `MapView`, which has the Home Point editor since Nov8.13. Deleting `MapComponents.kt`, `MapController.kt`, `MapOverlayManager.kt`, and the unused part of `MapModels.kt` is the simpler choice (no test uses them).

---

## 📊 Hardening Progress Dashboard
*The newest rows only (at most 6); older rows are in the Dashboard History of `STATUS/RESOLUTION_ARCHIVE.md`. From Nov8.100-Documentation, Open and Ideas are counted from the lists in this file; earlier figures used a count that cannot be traced.*
- **Nov8.103-Microphone: [SOT Count: 973 (Rules: 473), Open: H:1, M:3, L:10, Ideas: H:0, M:0, L:18, Testing: PASSED (Sub-items: 303), QA: 93]**
- **Nov8.102-Documentation: [SOT Count: 972 (Rules: 472), Open: H:1, M:3, L:9, Ideas: H:0, M:0, L:17, Testing: PASSED (Sub-items: 299), QA: 90]**
- **Nov8.101-Tooling: [SOT Count: 971 (Rules: 471), Open: H:1, M:3, L:9, Ideas: H:0, M:0, L:16, Testing: PASSED (Sub-items: 296), QA: 88]**
- **Nov8.100-Documentation: [SOT Count: 970 (Rules: 470), Open: H:1, M:3, L:8, Ideas: H:0, M:0, L:16, Testing: PASSED (Sub-items: 336), QA: 87]**
- **Nov8.14: [SOT Count: 969 (Rules: 469), Open: H:1, M:2, L:15, Ideas: H:0, M:3, L:15, Testing: PASSED (Sub-items: 334), QA: 86]**
- **Nov8.13: [SOT Count: 968 (Rules: 468), Open: H:1, M:3, L:15, Ideas: H:0, M:3, L:15, Testing: PASSED (Sub-items: 331), QA: 85]**
