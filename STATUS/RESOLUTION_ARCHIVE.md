# Resolution Archive

*The single home of every resolved issue's full description (Documentation Rule D1); other documents point here by issue ID. The Dashboard History is the last section.*

## ✅ Resolved Issues (Nov8.100-Documentation)
- **Lean documentation workflow (user decision 2026-11-08, the 7 points)**: Each session re-read and re-wrote the same facts in up to 9 places, and `DOCS/issues.md` had grown to 450 lines of mostly history. `DEVELOPER_GUIDELINES.md` now holds Documentation Rules D1 to D7: (D1) one home per fact, this archive, with one-line pointers elsewhere; (D2) `issues.md` is a working file with open items, ideas, and at most 6 dashboard rows, older rows going to the Dashboard History here; (D3) a fixed short Handover template, a commit message of at most 12 lines, and no repetition of the Git block in chat; (D4) git tags as baselines (Nov8.14); (D5) a shorter Completion Sequence, 10 steps instead of 14; (D6) `chat_session.md` as a short milestone log; (D7) targeted reads. `issues.md` rewritten: 450 to about 90 lines; done Work Queue rows, Resolved Gaps Nov4.32 to Nov8.14, and resolved gap texts removed (their text is in tag Nov8.14); 46 old dashboard rows moved here unchanged; Open and Ideas recounted from the lists (H:1, M:3, L:8; 16 ideas, after removing SIMP-NOV7.0-S1, done in Nov8.10, and SIMP-NOV6.2-S1, obsolete since the local trail was built). `TrackingDocumentIntegrityTest` rewritten for the new layout and its limits (no Resolved Gaps in `issues.md`, at most 6 dashboard rows, `issues.md` at most 40,000 characters, `Handover.md` at most 70 lines, every archived version in the SOT baseline and in a dashboard row of either file); version names with a suffix such as `Nov8.100-Documentation` accepted by it, `VersionSourceTest`, and `BaselineIndexTest`. SOT rule 1.396, test chapter 158. **PASSED.**

## ✅ Resolved Issues (Nov8.14)
- **Git tags are the baselines (ISSUE-NOV8.5-O47, SIMP-NOV8.5-S1, Low)**: the hand-made `STATUS/BASELINE/<version>/` copies were partial or missing for many versions. User decision 2026-11-08, option C: the git tag is the baseline (`git show <tag>:<path>`); `STATUS/BASELINE/INDEX.md` lists all 90 versions newest first with the commit of each tag, and notes the versions without a tag (Nov8.7 in Nov8.8, Nov8.1 in Nov8.2, Nov4.37 in Nov4.44, Nov1.5 in Nov1.6), without a copy, or with a partial copy. The old copies are frozen, not deleted (Rule 9). `DEVELOPER_GUIDELINES.md`: Rule 9 and Completion step 10 (add a `pending` row instead of copying), step 5 (`gradle.properties`), and a session-start check that fills `pending` from the tag. `BaselineIndexTest` (5 tests) checks that every recorded version is indexed, rows are unique and newest first, every tag points at the indexed commit (run against git; a deliberate break with the old Nov8.11 hash failed as expected), and that only the newest row may be `pending`, until it is tagged; `core-shared` declares the index and the git tag refs as test inputs. The untraced SOT placeholder for rules 1.1 to 1.334, noted in O47, continues as ISSUE-NOV8.14-O58. SOT rule 1.395, test chapter 157. **PASSED.**
- **Nov8.12 release integrity (ISSUE-NOV8.13-O53, Medium)**: the user ran the Nov8.13 Git block. Verified: Nov8.13 = 981668c, Nov8.12 = 6021425, Nov8.11 = dd4be20, Nov8.10 = e45d8fd, the same on `origin`. **PASSED.**

## ✅ Resolved Issues (Nov8.13)
- **Home Points Geofence, Monitor Defined and Sensor Enforced (ISSUE-NOV8.13-O51, High)**: The Nov8.12 Sensor anchored "home" to its own first fix with a fixed 100 m radius, reset on every service start, and the Monitor's Home Points were never wired (the `ADD`/`DEL` map engine is never called; `homePoints` never filled; OUT markers matched a type no one sends). User decision 2026-11-08: option B, the Sensor enforces, the Monitor only edits and delivers (battery). Added `HomePoint`, `GeofenceConfig` (radius 50 to 500 m, growing revision), and `GEOFENCE_BREACH_STATUS` in `core-shared`; `SentinelLogic.evaluateHomeZone` (any point, accuracy hysteresis, no transition on the first fix after a start or change); `SignalingClient.geofenceFlow` / `sendGeofence` over the relay's existing `settings_update` / `settings_relay` event (`GeofenceWire`; relay unchanged, v6.044); `SensorDataStore.geofence` / `saveGeofence` (newest revision only); `SensorService` self-anchor removed, stored geofence enforced; `MonitorDataStore.geofence` / `editGeofence`; `geofenceDeliveries` in `MonitorSessionService` (each edit and each time the tracker comes online); `MonitorViewModel` add, remove, radius, clear; live map `HOME` edit mode with radius slider, circles, green Home squares with remove confirmation, red OUT dots from breach rows, real Home Point count in Phone Setup; osmdroid's own zoom buttons hidden. Verified by `HomeZoneTest`, `GeofenceWireTest`, `GeofenceSyncTest`, extended `GeofenceBreachAlarmTest`, 249 passing unit tests, and on the A15 through the production relay. SOT rule 1.394, test chapters 155 and 156. **PASSED.**
- **Clear Home Points switched alarms off (ISSUE-NOV8.13-O54, Low)**: "Clear Home Points & Geofence" switched off `distance`, `jammerDetection`, `tiltAlert`, `liftAlert`, and `tamperAlert` and acknowledged alarms, so Home Points added later raised no breach alarm. User decision 2026-11-08: it now removes the points only (`MonitorViewModel.clearHomePoints`). Verified by `CleanSetupActionsTest.clearHomePointsRemovesOnlyThePoints`. Test chapter 147.1. **PASSED.**
- **Version bump restored (part of ISSUE-NOV8.13-O53)**: Nov8.12 left `gradle.properties` at Nov8.11 / 62 because `VersionSourceTest` pinned those values. Now Nov8.13 / 64, and the test checks the format and a minimum code. Test chapter 153.1. The tag and commit part of O53 stays open for the user.

