# Resolution Archive

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
