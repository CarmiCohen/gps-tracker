# Test Procedure - GPS Tracker (vSep.02.76)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
    *   **Verification:** Unit tests verified. 10 Android integration tests pass on the target hardware.
    *   **Status (Sep.02.76):** ✅ PASSED.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
    *   **Verification:** App launches to the "Mode Selection" screen without crash.
    *   **Status (Sep.02.76):** ✅ PASSED.
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions: Location (Always), Notifications, Microphone, and "Display over other apps."
    *   Verify that the "Missing Permissions" warning disappears once all are granted.
    *   **Status (Sep.02.76):** ✅ PASSED. Battery Optimization navigation hardened (R896).
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page (Mode Selection) for 15 minutes (or 2 seconds for auto-recovery).
    *   **Verification:** Ensure no services start prematurely during the 2s recovery window.
    *   **Auto-Recovery Logic:** SOT requires that if a previous session exists, the app must auto-restore to the required mode within 2 seconds.
    *   **Status (Sep.02.76):** ✅ PASSED.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button on the landing page.
    *   **Status (Sep.02.76):** ✅ PASSED. Tracker HUD initialized.
*   **2.2 Exercise Setup Options:**
    *   Navigate to **Settings -> Phone Setup**.
    *   Verify **System Diagnostics**: Revoke a permission (e.g., Overlay) in system settings, return to the app, and tap **REFRESH STATUS**. The UI must update to "DENIED" (Red) immediately.
    *   Check **Battery Optimization:** Ensure the app prompts to be excluded from battery optimization.
    *   **Status (Sep.02.76):** ✅ PASSED.
*   **2.3 Sensor Calibration:**
    *   Adjust the sensitivity sliders for Vibration and Tilt.
    *   Verify that internal thresholds in `SentinelValidator` update accordingly (check Logcat).
    *   **Status (Sep.02.76):** ✅ PASSED. UI sensitivity propagated to Engine (R-ID 198).

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy, physical sentinel logic, and forensic integrity.

*   **3.1 Main Screen Completeness:**
    *   Verify all HUD elements: `Vibration`, `Tilt`, `Lux`, `Speed`, and `GPS Accuracy`.
    *   Check that the status ribbon shows `STATIONARY` when the device is at rest.
    *   **Status (Sep.02.76):** ✅ PASSED. Resolved Issue #240: Telemetry decoupled from GPS fix status.
*   **3.2 Physical Sentinel (Alarm Logic):**
    *   **Vibration Test:** Briefly shake the device. The `Vibration` field should highlight, and the status should transition to `MOVING` or `ALARM`.
*   **3.3 Service Persistence:**
    *   Swipe the app away from the "Recents" menu.
    *   **Verification:** Confirm the foreground notification remains visible and telemetry continues to log (verified via `adb logcat -s TrackerService`).

## Chapter 4 - Viewer Mode & Remote Sync
**Goal:** Validate real-time synchronization and remote monitoring.

*   **4.1 Viewer Setup:** Deploy the app to a second device (Viewer) and enter the Tracker ID from the first device.
*   **4.2 Remote HUD Sync:**
    *   Trigger an alarm on the Tracker device.
    *   **Verification:** The Viewer HUD must transition to the alert state simultaneously. The `VWR` badge on the Tracker should turn Green.
*   **4.3 Temporal Authority Check:**
    *   Shift the Viewer's system time by -2 minutes.
    *   **Verification:** HUD elements must remain Green. The `isGpsFresh` logic should rely on receipt-time deltas, not absolute system time.
*   **4.4 Map Settings & Tools:**
    *   Open the Map in Viewer Mode.
    *   Tap the purple Settings icon to expand Map Tools.
    *   **Verification:** All tools (Zoom, Center, Fence Toggle, Violation Toggle, Save/Load, Add/Del Home Point) MUST be functional. Verify that tapping on the map in ADD mode correctly places a Home Point.
    *   **Status (Sep.02.76):** ✅ PASSED. Resolved Issue #246 (R247).

## Chapter 5 - Recovery and Edge Cases
**Goal:** Verify system resilience against signal loss and power events.

*   **5.1 GPS Revival:** Place the device in a shielded area (GPS Dead zone). Wait for the "Ghost Mode" (dimmed UI) to trigger. Move back to a clear view and verify "Escalated GPS Revival" brings the signal back within 30 seconds.
*   **5.2 Power Loss Recovery:** Force stop the app. Relaunch and verify the `HistoryDao` restores the last known valid state without data corruption.

## Chapter 6 - Forensic Stress Testing
**Goal:** Verify system stability and alert latching under high-frequency load and thermal stress.