## ✅ Resolved Issues (Nov8.12)
- **Geofencing Implementation (ISSUE-NOV5.2-O11 Group 2)**: Wired spatial geofence transition evaluation into `SensorService` using `SentinelLogic.evaluateGeofenceTransition` against a home anchor point, emitting `ViolationEvent(type = "GEOFENCE_BREACH", ...)` upon boundary exit. Added `AlarmType.GEOFENCE_BREACH` to `MonitorAlarms`, controlled by `AlertSettings.distance` and raised by `MonitorAlarmEngine` to sound the siren and show alarm notifications. Verified by `GeofenceBreachAlarmTest` (Chapter 155) and 100% passing automated unit tests across all subprojects with clean `--warning-mode all` build. SOT rule 1.393, test chapter 155. **PASSED.**

## ✅ Resolved Issues (Nov8.11)
- **Trail Persistence Integrity & Pending Identity Collection (ISSUE-NOV4.3-O48, ISSUE-NOV4.3-O49)**: Updated `TrailEntity` primary key composite `["trackerId", "timestamp", "sequenceId"]` in `MonitorDatabase.kt` to prevent equal-timestamp telemetry replacement overwrite, and refactored `MonitorViewModel` local trail collection flow to filter valid `selfId` first (`selfId.filter { it != "LOADING..." && it.isNotBlank() }`) and `flatMapLatest` into `localFix`, ensuring zero initial local fixes are dropped during cold start identity loading. Verified by `TrailPersistenceIntegrityTest` (Chapter 154) and 100% passing automated test execution across all subprojects with clean `--warning-mode all` build. SOT rule 1.392, test chapter 154. **PASSED.**

## ✅ Resolved Issues (Nov8.10)
- **Single Source Version Management Architecture & Gradle Build Synchronization (SIMP-NOV7.0-S1)**: Centralized application `versionName` ("Nov8.10") and `versionCode` (60) in `gradle.properties` as the single source of truth across all subprojects. Refactored `app-monitor/build.gradle.kts` and `app-sensor/build.gradle.kts` to dynamically pull version properties via `providers.gradleProperty()`, eliminating version code drift and redundant manual build script declarations. Verified by `VersionSourceTest` (Chapter 153) and 100% passing unit tests across all subprojects with clean `--warning-mode all` build. SOT rule 1.391, test chapter 153. **PASSED.**

## ✅ Resolved Issues (Nov8.8)
- **Comprehensive Test Coverage & Verification for Chapters 67, 68, 83, 93, 145, 147, 148, and 149 (ISSUE-NOV6.0-O23)**: Implemented 8 robust, non-placebo automated unit test suites (`WalPerformanceTest`, `IndexingSpeedTest`, `RealWatchdogRecoveryTest`, `SoakSimulationTest`, `AlarmSetupPermissionTest`, `CleanSetupActionsTest`, `LocalTrailTest`, and `UnpairedTrailRetentionTest`) verifying Write-Ahead-Log burst latency (<10ms/write), 50,000-point trail indexing and query speed (<2s), stall watchdog recovery threshold logic, continuous 24h soak sequence continuity, DND & full-screen intent permission intent helpers, independent clean setup maintenance actions, local trail 48h TTL pruning, and unpaired tracker trail retention. Upgraded chapters 67, 68, 83, 93, 145, 147, 148, and 149 from `T` to `A` in `CHAPTER_TRIAGE.md`, raising QA count to 80 of 140 active chapters. Verified by 100% passing automated unit tests across all subprojects and clean build with `--warning-mode all`. SOT rule 1.390, test chapters 67, 68, 83, 93, 145, 147, 148, 149. **PASSED.**

## ✅ Resolved Issues (Nov8.7)
- **Async ForensicBuffer Initialization & Startup Main-Thread Frame Optimization (ISSUE-NOV7.15-O41)**: Moved `ForensicBuffer` disk I/O initialization and header mapping in `SensorService.onCreate()` to `Dispatchers.IO` background coroutine (`initJob`), eliminating main-thread disk I/O stalls during application cold-start and service creation. `startSensingLoop()` and `setupBackfillListener()` safely await `initJob.join()` prior to trace writes or range reads. Verified by 100% passing automated unit test suite across all subprojects, clean build with `--warning-mode all`, and zero main-thread Davey delays on launch. SOT rule 1.389, test chapter 152. **PASSED.**

## ✅ Resolved Issues (Nov8.5)
- **Tracking Document Integrity & Activity Rotation Session Lifecycle (ISSUE-NOV8.2-O46, ISSUE-NOV8.5-O50)**: Restored truncated tracking documentation (`issues.md` Resolved Gaps Nov4.32 to Nov7.34, Ideas, Dashboard; `RESOLUTION_ARCHIVE.md` Nov4.32 to Nov7.12; SOT chapters 5.52 to 5.59 and baseline entries Nov7.36, Nov7.6, Nov6.0) guarded by automated unit tests (`TrackingDocumentIntegrityTest`, Chapter 150). Fixed `MonitorSession` Activity rotation lifecycle defect where activity recreation re-started service and reset alarm states, introducing `SessionCommand` state machine (`SessionCommandTest`, Chapter 151). Verified by 302/302 passing unit and instrumented tests, clean build with `--warning-mode all`, device audit 0 Davey events on A15, and reciprocal simulation. SOT rules 1.387, 1.388, test chapters 150, 151. **PASSED.**

## ✅ Resolved Issues (Nov8.2)
- **Tracking Documents & Dashboard Synchronization (ISSUE-NOV7.15-O40)**: Synchronized and completed all tracking documentation, baselines, dashboard entries, and resolved gaps across versions Nov7.6 to Nov7.12 in `DOCS/issues.md` and `RESOLUTION_ARCHIVE.md`. SOT rule 1.387, test chapter 150. **PASSED.**

## ✅ Resolved Issues (Nov8.1)
- **Comprehensive Test Coverage & Verification for Chapters 67, 68, 83, and 93 (ISSUE-NOV6.0-O23)**: Implemented automated unit tests (`WalPerformanceTest`, `IndexingSpeedTest`, and `SoakSimulationTest`) in `app-monitor` to rigorously verify Write-Ahead-Log (WAL) burst performance (< 10ms per write), scaling and query speed for 50,000+ history trail points (< 2s), main-thread stall watchdog recovery, and programmatic 24h soak simulation stability. Upgraded chapters 67, 68, 83, and 93 from `T` to `A` in `CHAPTER_TRIAGE.md` and `VERIFICATION_MANIFEST.md`. Verified by 100% passing automated unit tests across all subprojects and clean build with `--warning-mode all`. SOT rule 1.386, test chapters 67, 68, 83, 93. **PASSED.**

