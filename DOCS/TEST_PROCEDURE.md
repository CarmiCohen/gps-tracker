# Test Procedure - GPS Tracker

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
    *   **Verification:** Unit tests verified. 3 Android integration tests pass on the target hardware.
    *   **Status (Aug.21.00):** ✅ PASSED. Unit and integration tests confirmed on hardware.
*   **1.2 Deployment:** 
    *   Deploy the latest build (`Aug.21.00`) via Android Studio.
    *   **Verification:** App launches to the "Mode Selection" screen without crash.
    *   **Status (Aug.21.00):** ✅ PASSED. Build deployed successfully to Samsung A15.
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions: Location (Always), Notifications, Microphone, and "Display over other apps."
    *   Verify that the "Missing Permissions" warning disappears once all are granted.
    *   **Status (Aug.21.00):** ✅ PASSED. All permissions granted; HUD warning cleared. Verified via `adb shell dumpsys package`.
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page (Mode Selection) for 15 minutes (or 2 seconds for auto-recovery).
    *   **Verification:** Ensure no services start prematurely during the 2s recovery window.
    *   **Auto-Recovery Logic:** SOT requires that if a previous session exists, the app must auto-restore to the required mode within 2 seconds.
    *   **Status (Aug.21.00):** ✅ PASSED. 2s auto-recovery delay aligned with SOT and verified on cold start.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button on the landing page.
    *   **Status (Aug.21.00):** ✅ PASSED. Tracker HUD initialized. Version badge Aug.21.00 confirmed.
*   **2.2 Exercise Setup Options:**
    *   Navigate to **Settings -> Phone Setup**.
    *   Verify **System Diagnostics**: Revoke a permission (e.g., Overlay) in system settings, return to the app, and tap **REFRESH STATUS**. The UI must update to "DENIED" (Red) immediately.
    *   Check **Battery Optimization**: Ensure the app prompts to be excluded from battery optimization.
    *   **Status (Aug.21.00):** ✅ PASSED. Reactive diagnostic refresh verified on Samsung A15.
*   **2.3 Sensor Calibration:**
    *   Adjust the sensitivity sliders for Vibration and Tilt.
    *   Verify that internal thresholds in `TelemetryAggregator` update accordingly (check Logcat).
    *   **Status (Aug.21.00):** 🔴 BLOCKED by Issue #247 (Missing sliders in UI).

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
*   **5.1 GPS Revival:** Place the device in a shielded area (GPS Dead zone). Wait for the "Ghost Mode" (dimmed UI) to trigger. Move back to a clear view and verify "Escalated GPS Revival" brings the signal back within 30 seconds.
*   **5.2 Power Loss Recovery:** Force stop the app. Relaunch and verify the `HistoryDao` restores the last known valid state without data corruption.

---
## Test Log: Aug.21.00
*   **1.1 Environment Reset:** ✅ PASSED (2024-08-21) - Hardware integration tests verified.
*   **1.2 Deployment:** ✅ PASSED (2024-08-21) - Version Aug.21.00 deployed.
*   **1.3 Permission Onboarding:** ✅ PASSED (2024-08-21) - Manual onboarding verified.
*   **1.4 Landing Page Stability:** ✅ PASSED (2024-08-21) - 2s Auto-recovery verified.
*   **2.1 Enter Tracker Mode:** ✅ PASSED (2024-08-21)
*   **2.2 Exercise Setup Options:** ✅ PASSED (2024-08-21) - Diagnostic reactivity confirmed.
*   **2.3 Sensor Calibration:** 🔴 BLOCKED by Issue #247.
