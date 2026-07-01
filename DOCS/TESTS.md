# Testing Dashboard and Telemetry (Tracker Mode)

## 1. Verify Sensor Telemetry
The dashboard currently shows static values. Verify that internal sensors report data to the `MainViewModel`.
* **Action:** Move the device or cover the light sensor to see if `Vibration`, `Tilt`, or `Lux` change.
* **Logcat Verification:** `adb logcat -s AppSensorManager TrackerService`

## 2. Test Alarm Logic (Telemetry Reaction)
Verify that the dashboard responds to "Violations."
* **Action:** Briefly shake the device to trigger a `MOVING` state or `Vibration` alert.
* **Verification:** Dashboard should highlight the triggered alert in **Red** (or **Pink** for forensics).

## 3. Verify Remote Connectivity (End-to-End)
* **Action:** Deploy the app to a **second device** in **Viewer Mode** using Tracker ID `renumb`.
* **Verification:** The `VWR` badge on the Tracker should turn green and `RTT` (Ping) should show active timing.

## 4. Monitor UI Performance
* **Action:** Observe if the UI remains fluid while telemetry is active.
* **Verification:** Check logs for "Skipped frames" or "Davey" events during active sensor streaming.