## ✅ Resolved Issues (Nov8.0)
- **Unpaired Tracker Trail Retention (ISSUE-NOV6.2-O30)**: Verified and tested that unpairing a tracker (clearing target ID via `identityManager.clearTargetId()`) does not delete historical trail data stored in Room database (`TrailDao`), preserving historical telemetry and movement points for potential future re-pairing. Verified by unit tests (`UnpairedTrackerTrailRetentionTest`), 100% passing automated test suite across all subprojects, and clean build with `--warning-mode all`. SOT rule 1.385, test chapter 149. **PASSED.**

## ✅ Resolved Issues (Nov7.38)
- **Local Trail Storage & Pruning (ISSUE-NOV6.2-O29)**: The Monitor application now records and stores its own movement path (`isLocalUser = true`) in the local Room database (`TrailDao`) upon receiving location fixes from `MonitorLocationSource`. `MonitorViewModel` exposes `localTrail` and `MonitorActivity` renders independent polylines for remote tracker trail (Blue) and local viewer trail (Cyan) on the osmdroid map canvas. Additionally, local user trail entries older than 48 hours are automatically pruned by `TrailDao.pruneLocalTrail` and `TrailPruningWorker`. Verified by unit tests (`LocalTrailTest`), 100% passing automated test suite across all subprojects, and clean build with `--warning-mode all`. SOT rule 1.384, test chapter 148. **PASSED.**

## ✅ Resolved Issues (Nov7.36)
- **Independent Clean Setup Actions (ISSUE-NOV7.1-O35)**: `CleanSetupOverlay` (`core-ui`) previously passed `requestUnpair` (or empty callbacks in Sensor) to all three of its buttons, so "Clear Home Points & Geofence", "Reset Operational Statistics", and "Trigger Full System Re-Initialization" all executed tracker unpairing. Each action now executes its distinct, independent maintenance logic: "Clear Home Points & Geofence" (`onClearHomePoints`) clears geofence violation alert settings (`distance`, `jammerDetection`, `tiltAlert`, `liftAlert`, `tamperAlert`) and acknowledges active alarms in `MonitorAlarmEngine`; "Reset Operational Statistics" (`onResetStats`) clears stored trail history from Room DB via `MonitorRepository.clearTrail()` (`TrailDao.deleteAll()`), clears latest telemetry in `MonitorViewModel`, acknowledges alarms, and resets service uptime timer in `SensorServiceStatus` / `SensorViewModel`; and "Trigger Full System Re-Initialization" (`onFullInit`) clears target tracker pairing ID, resets `AlertSettings` to default defaults, resets `sirenType` to `"Siren"`, resets `relayUrl` to `RelayConfig.DEFAULT_URL`, clears stored trail history, and resets service status on both applications. Verified by unit tests in `CleanSetupActionsTest` and `SensorServiceStatusTest`, 100% passing automated test suite across all subprojects, build clean with `--warning-mode all`. SOT rule 1.383, test chapter 147. **PASSED.**

## ✅ Resolved Issues (Nov7.34)
- **Automatic Sensor Service Startup, Live Uptime Timer & Protected Stop Control (ISSUE-NOV7.2-O36)**: `AdminActivity` now automatically launches `SensorService` on app launch when permissions are verified, providing seamless continuous background sensing. `SensorServiceStatus` tracks service start time (`startTimeMillis`) and `SensorViewModel` exposes live uptime formatted as `HH:mm:ss` (`serviceUptimeText`). `AdminActivity` displays a live Service Status card with uptime counter and a protected `STOP SENSOR SERVICE` action button that presents a confirmation dialog (`"Are you sure you want to stop background sensing?"`) before sending `ACTION_STOP_SERVICE` to `SensorService`. `SensorService.onStartCommand` cleanly intercepts `ACTION_STOP_SERVICE` and calls `stopSelf()`. Verified by 100% passing automated unit tests across all subprojects (`SensorServiceStatusTest`), `assembleDebug --warning-mode all` build clean. SOT rule 1.382, test chapter 146. **PASSED.**

## ✅ Resolved Issues (Nov7.32)
- **Alarm Reach Under Do Not Disturb and Over a Locked Screen (ISSUE-NOV7.3-O39)**: Both Sensor and Monitor applications now provide dedicated setup guidance and direct action buttons in their Phone Setup overlays (`PhoneSetupOverlay` in `:core-ui`) to configure Do Not Disturb (Notification Policy Access via `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`) and Full-Screen Intent permissions (`Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`), ensuring critical tracker alarms and sirens successfully bypass Do Not Disturb mode and display over locked screens. Verified by automated unit tests, build clean with `--warning-mode all`. SOT rule 1.381, test chapter 145. **PASSED.**

## ✅ Resolved Issues (Nov7.30)
- **Real Telemetry Unknown Values Handling (ISSUE-NOV7.2-O37)**: `SensorService` copied `loc.speed` without checking `hasSpeed()`, reporting `0.0 km/h` / `STATIONARY` when speed was unknown, and initialized battery level (100) and temperature (30°C) with invented defaults before broadcast intents arrived. `SensorService` now checks `loc.hasSpeed()` (reporting `-1f` when absent), initializes `lastBatteryLevel` to `-1` and `lastTempCelsius` to `-1f`, and `MonitorViewModel` treats negative speed/battery and non-positive temperature as unknown (`--` on HUD and safe from false low-battery/temperature alarms). Verified by unit tests including `UnknownTelemetryTest` and `FixReadingTest`, 284/284 JVM unit tests passing across all subprojects, build clean with `--warning-mode all`. SOT rule 1.380, test chapter 144. **PASSED.**

