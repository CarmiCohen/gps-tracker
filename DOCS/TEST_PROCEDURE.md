# Test Procedure - GPS Tracker (vSep.04.30)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
    *   **Verification:** Unit tests verified. 10 Android integration tests pass on the target hardware.
    *   **Status (Sep.04.30):** ✅ PASSED.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
    *   **Verification:** App launches to the "Mode Selection" screen without crash.
    *   **Status (Sep.04.30):** ✅ PASSED.
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions: Location (Always), Notifications, Microphone, and "Display over other apps."
    *   Verify that the "Missing Permissions" warning disappears once all are granted.
    *   **Status (Sep.04.30):** ✅ PASSED. Battery Optimization navigation hardened (R896).
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page (Mode Selection) for 15 minutes (or 2 seconds for auto-recovery).
    *   **Verification:** Ensure no services start prematurely during the 2s recovery window.
    *   **Status (Sep.04.30):** ✅ PASSED.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button on the landing page.
    *   **Status (Sep.04.30):** ✅ PASSED. Tracker HUD initialized.
*   **2.2 Exercise Setup Options:**
    *   Navigate to **Settings -> Phone Setup**.
    *   Verify **System Diagnostics**: Revoke a permission (e.g., Overlay) in system settings, return to the app, and tap **REFRESH STATUS**. The UI must update to "DENIED" (Red) immediately.
    *   Check **Battery Optimization:** Ensure the app prompts to be excluded from battery optimization.
    *   **Status (Sep.04.30):** ✅ PASSED.
*   **2.3 Sensor Calibration:**
    *   Adjust the sensitivity sliders for Vibration and Tilt.
    *   Verify that internal thresholds in `SentinelValidator` update accordingly.
    *   **Status (Sep.04.30):** ✅ PASSED.

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy, physical sentinel logic, and forensic integrity.

*   **3.1 Main Screen Completeness:**
    *   Verify all HUD elements: `Vibration`, `Tilt`, `Lux`, `Speed`, and `GPS Accuracy`.
    *   Check that the status ribbon shows `STATIONARY` when the device is at rest.
    *   **Status (Sep.04.30):** ✅ PASSED.
*   **3.2 Physical Sentinel (Alarm Logic):**
    *   **Vibration Test:** Briefly shake the device. The `Vibration` field should highlight, and the status should transition to `MOVING` or `ALARM`.
*   **3.3 Service Persistence:**
    *   Swipe the app away from the "Recents" menu.
    *   **Verification:** Confirm the foreground notification remains visible and telemetry continues to log.

## Chapter 4 - Viewer Mode & Remote Sync
**Goal:** Validate real-time synchronization and remote monitoring.

*   **4.1 Viewer Setup:** Deploy the app to a second device (Viewer) and enter the Tracker ID from the first device.
*   **4.2 Remote HUD Sync:**
    *   Trigger an alarm on the Tracker device.
    *   **Verification:** The Viewer HUD must transition to the alert state simultaneously. The `VWR` badge on the Tracker should turn Green.
*   **4.3 Temporal Authority Check:**
    *   Shift the Viewer's system time by -2 minutes.
    *   **Verification:** HUD elements must remain Green. The `isGpsFresh` logic should rely on receipt-time deltas.
*   **4.4 Map Settings & Tools:**
    *   Open the Map in Viewer Mode.
    *   Tap the purple Settings icon to expand Map Tools.
    *   **Verification:** All tools MUST be functional.
    *   **Status (Sep.04.30):** ✅ PASSED.

## Chapter 5 - Recovery and Edge Cases
**Goal:** Verify system resilience against signal loss and power events.

*   **5.1 GNSS Zombie Recovery (Issue #905 / R252):**
    *   Place the device in a shielded area until `satView` drops to 0. 
    *   **Verification:** Observe logs for `GNSS Recovery Pulse triggered`. Ensure the hardware-level location restart forces a rescan once the device is moved to a clear view.
    *   **Status (Sep.04.30):** ✅ PASSED.
*   **5.2 Power Loss Recovery:** Force stop the app. Relaunch and verify state restoration.

## Chapter 16 - Network Resilience & Relay Failover
**Goal:** Verify connection stability and automatic failover.

*   **16.1 Signaling Transport Robustness (Issue #906 / R251):**
    *   Test connection on a restricted network (or budget hardware like Samsung A15).
    *   **Verification:** Ensure `SRV` indicator turns Green via polling-to-websocket upgrade handshake. Verify no "Websocket connection failed" loops occur.
    *   **Status (Sep.04.30):** ✅ PASSED.

## Chapter 22 - Protocol Integrity
**Goal:** Verify interconnectivity across binary and JSON paths.

*   **22.1 Protobuf Identity Parity (Issue #907 / R253):**
    *   Connect S21FE (Viewer) to A15 (Tracker).
    *   **Verification:** Ensure binary telemetry packets are accepted and validated. Verify ID aliasing consistency (T -> Trk) prevents handshake rejections.
    *   **Status (Sep.04.30):** ✅ PASSED.

---
## Test Log: Sep.04.30
*   **Chapter 5.1:** ✅ PASSED. Verified #905 (GNSS Revival Pulse) for SIGNAL_LOSS/GPS_GAP.
*   **Chapter 16.1:** ✅ PASSED. Verified #906 (Transport Robustness) with polling fallback.
*   **Chapter 22.1:** ✅ PASSED. Verified #907 (Protobuf Identity Parity) and Binary Validation.
*   **Status (Sep.04.30):** Hardening phase Sep.04.xx verified on A15 and S21FE.
