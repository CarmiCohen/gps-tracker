# Testing Dashboard and Telemetry (Tracker Mode)

This document provides manual verification procedures. For the current testing backlog and task status, refer to [STATUS/testing_status.md](../STATUS/testing_status.md).

## 1. Verify Sensor Telemetry (Task #056)
The dashboard currently shows static values. Verify that internal sensors report data to the `MainViewModel`.
* **Action:** Move the device or cover the light sensor to see if `Vibration`, `Tilt`, or `Lux` change.
* **Acoustic Check:** Clap near the device. Observe the "Acoustic" bar (yellow/orange ribbon). It should pulse or jump. If not moving, `AppSensorManager` is not registering peaks.
* **Peak Test:** Try a sharper sound or tap the frame near the mic while the phone is on a soft surface (to isolate from vibration).
* **Logcat Verification:** `adb logcat -s AppSensorManager TrackerService`
* **Relates to:** Integration Test **#056** (Forensic Pipeline).

## 2. Test Alarm Logic (Task #053)
Verify that the dashboard responds to "Violations" and breaks out of locks.
* **Action:** Briefly shake the device to trigger a `MOVING` state or `Vibration` alert.
* **Anchor Test:** Physically move the device after a Hard-Lock and verify immediate breakout.
* **Muzzle Watch:** Monitor for the "GPS: Polling interval" log or map pulse; verify the "muzzle" logic runs checks at these points.
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
* **Relates to:** Manual Validation **#031** (Soak Test Monitoring).

## 5. Diagnostics & Permissions (Task #064)
* **Action:** Open the Diagnostics screen (if implemented) or check the "Permission Health Check".
* **Verification:** Screen should correctly identify Xiaomi-specific states (Autostart, Battery Saver).
* **Relates to:** Manual Validation **#064** (Diagnostics UI).

## 6. Manual Forensic Stress Test (Task #071)
* **Setup:** Tap the ⚠️ icon (if system issues exist) or go to Settings -> Phone Setup.
* **Action:** Click the pink **TRIGGER FORENSIC STRESS TEST** button.
* **Verify Latching:**
    * Open the Log Overlay. Look for `FORENSIC TEST: Manually injecting Jammer/Stall markers`.
    * Verify `JAMMER SUSPICION` and `GPS STALL` violations appear in the log.
    * Verify the "P" badge appears on the HUD with the appropriate reason.
    * Check the Ribbons Overlay (SNR or Connection) to ensure the violation state is persisted.
* **Relates to:** Manual Validation **#071**.