## ✅ Resolved Issues (Nov7.28)
- **Sensor Siren Test Stealth Enforcement (ISSUE-NOV7.3-O38)**: The Sensor displayed `TEST SIREN AUDIO` in its Siren & Audio setup overlay, which only toggled an unused `_isSirenPlaying` state flow in `SensorViewModel` and could never sound. Rule 14 (Stealth Authority, R872) forbids the Sensor to sound. `AlarmSoundOverlay` (`core-ui`) now checks `isTracker`, rendering `Disabled (Stealth Tracker)` in place of the test button when `isTracker` is true. `_isSirenPlaying` and `testSirenAudio()` were removed from `SensorViewModel` (`app-sensor`), and `AdminActivity` passes `isSirenPlaying = false` and `{}` for the test action. Verified by unit tests across all subprojects, build clean with `--warning-mode all`, and `SensorStealthGuardTest`. SOT rule 1.379. **PASSED.**

## ✅ Resolved Issues (Nov7.26)
- **Sensor PermissionGuard Re-check On Resume (ISSUE-NOV7.19-O44)**: `PermissionGuard` in `AdminActivity` (`app-sensor`) evaluated permission check only once when entering composition, so after a user granted missing permissions in system Settings or external permission flows and returned to `AdminActivity`, the screen remained stuck on "Permissions Required" until "Grant Permissions" button was clicked again. `PermissionGuard` now observes `Lifecycle.Event.ON_RESUME` using `LocalLifecycleOwner.current` and `DisposableEffect`, re-evaluating required permissions upon activity resume. Extracted `getRequiredSensorPermissions(sdkInt)` and `checkSensorPermissions(context, permissions, checker)` helper functions. Verified by 5 unit tests in `PermissionGuardTest`, 194/194 JVM unit tests passing across all subprojects, `assembleDebug --warning-mode all` build clean. SOT rule 1.378, test chapter 142. **PASSED.**

## ✅ Resolved Issues (Nov7.24)
- **Sensor Battery Seeding & Unknown Value Guarding (ISSUE-NOV7.21-O45, ISSUE-NOV7.2-O37)**: Sensor service start on a charging phone raised a false Power disconnected alarm because `SensorService` started with `lastIsCharging = false` before the battery broadcast arrived. `SensorService` now seeds its initial charging state, battery level, and temperature from the sticky `ACTION_BATTERY_CHANGED` broadcast intent returned by `registerReceiver` before the sensing loop starts. Unknown battery level (-1) and temperature (0f) are handled gracefully, and `MonitorAlarms` guards low battery (`batteryLevel >= 0`) and high temperature (`tempCelsius > 0f`) checks against unknown values. Verified by unit tests, 189/189 JVM unit tests passing, `ProductionReadinessAuditTest` 3/3 on A15, device audit 0 Davey, StackLog, or fatal entries. SOT rule 1.377, test chapter 141. **PASSED.**

## ✅ Resolved Issues (Nov7.21)
- **The Sensor Reports A Real GPS Loss (ISSUE-NOV7.19-O43)**: `SensorService.lastLocation` was set by each fix and never aged, so after a real signal loss the sensing loop kept sending the last fix with a fresh timestamp and the Monitor's `Tracker GPS signal loss` could only fire before the Sensor's first fix. Reporting `NO_FIX` needed a transport change first, because `SocketioSignalingClient.sendTelemetry` and `SignalingQueue` treated every status other than `VALID` as critical (unbatched, never dropped, not deduplicated), which would have flooded the relay at 10 Hz. **SIMP-NOV7.19-S1, implemented**: `LocationUpdate.isCritical` (`core-shared/.../Models.kt`, cleared by `reset()`, not put on the wire) now drives all four decisions: the unbatched send in `sendTelemetry` and the deduplication, `pushFront`, and drop-oldest rules in `SignalingQueue`. `SensorService` sets it only on an update carrying a `JUMP` or `OUTLIER` verdict and passes it to `ForensicBuffer.writeTrace`, which commits such an entry at once. **Loss rule**: the new `app-sensor/.../FixFreshness.kt` holds `FIX_STALE_AFTER_MS` (15 s, three times the 5 s location request) and `isFixFresh` on `elapsedRealtimeNanos`; while the Sensor has no fix younger than that, each tick is sent as `NO_FIX` with no position, and the service logs `No current GPS fix ... reporting NO_FIX` and `GPS fix current again` at each change. Verified by 13 new unit tests (`FixFreshnessTest` 5, `SignalingQueueTest` +4, `SignalingBatchTest` +1 with `testCriticalEventFlushesBatch` moved to `isCritical`, `MovementCheckTest` +3), 189 of 189 unit tests, `ProductionReadinessAuditTest` 3 of 3 on the A15, the device audit (0 Davey, StackLog, or fatal entries in both apps), and live on the A15 through the production relay. SOT rule 1.376, test chapter 140. **PASSED.**

## ✅ Resolved Issues (Nov7.19)
- **No False Alarms At Start (ISSUE-NOV7.17-O42)**: every Monitor cold start raised `RELAY_DISCONNECTED` and sounded the siren at full volume, and a start of both apps also raised `GPS_SIGNAL_LOSS` and `GPS_JUMP`. Three root causes, all confirmed in the A15 log of 2026-11-07 19:46: (1) `SocketioSignalingClient.isConnected` starts `false`, and `alarmBooks` raised the relay alarm on that first value, 18 ms after `Connecting to relay`; the relay alarm now holds only after the relay was connected once in this watch, or 30 s (`RELAY_FIRST_CONNECT_GRACE_MS`) after watching starts with no connection. (2) The Sensor sends `(0,0)` until its first fix, and the Monitor raised GPS loss on the first such reading; `GPS_SIGNAL_LOSS` now needs 30 s (`GPS_SIGNAL_LOSS_AFTER_MS`) without a position, tracked as `noGpsSignalSinceMs`. (3) `SensorService` ran `validateMovement` on every loop tick (100 ms while charging) against the previous tick, so the drift between two fixes about 5 s apart was divided by one tick. Verified by 14 new unit tests (`MonitorAlarmsTest` +6, `MovementCheckTest` 6, `SentinelLogicTest` +2), 176 of 176 unit tests, `ProductionReadinessAuditTest` 3 of 3 on the A15. SOT rule 1.375, test chapter 139. **PASSED.**

