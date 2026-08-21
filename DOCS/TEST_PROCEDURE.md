# Test Procedure - GPS Tracker

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
    *   **Verification:** Unit tests verified. 3 Android integration tests pass on the target hardware.
    *   **Status (Aug.21.00):** ✅ PASSED.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
    *   **Verification:** App launches to the "Mode Selection" screen without crash.
    *   **Status (Aug.21.00):** ✅ PASSED. 
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions: Location (Always), Notifications, Microphone, and "Display over other apps."
    *   Verify that the "Missing Permissions" warning disappears once all are granted.
    *   **Status (Aug.21.00):** ✅ PASSED. All permissions granted; HUD warning cleared.
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page (Mode Selection) for 15 minutes (or 2 seconds for auto-recovery).
    *   **Verification:** Ensure no services start prematurely during the 2s recovery window.
    *   **Auto-Recovery Logic:** SOT requires that if a previous session exists, the app must auto-restore to the required mode within 2 seconds.
    *   **Status (Aug.21.00):** ✅ PASSED. 2s auto-recovery delay verified.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button on the landing page.
    *   **Status (Aug.21.00):** ✅ PASSED. Tracker HUD initialized.
*   **2.2 Exercise Setup Options:**
    *   Navigate to **Settings -> Phone Setup**.
    *   Verify **System Diagnostics**: Revoke a permission (e.g., Overlay) in system settings, return to the app, and tap **REFRESH STATUS**. The UI must update to "DENIED" (Red) immediately.
    *   Check **Battery Optimization**: Ensure the app prompts to be excluded from battery optimization.
    *   **Status (Aug.21.00):** ✅ PASSED. Reactive diagnostic refresh verified.
*   **2.3 Sensor Calibration:**
    *   Adjust the sensitivity sliders for Vibration and Tilt.
    *   Verify that internal thresholds in `TelemetryAggregator` update accordingly (check Logcat).
    *   **Status (Aug.21.06):** ✅ PASSED. (Sliders restored and verified in vAug.21.04).

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy, physical sentinel logic, and forensic integrity.

*   **3.1 Main Screen Completeness:**
    *   Verify all HUD elements: `Vibration`, `Tilt`, `Lux`, `Speed`, and `GPS Accuracy`.
    *   Check that the status ribbon shows `STATIONARY` when the device is at rest.
*   **3.2 Physical Sentinel (Alarm Logic):**
    *   **Vibration Test:** Briefly shake the device. The `Vibration` field should highlight, and the status should transition to `MOVING` or `ALARM`.
    *   **Tilt Test:** Tilt the device >15°. Verify the UI reflects the orientation change.
    *   **Light-Jump:** Cover the light sensor for 5 seconds, then expose it to bright light. Verify the `Lux` trigger.
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
    *   Trigger thermal simulation via `MainViewModel.simulateThermalEvent(true)` (Command: `SimulateThermalEvent`).
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

---
## Test Log: Aug.21.06
*   **Chapter 1 (Launch):** ✅ PASSED (2024-08-21)
*   **Chapter 2 (Config):** ✅ PASSED (2024-08-21) - Sensitivity Sliders Verified.
*   **Chapter 3 (Tracker):** ✅ PASSED (2024-08-21)
*   **Chapter 4 (Viewer):** ✅ PASSED (2024-08-21)
*   **Chapter 5 (Recovery):** ✅ PASSED (2024-08-21)
*   **Chapter 6 (Stress):** ✅ PASSED (2024-08-21) - Hysteresis logic confirmed.
*   **Chapter 7 (Integrity):** ✅ PASSED (2024-08-21) - Hilt injection stable.
