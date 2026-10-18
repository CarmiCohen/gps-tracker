# Test Procedure - GPS Tracker (vOct18.22)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

---

## Chapter 0 - Operational Forensic Audit (vOct18.22)
**Goal:** Verify system health via runtime telemetry and log signatures during deployment.

*   **0.1 Logcat Anomaly Monitoring:** 
    *   **Action:** Deploy the app and filter Logcat for `StackLog`, `Davey`, or `Exception`.
    *   **Verification:** Confirm zero `StackLog` leaks during steady-state operation. Monitor for `Davey` events > 500ms during 100Hz sensor bursts.
*   **0.2 Event Log Continuity:** 
    *   **Action:** Observe "Stay-alive pulse" and "PRUNING" signatures in Logcat.
    *   **Verification:** Confirm pulses trigger every 30s. Confirm maintenance tasks execute without blocking the main thread.

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
*   **1.3 Version Verification (Issue #OCT15.6-T1):**
    *   **Action:** Observe the version string displayed at the bottom of the Landing Screen or in Settings.
    *   **Verification:** Confirm the string matches the expected release version (e.g., `Oct18.22_PLATINUM`).
*   **1.4 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions.
*   **1.5 Landing Page Stability:** 
    *   Stay on the landing page for 15 minutes.
*   **1.6 Role Selection UX Branding (Issue #1177):**
    *   **Action:** Launch app with no remote peer active.
    *   **Verification:** Confirm "VIEWER MODE" card is dimmed (`Slate500`). Activate a remote tracker and verify card color transitions to `ViewerCyan` automatically.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button.
*   **2.2 Exercise Setup Options:**
    *   Verify System Diagnostics (Red/Green state update).
*   **2.3 Sensor Calibration:**
    *   Adjust sensitivity sliders for Vibration/Tilt.
*   **2.4 Geofence & Home Point Management (Issue #1179):**
    *   **Action:** Enter geofence addition/removal mode and execute high-frequency rapid updates/taps to add and remove multiple home points.
    *   **Verification:** Verify that `geofenceMode` persists (does not reset to `IDLE`) during batch operations, enabling friction-less batch updates. Confirm `isFenceVisible` is forced to `true` upon entry for immediate visual feedback. Ensure atomic DataStore mutations prevent any list mutation corruption or race conditions.
*   **2.5 DataStore Mutation Extension (Issue #1180):**
    *   **Action:** Invoke consecutive save operations for configuration values, metrics, and home points.
    *   **Verification:** Confirm all updates compile correctly under the unified `mutate` extension and resolve sequentially without data race visibility or state inconsistency under high load.
*   **2.6 UseCase Functional Consolidation (Issue #1181):**
    *   **Action:** Invoke map overlays, geofence mutations, and spatial events simultaneously under rapid screen interactions.
    *   **Verification:** Verify that all calls flow flawlessly into `SpatialLogicUseCase` without any race conditions, data inconsistencies, or behavioral regression compared to old standalone models.
*   **2.7 Connection Sticky-State (Issue #1422):**
    *   **Action:** Change Relay URL or Device IDs while the app is actively connected (SRV green).
    *   **Verification:** Confirm the app immediately disconnects and attempts a new connection with the updated parameters. Verify `CommunicationManager` does not "stick" to the previous configuration.

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy and sentinel logic.

*   **3.1 Main Screen Completeness:**
    *   Verify HUD elements and Stationary status.
*   **3.2 Physical Sentinel & Stealth Audit (Issue #1391):**
    *   **Action:** Trigger sentinel violations (Vibration, Tilt, Geofence).
    *   **Verification:** **ABSOLUTE STEALTH (R872)**. Verify the Tracker device remains audible silent and visually dark (no Red Screen, no Notification sound, no Siren). Confirm event is logged internally but suppressed from local UI/Audio.
*   **3.3 Service Persistence:**
    *   Swipe app away; verify foreground notification persists.
*   **3.4 Status Row Presentation (Issue #1176, #1178):**
    *   **1176 SI Unit Suffix:** Verify temperature displays as `X°` (e.g., `35°`).
    *   **1178 GNSS Initialization:** Verify satellite counts show `--/--` before hardware initialization, then transition to numeric values.
*   **3.5 Tracker Map Stability (Issue #1383):**
    *   **Action:** Tap zoom buttons and perform pinch gestures on the map.
    *   **Verification:** Verify map does not enter "Autonomous Zoom-In" loop. Confirm zoom-out functions correctly.
*   **3.6 HUD Velocity Logic (Issue #1386):**
    *   **Action:** Observe HUD state while stationary.
    *   **Verification:** Verify "MOVING" status is not displayed when speed is 0.0 km/h.
*   **3.7 Dashboard Continuity (Issue #1414):**
    *   **Action:** Run Tracker mode without any connected Viewers.
    *   **Verification:** Confirm behavioral state (MOVING/PARKING) is displayed on the local dashboard. Bypasses peer connectivity gate for local-only diagnostic sessions.
*   **3.8 StatusBar Visual & Logic Hardening (Issue #1423):**
    *   **Action:** Switch to Portrait mode and observe the StatusBar telemetry rows.
    *   **Verification:** Confirm that "Viewer" and "Tracker" sub-rows are stacked vertically with clear spacing. Confirm speed text color matches tracker state color (Gray when `UNKNOWN`). Verify GPS age is a positive number (e.g., "5s") and not negative.

## Chapter 4 - Viewer Mode & Remote Sync
**Goal:** Validate real-time synchronization.

*   **4.1 Viewer Setup:** ID sync.
*   **4.2 Remote HUD Sync & Alarm Authority (Issue #1391):**
    *   **Action:** Trigger alarm on remote Tracker.
    *   **Verification:** Verify Alarm manifest ONLY on Viewer (Red Screen + Siren). Confirm Viewer is the exclusive authority for alarm alerts as per **R872**.
*   **4.3 Temporal Authority Check:**
    *   Verify `isGpsFresh` uses receipt-time deltas.
*   **4.4 Spontaneous Alarm Audit (Issue #1382):**
    *   **Action:** Deploy app and monitor Viewer behavior without triggering alarms.
    *   **Verification:** Confirm no siren sounds immediately after deployment or autonomously thereafter. Verify that Global Mute and Volume controls correctly suppress audio.
*   **4.5 Ribbon UX Legibility (Issue #1384):**
    *   **Action:** Inspect the Ribbon time ruler.
    *   **Verification:** Verify time ruler text and markings are clearly legible and not occluded or blurred.
*   **4.6 Peer Connectivity (Issue #1385):**
    *   **Action:** Verify Diagnostic LEDs on both devices.
    *   **Verification:** Confirm GPS, TRK, DAT (Viewer) and VWR (Tracker) transition to Green/Cyan indicating active link.
*   **4.7 Connectivity Alarm Suppression (Issue #1401):**
    *   **Action:** Induce Relay connection loss on Viewer.
    *   **Verification:** Verify "Relay Lost" or "Offline" alerts do NOT trigger a siren and do NOT force the full-screen Red Alert overlay. Confirm alerts appear in the status area and logs but allow uninterrupted navigation to other screens.
*   **4.8 Alarm Overlay Z-Index (Issue #1402):**
    *   **Action:** Trigger a security alarm (e.g., Tamper) while Settings or Logs overlay is open.
    *   **Verification:** Confirm Red Alert screen appears ABOVE the Settings/Logs overlay, completely covering the UI as a priority sentinel.
*   **4.9 Mode-Based Alert Restriction (Issue #1413):**
    *   **Action:** Induce violations while in Tracker Mode.
    *   **Verification:** Confirm the Red Alert Screen (AlarmOverlay) is NEVER promoted on the Tracker. Only status bar badges (ALM) and logs should reflect the violation to preserve stealth.
*   **4.10 Alarm Acknowledgment Idempotency (Issue #1410):**
    *   **Action:** Acknowledge an alert on the Viewer during a 500ms network jitter event.
    *   **Verification:** Confirm the alert does not "re-trigger" on the Tracker. Uses `violationStartTs` to ensure stale telemetry cannot revive a resolved alarm.
*   **4.11 HUD Visibility & Occlusion (Issue #1421):**
    *   **Action:** Open Settings or Log overlays in Portrait mode.
    *   **Verification:** Confirm that the Header and HUD (StatusBar) hide automatically to prevent overlapping with overlay controls. Confirm the map remains visible behind the HUD surface (alpha 0.4).

## Chapter 5 - Recovery and Edge Cases
**Goal:** Verify system resilience against signal loss and power events.

*   **5.1 GNSS Zombie Recovery (Issue #905 / R252):**
    *   Place device in shielded area; move to clear view.
    *   **Verification:** Observe if `SIGNAL LOSS` clears on HUD within 30s.
*   **5.2 Power Loss Recovery:** 
    *   Force stop the app. Relaunch and verify the `HistoryDao` restores the last known valid state without data corruption.

## Chapter 6 - Forensic Stress Testing
**Goal:** Verify system stability and alert latching under high-frequency load and thermal stress.

*   **6.1 Manual Forensic Stress Test (Task #071):**
    *   Go to **Settings -> Phone Setup** and tap **TRIGGER FORENSIC STRESS TEST**.
    *   **Verification:** 
        *   Open the Log Overlay and confirm `JAMMER SUSPICION` and `GPS STALL` markers are injected and latched correctly.
        *   Check the Ribbons Overlay to ensure the violation state is persisted.
*   **6.2 Heat Mitigation Validation (Issue #191):**
    *   Trigger thermal simulation via `MainViewModel.simulateThermalEvent(true)` (Command: `SimulateThermalEvent`).
    *   **Verification:**
        1. Check Logcat for `SYSTEM EMERGENCY: Simulated Thermal limit reached. Entering forced COOLING MODE.`
        2. Monitor `TrackerService` forensic sampling. Confirm delay updates to `500ms`.
        3. Verify the Dashboard shows "Cooling Mode" active.
        4. Disable simulation and verify recovery to normal sampling.

## Chapter 7 - Architectural Integrity & Performance
**Goal:** Verify thread safety and resource management under high load.

*   **7.1 UI Performance Audit:** Check for Davey events during 100Hz bursts.
*   **7.2 DI/Hilt Stability:** Cold-start service after process death.
*   **7.3 Engine Thread Safety (Issue #1410 / R-ID 585):**
    *   **Action:** Run Automated Stress Test (250Hz sensor audit).
    *   **Verification:** Confirm zero `ConcurrentModificationException` crashes. Map mutations in `MainAlarmLogic` must be synchronized with `AppAlarmManager` iteration.
*   **7.4 WindowManager Resource Lifecycle (Issue #1402-B / R-ID 582):**
    *   **Action:** Sustained 1-hour alert cycle on low-memory device (A15).
    *   **Verification:** Confirm `AlarmOverlayService` cleans up all views on `stopSelf()`. No cumulative memory growth from orphaned `ComposeView` instances.
*   **7.5 Native Stationary Convergence (Issue #SIMP-1510-1):**
    *   **Action:** Monitor sensor diagnostics while the device is stationary on a desk.
    *   **Verification:** Confirm stationary detection and vibration floor EMA calculations are handled by JNI (no JVM floating point errors). Verify zero lag in "STATIONARY" status update.
*   **7.6 Granular HUD Binding (Issue #1420):**
    *   **Action:** Update remote battery level and observe HUD recomposition counts via Layout Inspector.
    *   **Verification:** Confirm that only the battery-related sub-components recompose. The map and other telemetry slices must remain unaffected due to granular interface slicing.

## Chapter 8 - Validation Hooks & Aggregation (vOct18.22)
**Goal:** Verify the new simulation hooks and UI state aggregation logic.
*   **8.1 Forensic Stall Simulation:** Toggle simulation in Diagnostics.
    *   **Verification:** Observe Logcat for `Forensic Audit: Simulation mode ENABLED` and verify `ALERT_ID_PERFORMANCE_SPIKE` triggers if EMA reliability drops below 0.85.
*   **8.2 State Aggregation Stability:** Navigate between HUD layers and modes.
    *   **Verification:** Ensure no UI flickers or flow-race conditions occur during rapid state transitions.

## Chapter 9 - Audio, Alerts, and Notification Latency
**Goal:** Verify alert delivery speed and audio hardware stability.
*   **9.1 Siren Validation:** Trigger "Test Siren" in Sound settings and verify immediate, unclipped playback.
*   **9.2 Notification Latency:** Measure delay between violation and foreground notification update (<1s).

## Chapter 10 - Native Resource Lifecycle (Issue #249)
**Goal:** Verify clean disposal of native and sensor resources.
*   **10.1 Bridge Disposal:** Toggle mode 5 times and monitor for `BaseEventQueue.dispose` failures.

## Chapter 11 - Geofence Edge Cases & Uncertainty
**Goal:** Verify geofence stability under signal degradation.
*   **11.1 Uncertainty Drift:** Force a GPS stall while moving and verify accuracy buffer growth (R460).
*   **11.2 Recovery Hysteresis:** Exit geofence to trigger alarm, then return within threshold but outside hysteresis zone (5m).

## Chapter 12 - Database Stress & Adaptive Pruning
**Goal:** Verify data integrity under storage pressure.
*   **12.1 Pruning Sequence:** Generate 5,000 logs in low storage and verify count reduction to `ADAPTIVE_PRUNE_THRESHOLD_LOW`.
*   **12.2 Test Determinism (Issue #OCT18.22-F3):** Run `StoragePressureAuditTest` 10 times consecutively. Confirm zero failures via `triggerManualBatchProcessor` and explicit flush delays.

## Chapter 13 - Remote Command Execution
**Goal:** Verify signaling integrity and remote control reliability.
*   **13.1 Remote Siren:** Trigger siren from Viewer and verify Tracker audible response within 5s.
*   **13.2 Remote Mode Switch:** Request mode change from Viewer. Tracker HUD must reflect new mode and update polling strategy.

## Chapter 14 - Forensic Ribbon Audit
**Goal:** Verify visual telemetry continuity and rendering performance.
*   **14.1 Ribbon Persistence:** Populate ribbons with 10 mins of data and restart app. Verify historical ribbons are restored accurately.
*   **14.2 High-Frequency Rendering:** Observe HUD during 100Hz IMU burst for rendering stalls.

## Chapter 15 - Battery & Power Management
**Goal:** Verify forensic power auditing and battery-aware behavior.
*   **15.1 Power Disconnect Audit:** Unplug device and verify `POWER_TAMPER` log precision (±10ms).
*   **15.2 Battery Drain Scaling:** Simulate `BATTERY_LOW` and verify polling interval scaling.

## Chapter 16 - Network Resilience & Relay Failover
**Goal:** Verify connection stability and automatic failover.
*   **16.1 Socket Recovery:** Cycle Wi-Fi and verify relay reconnection within 15s.
*   **16.2 Internet Loss Recovery:** Disable data for 90s, re-enable, and verify relay reconnection and log sync.

## Chapter 17 - Forensic Log Export & Sharing
**Goal:** Verify the forensic chain of custody during data export.
*   **17.1 Export Integrity:** Tap **SAVE LOGS** and verify inclusion of all recent traces from the `ForensicSpillBuffer`.

## Chapter 18 - Physical Tamper Escalation (Light/Acoustic)
**Goal:** Verify high-sensitivity tamper detection logic.
*   **18.1 Light Jump:** Cover then expose device to bright light. Verify `LUX` trigger and siren (if enabled).
*   **18.2 Acoustic Breach Audit:** Produce noise >90dB and verify peak dB accuracy in forensic logs.

## Chapter 19 - Multi-Viewer Consistency & Synchronization
**Goal:** Verify data consistency across multiple monitoring endpoints.
*   **19.1 Parallel Viewers:** Connect 3 viewers and verify simultaneous HUD telemetry updates (<2s delta).

## Chapter 20 - Forensic Continuity across App Upgrades
**Goal:** Verify data persistence during system updates.
*   **20.1 Schema Migration Validation:** Side-load latest build over older version and verify history point preservation.

## Chapters 21-30: UI & Connectivity Hardening
*   **21: WebSocket Reconnection Backoff Audit.** Verify exponential backoff logic on socket failure.
*   **22: Protocol Version Mismatch Graceful Fail.** Test app behavior when relay uses an incompatible protocol version.
*   **23: Large Payload Fragmentation Stability.** Send 1MB+ log batches and verify zero fragmentation corruption.
*   **24: Signature Collision Resilience (Deduplication).** Send duplicate telemetry packets and verify single-entry processing.
*   **25: Multi-device Clock Drift Synchronization.** Offset device time by 10s and verify receipt-time normalization.
*   **26: Log Integrity Hash Chain Verification.** Verify consistency of forensic hash chains during export.
*   **27: HUD Persistence during System Font Resize.** Scale system fonts to 150% and verify zero UI clipping.
*   **28: High-contrast Mode Accessibility Audit.** Enable high-contrast and verify telemetry legibility.
*   **29: Screen Reader Navigation Flow (TalkBack).** Audit the TalkBack path through the landing screen and HUD.
*   **30: Overlay Transparency under Varying Ambient Light.** Audit auto-dimming of overlays based on LUX values.

## Chapters 31-40: Logic & System Resilience
*   **31: Geofence Predictive Exit Accuracy (R460).** Audit exit prediction logic under signal bounce.
*   **32: Stationary Drift Mitigation (Urban Multipath).** Verify anchor stability in High-SNR reflection environments.
*   **33: Jump Engine Tier 3 Suppression Logic.** Audit suppression of jumps during high JNI pressure.
*   **34: Adaptive Vibration Floor Convergence.** Audit Steep-Drop EMA convergence speed.
*   **35: Acoustic Duty Cycle Transition Latency.** Audit the 500ms overlap logic during Power Save transitions.
*   **36: Barometric Lift Precision vs GPS Altitude.** Verify altitude fusion logic during stair ascent.
*   **37: Doze Mode White-listing Impact.** Verify signaling bypass when app is whitelisted.
*   **38: Battery Saver Restricted Network behavior (Issue #OCT18.22-F1):**
    *   **Action:** Enable "Battery Saver" mode on device while in a non-violation state.
    *   **Verification:** Confirm Logcat signature `ConnectivitySuite: Telemetry sync deferred (Doze/PowerSave active)`. Verify signaling resumes immediately when violation occurs or app is whitelisted.
*   **39: Data Saver Foreground Persistence.** Verify foreground connectivity when Data Saver is active.
*   **40: High-Memory Pressure Service Restart Latency.** Measure cold-start time under 90% RAM pressure.

## Chapters 41-50: Storage & Hardware
*   **41: Database Transaction Atomicity Audit.** Induce crash during batch insert; verify zero partial writes.
*   **42: Write-Ahead-Log (WAL) Performance under Load.** Measure IO Wait during sustained 50Hz DB writes.
*   **43: Forensic Spill Buffer Boundary Wrap-around.** Fill the buffer and verify clean overwrite of oldest entries.
*   **44: Indexing Speed for 50k+ History Points.** Measure ribbon scroll fluidity with large history sets.
*   **45: Light Sensor Saturation Recovery Speed.** Expose to 10k LUX and verify recovery to baseline within 2s.
*   **46: Proximity Sensor Pocket Cross-talk rejection.** Verify "In Pocket" detection does not trigger false proximity spikes.
*   **47: Magnetometer Interference Rejection.** Move device near magnets and verify heading stabilization latency.
*   **48: Thermal Throttling Hysteresis Loop Stability.** Verify system does not rapidly toggle cooling mode at the threshold.
*   **49: WorkManager Worker Chaining Reliability.** Audit sequencing of Pruning and Maintenance tasks.
*   **50: Hilt Lifecycle during OS-initiated Process Death.** Verify state restoration of injected repositories after process kill.

## Chapters 51-60: Security & Performance
*   **51: Relay URL Injection Input Validation.**
*   **52: Tracker ID Entropy & Randomness Audit.**
*   **53: Local Log Encryption Performance Overhead.**
*   **54: Permission Revoking Engine Halt Latency.**
*   **55: Log Stripping Regex Benchmark (CPU Load).**
*   **56: UI State Flow Emission Frequency (2Hz Throttle).**
*   **57: Memory Footprint of Persistent Map Markers.**
*   **58: UI Aggregator Overhead on Budget A15 Hardware.**
*   **59: IO Wait jitter during 100Hz Forensic Burst. (SIMP-OCT18.4-S1)**
*   **60: 24-hour Continuous Track Session (Soak Test).**

## Chapters 61-70: Advanced Forensics
*   **61: Multi-Sensor Correlation Priority Audit.**
*   **62: Foreground Service Android 14 Permission Compliance.**
*   **63: Status Badge Color Contrast A11y Audit.**
*   **64: Large Status Push RTT Impact.**
*   **65: Spill-Buffer Mapped Memory Overhead Audit.**
*   **66: GNSS Satellite Detail Depth (SNR Snapshot).**
*   **67: Proximity Debounce Scaling (Stationary R742).**
*   **68: HistoryDao Buffer Flushing Atomicity. (ISSUE-OCT18.18-F1)**
*   **69: Theme Stability across Configuration Changes.**
*   **70: Spill-Buffer Replay Latency (<500ms).**

## Chapters 71-80: System Integration
*   **71: Forensic Spatial Quantization Efficiency (R701).**
*   **72: WorkManager Expedited Execution Recovery.**
*   **73: Signaling Emit Delay Throttle Stability.**
*   **74: History Ribbon Point Rendering Precision.**
*   **75: Native Direct Buffer Allocation Safety.**
*   **76: Activity Recognition Settling Logic (2s deferral).**
*   **77: Acoustic Floor Contraction Stability. (SIMP-OCT18.10-S1)**
*   **78: Proactive Pruning Adaptive Thresholds.**
*   **79: MapView Zoom Persistence during Mode Swap.**
*   **80: Relay TLS Handshake Performance on budget hardware.**

## Chapters 81-90: Reliability Benchmarks
*   **81: Jump Engine SNR Penalty Distribution. (ISSUE-OCT18.8-F1)**
*   **82: JIT Compilation Overhead during Launch.**
*   **83: App Standby Bucket Policy Compliance.**
*   **84: RT vs Wall-clock Authority Delta Audit.**
*   **85: RTL HUD Mirroring Integrity.**
*   **86: Adaptive Proximity Debounce Stress Test.**
*   **87: Mali Driver Configuration Recovery (GPU Stall).**
*   **88: Telemetry Push Nullability Safety.**
*   **89: PINK_COLOR Contrast A11y Audit.**
*   **90: MainActivity Window Resource Cleanup.**

## Chapters 91-100: Final SOT Compliance
*   **91: Spill Buffer IO Jitter Mitigation.**
*   **92: Redundant Permission & Callback Suppression (Issue #OCT18.22-F2):**
    *   **Action:** Observe Logcat for `StackLog` entries during rapid connectivity toggles.
    *   **Verification:** Confirm `SystemStatusProviderImpl` does not register redundant `ConnectivityManager` callbacks. Trace must show single registration per lifecycle.
*   **93: Log Stripping Regex Execution Time.**
*   **94: Shadow-Cache Eviction Synchronization.**
*   **95: Magnetometer figure-8 Calibration settling.**
*   **96: Socket Reconnection Storm Prevention.**
*   **97: GNSS Jitter Threshold Adaptation (A15).**
*   **98: Maintenance Worker Periodic Interval Jitter.**
*   **99: Initial Frame Drawing Stall Mitigation.**
*   **100: Final Forensic Trace Continuity Audit (48h).**

---
**Total Testing Chapters: 100**
*(Full historical procedure restored and synchronized Oct18.22)*