## ✅ Resolved Issues (Nov7.17)
- **Real Sensor Service Status (ISSUE-NOV6.4-O34)**: the Sensor Setup dialog showed the fixed text `Sensor Service Status: Active / Ready`, also before the service was started, because nothing recorded whether `SensorService` was running. The new `@Singleton` `SensorServiceStatus` (`app-sensor/.../SensorServiceStatus.kt`) holds `isRunning`; `SensorService` sets it at the end of `initializeServiceComponents` and clears it in `onDestroy`, next to `SensorFixStore.clear()`. `SensorViewModel.isServiceRunning` passes it to `SensorAdminConsole`, and the dialog shows `Sensor Service: Running` / `Stopped` (`serviceStatusText`) and `Relay Connection: Connected` / `Disconnected` (`relayStatusText`, from `hudState.isRelayConnected`). SOT rule 1.374, test chapter 138. **PASSED.**

## ✅ Resolved Issues (Nov7.15)
- **Backfill Answer Stops On Send Failure (ISSUE-NOV6.4-O33)**: `SensorService.setupBackfillListener` ignored the result of `sendBackfillResponse`, so after a connection drop between chunks the completion chunk could still reach the Monitor, which then marked the range answered with rows missing until the Monitor process restarted. The chunk loop moved to the new `answerBackfill` in `app-sensor/.../BackfillResponder.kt` (`BACKFILL_CHUNK_SIZE` = 50), which stops at the first failed send and never sends a later chunk or the completion flag for that request. SOT rule 1.373, test chapter 137. **PASSED.**

## ✅ Resolved Issues (Nov7.12)
- **Coarse Network Fix Filtering (ISSUE-NOV6.1-O28)**: Added `isBetterLocation` filter to `MonitorLocationSource.kt` based on standard Android location evaluation. Prevents a coarse Network location fix (e.g., accuracy ±77.6m) from overriding a fresh, more accurate GPS location fix (e.g., accuracy ±11m) on the Monitor UI and map while the GPS fix is less than 2 minutes (120s) old. Verified by unit tests in `MonitorLocationSourceTest` and full automated test suite pass across all modules. **PASSED.**

## ✅ Resolved Issues (Nov7.11)
- **Thread-Safe SignalingQueue & Failed Flush Re-queuing (ISSUE-NOV6.0-O24)**: Made `SignalingQueue` thread-safe by protecting operations with `ReentrantLock` and thread-safe collections. Updated `SocketioSignalingClient.flushQueue` to push failed telemetry batches back to the front of the queue in their original order via `queue.pushFront(batch)`, preventing reordering behind newer updates. Verified by `SignalingQueueTest` and test suite pass across all subprojects. **PASSED.**

## ✅ Resolved Issues (Nov7.10)
- **Screen State & Rotation Preservation (ISSUE-NOV6.0-O22)**: Updated all UI overlay visibility flags, dialog states, and map follow modes in `MonitorActivity.kt` (`MonitorScreen`, `PairingWizard`) and `AdminActivity.kt` (`AdminActivity`, `SettingsOverlay`) from plain `remember` to `rememberSaveable`, ensuring open overlays, dialogs, unlock states, and follow modes persist across screen rotation and configuration changes. Verified by automated tests and test suite pass across all modules. **PASSED.**

## ✅ Resolved Issues (Nov7.9)
- **HUD Flow 2Hz Rate Limiting (ISSUE-NOV6.0-O21)**: Applied `atUiRate()` (500ms sampling window) to `localHudState` and `remoteHudState` in `MonitorViewModel.kt` and to `hudState` in `SensorViewModel.kt`. Throttled incoming telemetry/fix inputs (`uiTelemetry = pairedTelemetry.atUiRate()`, `uiFix = fixStore.fix.atUiRate()`) before combining in `localHudState`, `remoteHudState`, and `hudState`, preventing 10Hz re-evaluations of expensive system service checks (`isNetworkAvailable`, `isLocationProviderEnabled`) and capping UI emissions to 2Hz. Verified by `UiRateTest` and new `HudRateTest` unit tests and full automated test suite pass across all subprojects. **PASSED.**

## ✅ Resolved Issues (Nov7.8)
- **Identity Reopen Test (ISSUE-NOV6.0-O23, partial)**: `IdentityPersistenceTest` gained a real manager re-instantiation test across a simulated app restart; `TESTS/TEST_PROCEDURE.md` and `TESTS/VERIFICATION_MANIFEST.md` were relabelled `Nov7.8`. Chapters 67, 68, 83, and 93 stayed T until Nov8.1. *(Entry reconstructed in Nov8.5 from commit `046387d`; Nov7.8 never wrote one, ISSUE-NOV8.2-O46.)* **PARTIAL.**

## ✅ Resolved Issues (Nov7.7)
- **False MOVING & Sensor-Detected Violations (ISSUE-NOV5.2-O13, group 1 slice 3)**: Fixed false MOVING on the Sensor by wiring `SentinelLogic.validateMovement` with accelerometer motion (`hasPhysicalMotion`) on each fix in `SensorService.kt`, transmitting motion state and violation status (`JUMP`, `OUTLIER`) in telemetry and via `ViolationEvent`. Added `AlarmType.GPS_JUMP` to `MonitorAlarms.kt`, wired to `AlertSettings.jammerDetection` and `signalingClient.violationFlow` in `MonitorAlarmEngine.kt`. Verified by unit tests in `MonitorAlarmsTest` and full automated test suite pass across all subprojects. **PASSED.**

## ✅ Resolved Issues (Nov7.6)
- **30-Second Siren Auto-Stop (ISSUE-NOV7.6-O40)**: Implemented 30-second timer auto-stop for Monitor alarms in `MonitorAlarmEngine.kt`: the siren automatically stops after 30 seconds (`30s`) or when acknowledged by the user, whichever occurs first. When the siren auto-stops via the 30-second timer, the visual alarm and notification remain pending until explicitly acknowledged. Verified by automated tests and test suite pass across all modules. **PASSED.**

## ✅ Resolved Issues (Nov7.5)
- **Communication Alarms (ISSUE-NOV5.2-O11, group 1, slice 2)**: Added communication alarms to `MonitorAlarms.kt` (`AlarmType.RELAY_DISCONNECTED`, `LOCAL_INTERNET_OFFLINE`, `GPS_SIGNAL_LOSS`), switched by `relayConnection`, `localInternet`, and `signalLoss` in `AlertSettings`. Integrated into `alarmConditions` and `alarmBooks`, wired to `signalingClient.isConnected` and `localInternetFlow()` in `MonitorAlarmEngine.kt`. Verified by 18 unit tests in `MonitorAlarmsTest` and full automated test suite pass across all modules. **PASSED.**

