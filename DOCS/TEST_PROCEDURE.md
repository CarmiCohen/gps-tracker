# Test Procedure - GPS Tracker (vSep.03.15)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
    *   **Verification:** Unit tests verified. 10 Android integration tests pass on the target hardware.
    *   **Status (Sep.03.15):** ✅ PASSED.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
    *   **Verification:** App launches to the \"Mode Selection\" screen without crash.
    *   **Status (Sep.03.15):** ✅ PASSED.
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions: Location (Always), Notifications, Microphone, and \"Display over other apps.\"
    *   Verify that the \"Missing Permissions\" warning disappears once all are granted.
    *   **Status (Sep.03.15):** ✅ PASSED. Battery Optimization navigation hardened (R896).
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page (Mode Selection) for 15 minutes (or 2 seconds for auto-recovery).
    *   **Verification:** Ensure no services start prematurely during the 2s recovery window.
    *   **Auto-Recovery Logic:** SOT requires that if a previous session exists, the app must auto-restore to the required mode within 2 seconds.
    *   **Status (Sep.03.15):** ✅ PASSED.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the \"Tracker\" button on the landing page.
    *   **Status (Sep.03.15):** ✅ PASSED. Tracker HUD initialized.
*   **2.2 Exercise Setup Options:**
    *   Navigate to **Settings -> Phone Setup**.
    *   Verify **System Diagnostics**: Revoke a permission (e.g., Overlay) in system settings, return to the app, and tap **REFRESH STATUS**. The UI must update to \"DENIED\" (Red) immediately.
    *   Check **Battery Optimization:** Ensure the app prompts to be excluded from battery optimization.
    *   **Status (Sep.03.15):** ✅ PASSED.
*   **2.3 Sensor Calibration:**
    *   Adjust the sensitivity sliders for Vibration and Tilt.
    *   Verify that internal thresholds in `SentinelValidator` update accordingly (check Logcat).
    *   **Status (Sep.03.15):** ✅ PASSED. UI sensitivity propagated to Engine (R-ID 198).

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy, physical sentinel logic, and forensic integrity.

*   **3.1 Main Screen Completeness:**
    *   Verify all HUD elements: `Vibration`, `Tilt`, `Lux`, `Speed`, and `GPS Accuracy`.
    *   **Verification:** HUD must show live sensor telemetry (Vibration/Tilt/Lux) immediately upon service start, even before a GPS fix is acquired.
    *   **Status (Sep.03.15):** ✅ PASSED. Resolved Issue #240: Telemetry decoupled from GPS fix status.
*   **3.2 Physical Sentinel (Alarm Logic):**
    *   **Vibration Test:** Briefly shake the device. The `Vibration` field should highlight, and the status should transition to `MOVING` or `ALARM`.
*   **3.3 Service Persistence:**
    *   Swipe the app away from the \"Recents\" menu.
    *   **Verification:** Confirm the foreground notification remains visible and telemetry continues to log (verified via `adb logcat -s TrackerService`).

*(Chapters 4-20 remains unchanged...)*

---
## Test Log: Sep.03.15
*   **Chapters 1-3, 7, 12, 17:** ✅ PASSED. Verified fixes for #240, #118, #120b, #005, #119, #180.
*   **Remaining Chapters:** 🟡 PENDING FIELD VALIDATION.
