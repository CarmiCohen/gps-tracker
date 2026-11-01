# Testing Dashboard and Telemetry (Tracker Mode)

This document provides manual verification procedures. For the current testing backlog and task status, refer to [STATUS/QA_VALIDATION_STATUS.md](../STATUS/QA_VALIDATION_STATUS.md).

## 1. Verify Sensor Telemetry (Task #056)
The dashboard currently shows static values. Verify that internal sensors report data to the `MainViewModel`.
* **Action:** Move the device or cover the light sensor to see if `Vibration`, `Tilt`, or `Lux` change.
* **Logcat Verification:** `adb logcat -s AppSensorManager TrackerService`
* **Relates to:** Integration Test **#056** (Forensic Pipeline).

## 2. Test Alarm Logic (Task #053)
Verify that the dashboard responds to "Violations" and breaks out of locks.
* **Action:** Briefly shake the device to trigger a `MOVING` state or `Vibration` alert.
* **Anchor Test:** Physically move the device after a Hard-Lock and verify immediate breakout.
* **Verification:** Dashboard should highlight the triggered alert in **Red** (or **Pink** for forensics).
* **Relates to:** Manual Validation **#053** (Anchor Lock Breakout).

## 3. Verify Remote Connectivity (Tasks #046, #051)
* **Action:** Deploy the app to a **second device** in **Viewer Mode** using Tracker ID `renumb`.
* **Verification:** The `VWR` badge on the Tracker should turn green and `RTT` (Ping) should show active timing.
* **State Sync:** Confirm Tracker HUD and Viewer HUD transition between states simultaneously.
* **Relates to:** Manual Validation **#046** (State Sync Audit) and **#051** (Binary Parity).

## 4. Monitor UI Performance (Task #031)
* **Action:** Observe if the UI remains fluid while telemetry is active over an extended period.
* **Verification:** Check logs for "Skipped frames" or "Davey" events during active sensor streaming.

## 5. Diagnostics & Permissions (Task #064)
* **Action:** Open **Settings** -> **Diagnostics** or **Settings** -> **Phone Setup** -> **VIEW SYSTEM DIAGNOSTICS**.
* **Live Refresh Test:** 
    1. Revoke "Display over other apps" in system settings.
    2. Return to the Diagnostics screen and tap **REFRESH STATUS**.
    3. Verify "Overlay Permission" changes to **DENIED** (Red).
* **Relates to:** Manual Validation **#064** (Diagnostics UI).

## 6. Manual Forensic Stress Test (Task #071)
* **Setup:** Tap the ⚠️ icon or go to Settings -> Phone Setup.
* **Action:** Click the pink **TRIGGER FORENSIC STRESS TEST** button.
* **Verify Latching:**
    * Open the Log Overlay. Look for `FORENSIC TEST: Manually injecting Jammer/Stall markers`.
    * Verify `JAMMER SUSPICION` and `GPS STALL` violations appear in the log.
* **Relates to:** Manual Validation **#071**.

## 7. Architectural Integrity (v9.3.12 Hardening)
* **Temporal Authority (#075):**
    * **Setup:** Manually change the system time on the Viewer device by -2 minutes relative to the Tracker.
    * **Verify:** Marker and HUD elements MUST remain Green. The `isGpsFresh` calculation should now rely on receipt-time deltas rather than absolute system time comparisons.
* **DI/Hilt Stability (#066):**
    * **Action:** Cold-start the `TrackerService` from a killed state (swipe away from recents).
    * **Verify:** Check Logcat for `@AndroidEntryPoint` initialization. Ensure no `IllegalStateException` occurs during `HistoryDao` or `LogDao` injection.
* **Relates to:** Milestone **v9.3.12** Verification.