## ✅ Resolved Issues (Nov7.3)
- **Monitor Alarms And Siren (ISSUE-NOV5.2-O11, group 1, slice 1)**: User decisions of 2026-11-07: build group 1, alarms and sirens, in slices, starting with the Monitor-side alarms, and sound an alarm until it is acknowledged. Before this, nothing raised an alarm and nothing played a sound. New in `app-monitor`: `MonitorAlarms.kt` (conditions, the `AlarmBook`, and the `alarmBooks` flow), `SirenPlayer.kt`, `MonitorAlarmEngine.kt`, and `AlarmAlertScreen.kt`; `AlarmOverlayActivity` now shows the engine's alarms. Alarms: tracker offline after 35 seconds, power disconnected, battery below 20% (ends at 22%), and temperature above 46°C (ends at 44°C), each on its existing switch and Global Mute. Root cause also fixed: `MonitorSessionService` re-created the alarm channel at low importance, which Android applied (importance 2 on the A15); the channels are now `monitor_alarm` and `monitor_session`, defined once, and the lowered channel is deleted. The Monitor now asks for the notification permission. `SensorStealthGuardTest` keeps the Sensor silent. Verified by 222 of 222 automated tests (`MonitorAlarmsTest` 15, `SirenSoundTest` 5, `SensorStealthGuardTest` 3, `MonitorAlarmDeviceTest` 5); one deliberate break of each of the book and the volume restore made its test fail. On the A15 through the production relay at low volume, every alarm was raised, silenced, and re-armed live. Chapters 4.2, 8, and 27 are now verified; 8.3, 26.2, and 26.3 are retired as superseded. New gaps: ISSUE-NOV7.3-O38 and O39. **PASSED.**

## ✅ Resolved Issues (Nov7.2)
- **Real Sensor HUD Values (ISSUE-NOV6.1-O27)**: The Sensor's own HUD always showed `STATIONARY`, `0.0 km/h`, `A:±5.0m`, and `AGE:0s`, because `SensorViewModel.hudState` hardcoded them while the real fix lived only inside `SensorService`. The service now publishes each fix to a new `@Singleton` `SensorFixStore` and clears it in `onDestroy`, and the HUD reads it through `withLocalReadings`, which moved with `motionState`, `metersPerSecondToKmh`, and a new `Location.toFixReading()` from `app-monitor` to `core-ui/.../FixReading.kt`, so the Sensor HUD and the Monitor local line share one rule. Unknown values show `--`. Verified by 194 of 194 automated tests (`FixReadingTest` 9, replacing `MonitorHudValuesTest`; `SensorHudValuesTest` 5 on the A15); with the hardcoded values put back, 3 of the 5 failed. On the A15 through the production relay: `--` before the service started, then `A:±22.4m` and `1.3 km/h`, matching the fused fix in `dumpsys location` (`hAcc=22.4`, `vel=0.3656` m/s), with an age counting between fixes. The stop step has automated proof only. New gaps: ISSUE-NOV7.2-O36, the service cannot be stopped on the device, and ISSUE-NOV7.2-O37, unknown speed is sent as 0. **PASSED.**

## ✅ Resolved Issues (Nov7.1)
- **Unpair Confirmation (ISSUE-NOV5.2-O15)**: The Monitor `Settings / Re-pair` gear cleared the pairing in one tap. `MonitorScreen` handed the destructive `onReset` to five controls, the gear, the `Disconnect` X, and the three `CLEAN` actions, and none asked first. All five now open one dialog, `Unpair from tracker <id>?`; only its `Unpair` button calls `onReset`, and `Cancel` keeps the pairing. `MonitorMenuBar` names its parameter `onUnpair`. Verified by `UnpairConfirmationTest` (5 tests) on the real `MonitorScreen`; with the menu bar wired straight to `onReset` again, 4 of the 5 failed on the A15, and the `CLEAN` test, whose path was not broken, passed. New gap: ISSUE-NOV7.1-O35, the `CLEAN` actions only unpair. **PASSED.**

## ✅ Resolved Issues (Nov7.0)
- **Real Version Strings (ISSUE-NOV5.0-O6)**: `SensorService` logged `Version: Nov1.7 (Final)` on every start, and `getAppVersionName` fell back to an invented `Nov4.27` in both apps, because versions were written by hand in the code and the function was copied. One `getAppVersionName` in `core-ui/.../AppVersion.kt` now reads the installed package and shows `--` if that fails, and `SensorService` logs the same value. `VersionLiteralGuardTest` fails if any production string holds a release version. Verified by 182 of 182 automated tests; with the old strings back, `noProductionStringHoldsAVersion` named both and `AppVersionTest.anUnknownPackageShowsNoVersion` failed on the A15. On the A15 through the production relay, all four version displays showed `Nov7.0`, and the Sensor logged `onCreate called. Version: Nov7.0`. The `--` case has automated proof only. **PASSED.**

## ✅ Resolved Issues (Nov6.4)
- **Backfill Gap Recovery (ISSUE-NOV6.3-O32, High)**: The Monitor reads gaps from Room DB with `TrailDao.getSequenceGaps`. `BackfillLedger` keeps 10 requests in flight. **PASSED.**
- **Unanswered Backfill Requests (ISSUE-NOV6.2-O31)**: Request released after 30 seconds without a chunk. **PASSED.**
- **Relay v6.044 On Production (ISSUE-NOV5.2-O12)**: Verified on production relay. **PASSED.**

## ✅ Resolved Issues (Nov6.3)
- **Real Relay Status Text (ISSUE-NOV5.0-O5)**: Monitor Setup dialog shows `HudState.relayStatusText`. Verified by `RelayStatusDialogTest`. **PASSED.**

## ✅ Resolved Issues (Nov6.2)
- **Bounded Trail Storage (ISSUE-NOV5.2-O14)**: Each tracker keeps newest 50,000 trail rows. `TrailPruningWorker` scheduled from `MonitorApp`. **PASSED.**

## ✅ Resolved Issues (Nov6.1)
- **Real HUD Values (ISSUE-NOV5.0-O2)**: Monitor local line uses phone's battery and position fix. Unknown values show `--`. **PASSED.**