*   **6.1 Manual Forensic Stress Test (Task #071):**
    *   Go to **Settings -> Phone Setup** and tap **TRIGGER FORENSIC STRESS TEST**.
    *   **Verification:** 
        *   Open the Log Overlay and confirm `JAMMER SUSPICION` and `GPS STALL` markers are injected and latched correctly.
        *   Check the Ribbons Overlay to ensure the violation state is persisted.
*   **6.2 Heat Mitigation Validation (Issue #191):**
    *   Trigger thermal simulation via `MainViewModel.simulateThermalEvent(true)`.
    *   **Verification:**
        1. Check Logcat for `SYSTEM EMERGENCY: Simulated Thermal limit reached. Entering forced COOLING MODE.`
        2. Monitor `TrackerService` forensic sampling. Confirm delay updates to `500ms`.
        3. Verify the Dashboard shows "Cooling Mode" active.
        4. Disable simulation and verify recovery to normal sampling.

## Chapter 7 - Architectural Integrity & Performance
**Goal:** Verify underlying system standards, timing authority, and UI fluidness.

*   **7.1 UI Performance Audit (Task #031):**
    *   Observe if the UI remains fluid while telemetry is active over an extended period.
    *   **Verification:** Check logs for "Skipped frames" or "Davey" events during active sensor streaming (>700ms).
*   **7.2 DI/Hilt Stability (#066):**
    *   Cold-start the `TrackerService` from a killed state (swipe away from recents).
    *   **Verification:** Check Logcat for `@AndroidEntryPoint` initialization. Ensure no `IllegalStateException` occurs during `HistoryDao` or `LogDao` injection.
*   **7.3 Forensic Pipeline Hardening (R196/R197):**
    *   Verify EMA reliability degradation and `ALERT_ID_PERFORMANCE_SPIKE` alarm triggers under load using the `SetForensicSimulation` hook.
    *   **Verification:** Ensure range-based deduplication eliminates IO spikes during 100Hz bursts.

## Chapter 8 - Validation Hooks & Aggregation (vAug.21.09)
**Goal:** Verify the new simulation hooks and UI state aggregation logic.
*   **8.1 Forensic Stall Simulation:** Toggle simulation in Diagnostics.
    *   **Verification:** Observe Logcat for `Forensic Audit: Simulation mode ENABLED` and verify `ALERT_ID_PERFORMANCE_SPIKE` triggers if EMA reliability drops below 0.85.
*   **8.2 State Aggregation Stability:** Navigate between HUD layers and modes.
    *   **Verification:** Ensure no UI flickers or flow-race conditions occur during rapid state transitions.
*   **Status (Aug.21.09):** ✅ PASSED. Granular flow segmentation (#248) eliminates Davey stalls.

## Chapter 9 - Audio, Alerts, and Notification Latency
**Goal:** Verify alert delivery speed and audio hardware stability.
*   **9.1 Siren Validation:** Trigger "Test Siren" in Sound settings and verify immediate, unclipped playback.
*   **9.2 Notification Latency:** Measure delay between violation and foreground notification update (<1s).

## Chapter 10 - Native Resource Lifecycle (vAug.21.09)
**Goal:** Verify clean disposal of native and sensor resources.
*   **10.1 Bridge Disposal:** Toggle mode 5 times and monitor for `BaseEventQueue.dispose` failures.
*   **Status (Aug.21.09):** ✅ PASSED. Migrated JNI to suspend initialize (#265) and implemented missing native release (#249).

## Chapter 11 - Geofence Edge Cases & Uncertainty
**Goal:** Verify geofence stability under signal degradation.
*   **11.1 Uncertainty Drift:** Force a GPS stall while moving and verify accuracy buffer growth (R460).
*   **11.2 Recovery Hysteresis:** Exit geofence to trigger alarm, then return within threshold but outside hysteresis zone (5m).

## Chapter 12 - Database Stress & Adaptive Pruning
**Goal:** Verify data integrity under storage pressure.
*   **12.1 Pruning Sequence:** Generate 5,000 logs in low storage and verify count reduction to `ADAPTIVE_PRUNE_THRESHOLD_LOW`.
*   **12.2 Startup Staggering:** Verify that DB pruning is delayed by >15s during boot to prevent frame drops on budget hardware.
*   **Status (Sep.02.62):** ✅ PASSED. Verified 16s delay in `MainViewModel` (#120b).

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
*   **17.2 Metadata Sanitization (R779):** Open the exported JSON file. Verify all internal paths are replaced with `[INTERNAL_PATH]` and hardware models are normalized to `[HW_ID]`.
*   **Status (Sep.02.62):** ✅ PASSED. Hardened Replay and export paths verified.

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
*   **21:** WebSocket Reconnection Backoff Audit.
*   **22:** Protocol Version Mismatch Graceful Fail.
*   **23:** Large Payload Fragmentation Stability.
*   **24:** Signature Collision Resilience (Deduplication).
*   **25:** Multi-device Clock Drift Synchronization.
*   **26:** Log Integrity Hash Chain Verification.
*   **27:** HUD Persistence during System Font Resize.
*   **28:** High-contrast Mode Accessibility Audit.
*   **29:** Screen Reader Navigation Flow (TalkBack).
*   **30:** Overlay Transparency under Varying Ambient Light.

## Chapters 31-40: Logic & System Resilience
*   **31:** Geofence Predictive Exit Accuracy (R460).
*   **32:** Stationary Drift Mitigation (Urban Multipath).
*   **33:** Jump Engine Tier 3 Suppression Logic.
*   **34:** Adaptive Vibration Floor Convergence.
*   **35:** Acoustic Duty Cycle Transition Latency.
*   **36:** Barometric Lift Precision vs GPS Altitude.
*   **37:** Doze Mode White-listing Impact.
*   **38:** Battery Saver Restricted Network behavior.
*   **39:** Data Saver Foreground Persistence.
*   **40:** High-Memory Pressure Service Restart Latency.

## Chapters 41-50: Storage & Hardware
*   **41:** Database Transaction Atomicity Audit.
*   **42:** Write-Ahead-Log (WAL) Performance under Load.
*   **43:** Forensic Spill Buffer Boundary Wrap-around.
*   **44:** Indexing Speed for 50k+ History Points.
*   **45:** Light Sensor Saturation Recovery Speed.
*   **46:** Proximity Sensor Pocket Cross-talk rejection.
*   **47:** Magnetometer Interference Rejection.
*   **48:** Thermal Throttling Hysteresis Loop Stability.
*   **49:** WorkManager Worker Chaining Reliability.
*   **50:** Hilt Lifecycle during OS-initiated Process Death.

## Chapters 51-60: Security & Performance
*   **51:** Relay URL Injection Input Validation.
*   **52:** Tracker ID Entropy & Randomness Audit.
*   **53:** Local Log Encryption Performance Overhead.
*   **54:** Permission Revoking Engine Halt Latency.
*   **55:** Log Stripping Regex Benchmark (CPU Load).
*   **56:** UI State Flow Emission Frequency (2Hz Throttle).
*   **57:** Memory Footprint of Persistent Map Markers.
*   **58:** UI Aggregator Overhead on Budget A15 Hardware.
*   **59:** IO Wait jitter during 100Hz Forensic Burst.
*   **60:** 24-hour Continuous Track Session (Soak Test).

## Chapters 61-70: Advanced Forensics
*   **61:** Multi-Sensor Correlation Priority Audit.
*   **62:** Foreground Service Android 14 Permission Compliance.
*   **63:** Status Badge Color Contrast A11y Audit.
*   **64:** Large Status Push RTT Impact.
*   **65:** Spill-Buffer Mapped Memory Overhead Audit.
*   **66:** GNSS Satellite Detail Depth (SNR Snapshot).
*   **67:** Proximity Debounce Scaling (Stationary R742).
*   **68:** HistoryDao Buffer Flushing Atomicity.
*   **69:** Theme Stability across Configuration Changes.
*   **70:** Spill-Buffer Replay Latency (<500ms).

## Chapters 71-80: System Integration
*   **71:** Forensic Spatial Quantification Efficiency (R701).
*   **72:** WorkManager Expedited Execution Recovery.
*   **73:** Signaling Emit Delay Throttle Stability.
*   **74:** History Ribbon Point Rendering Precision.
*   **75:** Native Direct Buffer Allocation Safety.
*   **76:** Activity Recognition Settling Logic (2s deferral).
*   **77:** Acoustic Floor Contraction Stability.
*   **78:** Proactive Pruning Adaptive Thresholds.
*   **79:** MapView Zoom Persistence during Mode Swap.
*   **80:** Relay TLS Handshake Performance on budget hardware.

## Chapters 81-90: Reliability Benchmarks
*   **81:** Jump Engine SNR Penalty Distribution.
*   **82:** JIT Compilation Overhead during Launch.
*   **83:** App Standby Bucket Policy Compliance.
*   **84:** RT vs Wall-clock Authority Delta Audit.
*   **85:** RTL HUD Mirroring Integrity.
*   **86:** Adaptive Proximity Debounce Stress Test.
*   **87:** Mali Driver Configuration Recovery (GPU Stall).
*   **88:** Telemetry Push Nullability Safety.
*   **89:** PINK_COLOR Contrast A11y Audit.
*   **90:** MainActivity Window Resource Cleanup.

## Chapters 91-100: Final SOT Compliance
*   **91:** Spill Buffer IO Jitter Mitigation.
*   **92:** Redundant Permission Flow Suppression.
*   **93:** Log Stripping Regex Execution Time.
*   **94:** Shadow-Cache Eviction Synchronization.
*   **95:** Magnetometer figure-8 Calibration settling.
*   **96:** Socket Reconnection Storm Prevention.
*   **97:** GNSS Jitter Threshold Adaptation (A15).
*   **98:** Maintenance Worker Periodic Interval Jitter.
*   **99:** Initial Frame Drawing Stall Mitigation.
*   **100:** Final Forensic Trace Continuity Audit (48h).

---
## Test Log: Sep.02.76
*   **Chapters 1-4, 7, 12, 17:** ✅ PASSED. Verified fixes for #246, #240, #118, #120b, #005, #119, #180.
*   **Remaining Chapters:** 🟡 PENDING FIELD VALIDATION.