## ✅ Resolved Issues (Nov6.0)
- **Chapter Retirement Released (ISSUE-NOV5.4-O18)**: Retired chapters in `TESTS/RETIRED_CHAPTERS.md`. **PASSED.**
- **Forensic Entry Integrity (ISSUE-NOV6.0-O20)**: CRC32 checksums and power loss sequence reservation. **PASSED.**
- **Calls Missing On Older Android (ISSUE-NOV6.0-O25)**: JDK calls reviewed against API 26. **PASSED.**
- **Chapter Review (ISSUE-NOV5.4-O17)**: Chapter audit complete. **PASSED.**

## ✅ Resolved Issues (Nov5.2)
- **Telemetry Interval Explained (ISSUE-NOV5.0-O3)**: 100ms charging rate verified. **PASSED.**
- **Sequence Restart (GAP-REVOL71.0-G1)**: Sequence continuity across crashes. **PASSED.**
- **Wake Lock Expiry (GAP-REVOL71.0-G2)**: Watchdog lock renewal. **PASSED.**
- **Unbounded Trail Display (GAP-REVOL71.0-G3)**: 1000-point map display cap. **PASSED.**
- **Relay URL Validation (GAP-REVOL71.0-G4)**: Malformed URL rejection. **PASSED.**
- **Hidden Monitor Menu Actions (GAP-REVOL71.0-G5)**: Visible menu actions. **PASSED.**
- **Clipping At Largest Font (GAP-REVOL71.0-G6)**: 130% font cap. **PASSED.**
- **Backfill Could Not Complete (GAP-REVOL71.0-G7)**: Routable chunk IDs. **PASSED.**
- **Stale Data After Re-Pairing (GAP-REVOL71.0-G8)**: Paired device filtering. **PASSED.**

## ✅ Resolved Issues (Nov5.1)
- **Wrong Default Relay & Undeployed Relay Code (ISSUE-NOV5.0-O1, GAP-REVOL70.0-G1)**: `RelayConfig` centralized URLs. **PASSED.**
- **Final Build Verified Live (ISSUE-NOV5.0-O8, GAP-REVOL70.0-G2)**: Live two-device verification. **PASSED.**

## ✅ Resolved Issues (Nov5.0)
- **Compact Left-To-Right HUD (GAP-REVOL69.0-G1)**: Compact mode in `HudStatusBar`. **PASSED.**
- **Real-Position Map Centering (GAP-REVOL69.0-G2)**: Dynamic location map centering. **PASSED.**
- **Zeroed Telemetry (GAP-REVOL69.0-G3)**: Snapshot telemetry copies. **PASSED.**
- **VWR LED Never Activated (GAP-REVOL69.0-G4)**: Added `viewer_status_relay` listener. **PASSED.**
- **Relay URL Change Ignored (GAP-REVOL69.0-G5)**: Close old socket on URL change. **PASSED.**
- **Trail Drawn To (0,0) (GAP-REVOL69.0-G6)**: Filter zero positions. **PASSED.**
- **Monitor Instrumented Suite Did Not Compile (GAP-REVOL69.0-G7)**: `FakeSignalingClient` updated. **PASSED.**

## ✅ Resolved Issues (Nov4.44)
- **Release Candidate Verification & Reciprocal Virtual Presence Simulation Audit (GAP-REVOL68.0-G1)**: Automated test execution and virtual presence simulation. **PASSED.**

## ✅ Resolved Issues (Nov4.37)
- **Decoupled Peer LED Signaling & Relay-Independent Status Audit (GAP-REVOL67.0-G1)**: Independent `SRV`, `TRK`, and `VWR` connection LEDs. **PASSED.**

## ✅ Resolved Issues (Nov4.36)
- **Dual Device Hardware Deployment & Telemetry Audit (GAP-REVOL66.0-G1)**: Dual device deployment to A15 and S21FE. **PASSED.**

## ✅ Resolved Issues (Nov4.35)
- **Monitor Hardware Deployment & Single-Line Fitting Audit (GAP-REVOL65.0-G1)**: UI line fitting and formatting. **PASSED.**

## ✅ Resolved Issues (Nov4.34)
- **Upper Menu Line Expansion (GAP-REVOL64.0-G1)**: Quick navigation upper menu buttons. **PASSED.**

## ✅ Resolved Issues (Nov4.33)
- **Revived Sound & Setup Options Integration (GAP-REVOL63.0-G1)**: `CleanSetupOverlay` and `PhoneSetupOverlay` integration. **PASSED.**

## ✅ Resolved Issues (Nov4.32)
- **System Tools & Diagnostics Menu Integration (GAP-REVOL62.0-G1)**: Integrated **Tools** menu and diagnostics overlay. **PASSED.**

## 📊 Dashboard History
*Audit lines of versions older than the newest rows kept in `DOCS/issues.md` (Documentation Rule D2), moved unchanged in Nov8.100-Documentation; newest first.*
- **Nov8.10: [SOT Count: 965 (Rules: 465), Open: H:1, M:1, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 320), QA: 81]**
- **Nov8.8: [SOT Count: 964 (Rules: 464), Open: H:1, M:1, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 319), QA: 80]**
- **Nov8.7: [SOT Count: 963 (Rules: 463), Open: H:1, M:2, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 303), QA: 74]**
- **Nov8.5: [SOT Count: 962 (Rules: 462), Open: H:1, M:2, L:14, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 302), QA: 74]**
- **Nov8.2: [SOT Count: 961 (Rules: 461), Open: H:1, M:1, L:10, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 289), QA: 74]**
- **Nov8.1: [SOT Count: 960 (Rules: 460), Open: H:1, M:1, L:11, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 289), QA: 73]**
- **Nov8.0: [SOT Count: 959 (Rules: 459), Open: H:1, M:2, L:11, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 285), QA: 72]**
- **Nov7.38: [SOT Count: 958 (Rules: 458), Open: H:1, M:2, L:12, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 286), QA: 71]**
- **Nov7.36: [SOT Count: 957 (Rules: 457), Open: H:1, M:2, L:12, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 284), QA: 70]**
- **Nov7.34: [SOT Count: 956 (Rules: 456), Open: H:1, M:2, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 284), QA: 69]**
- **Nov7.32: [SOT Count: 955 (Rules: 455), Open: H:1, M:2, L:13, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 284), QA: 69]**
- **Nov7.30: [SOT Count: 955 (Rules: 455), Open: H:1, M:2, L:14, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 284), QA: 69]**
- **Nov7.28: [SOT Count: 954 (Rules: 454), Open: H:1, M:2, L:14, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 194), QA: 69]**
- **Nov7.26: [SOT Count: 953 (Rules: 453), Open: H:1, M:2, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 194), QA: 68]**
- **Nov7.24: [SOT Count: 952 (Rules: 452), Open: H:1, M:3, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 192), QA: 67]**
- **Nov7.21: [SOT Count: 951 (Rules: 451), Open: H:1, M:4, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 192), QA: 66]**
- **Nov7.19: [SOT Count: 950 (Rules: 450), Open: H:1, M:4, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 179), QA: 65]**
- **Nov7.17: [SOT Count: 949 (Rules: 449), Open: H:1, M:4, L:14, Ideas: H:0, M:3, L:12, Testing: PASSED (Sub-items: 170), QA: 64]**
- **Nov7.15: [SOT Count: 948 (Rules: 448), Open: H:1, M:3, L:15, Ideas: H:0, M:3, L:11, Testing: PASSED (Sub-items: 160), QA: 63]**
- **Nov7.7 to Nov7.12: no audit figures were recorded.**
- **Nov7.6: [SOT Count: 942 (Rules: 442), Open: H:1, M:4, L:18, Ideas: H:0, M:3, L:18, Testing: PASSED (Sub-items: 222), QA: 56]**
- **Nov7.5: [SOT Count: 941 (Rules: 441), Open: H:1, M:4, L:18, Ideas: H:0, M:3, L:18, Testing: PASSED (Sub-items: 222), QA: 55]**
- **Nov7.3: [SOT Count: 940 (Rules: 440), Open: H:1, M:4, L:18, Ideas: H:0, M:3, L:18, Testing: PASSED (Sub-items: 222), QA: 54]**
- **Nov7.2: [SOT Count: 939 (Rules: 439), Open: H:1, M:4, L:16, Ideas: H:0, M:3, L:16, Testing: PASSED (Sub-items: 194), QA: 51]**
- **Nov7.1: [SOT Count: 938 (Rules: 438), Open: H:1, M:5, L:14, Ideas: H:0, M:3, L:15, Testing: PASSED (Sub-items: 187), QA: 50]**
- **Nov7.0: [SOT Count: 937 (Rules: 437), Open: H:1, M:5, L:14, Ideas: H:0, M:3, L:14, Testing: PASSED (Sub-items: 182), QA: 49]**
- **Nov6.4: [SOT Count: 936 (Rules: 436), Open: H:1, M:5, L:15, Ideas: H:0, M:3, L:13, Testing: PASSED (Sub-items: 177), QA: 48]**
- **Nov6.3: [SOT Count: 935 (Rules: 435), Open: H:1, M:6, L:14, Ideas: H:0, M:3, L:12, Testing: PASSED (Sub-items: 156), QA: 47]**
- **Nov6.2: [SOT Count: 934 (Rules: 434), Open: H:1, M:6, L:15, Ideas: H:0, M:3, L:11, Testing: PASSED (Sub-items: 153), QA: 46]**
- **Nov6.1: [SOT Count: 933 (Rules: 433), Open: H:1, M:7, L:12, Ideas: H:0, M:3, L:10, Testing: PASSED (Sub-items: 145), QA: 44]**
- **Nov6.0: [SOT Count: 932 (Rules: 432), Open: H:1, M:7, L:11, Ideas: H:0, M:3, L:9, Testing: PASSED (Sub-items: 130), QA: 43]**
- **Nov5.2: [SOT Count: 931 (Rules: 431), Open: H:1, M:5, L:7, Ideas: H:0, M:3, L:8, Testing: PASSED (Sub-items: 118), QA: 48]**
- **Nov5.1: [SOT Count: 929 (Rules: 429), Open: H:0, M:3, L:5, Ideas: H:0, M:3, L:8, Testing: PASSED (Sub-items: 103), QA: 929]**
- **Nov5.0: [SOT Count: 928 (Rules: 428), Open: H:1, M:4, L:3, Ideas: H:0, M:3, L:8, Testing: PASSED (Sub-items: 100), QA: 928]**
- **Nov4.44: [SOT Count: 924 (Rules: 424), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Release Candidate & Reciprocal Simulation Audit), QA: 924]**
- **Nov4.37: [SOT Count: 924 (Rules: 424), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Decoupled Peer LED Signaling Audit), QA: 924]**
- **Nov4.36: [SOT Count: 923 (Rules: 423), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Dual Device Hardware Deployment & Telemetry Audit), QA: 923]**
- **Nov4.35: [SOT Count: 922 (Rules: 422), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Monitor Hardware Deployment & Single-Line Fitting Audit), QA: 922]**
- **Nov4.34: [SOT Count: 921 (Rules: 421), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Upper Menu Line Expansion), QA: 921]**
- **Nov4.33: [SOT Count: 920 (Rules: 420), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Revived Sound & Setup Options Integration), QA: 920]**
- **Nov4.32: [SOT Count: 919 (Rules: 419), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (System Tools & Diagnostics Menu Integration), QA: 919]**
- **Nov4.31: [SOT Count: 918 (Rules: 418), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Device-Specific Setup & Hardware Hardening Revival), QA: 918]**
- **Nov4.30: [SOT Count: 917 (Rules: 417), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Monitor Dual HUD, Centering & Alarming Setup Revival), QA: 917]**
- **Nov4.29: [SOT Count: 915 (Rules: 415), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Nov4.29 Stabilization & Comprehensive Test Execution Across All Subprojects), QA: 915]**
- **Nov4.28: [SOT Count: 914 (Rules: 414), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Pre-Revolution Map Functionality & Specifications Implementation & Test Chapters 107-108), QA: 914]**
- **Nov4.27: [SOT Count: 913 (Rules: 413), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Relay URL Setup Option Integration in Sensor App & Reactive Signaling Reconnect), QA: 913]**
- **Nov4.26: [SOT Count: 912 (Rules: 412), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Monitor UI Refinement, Upper Menu, Setup Button, SRV/GPS LED Fixes & Map Controls), QA: 912]**
