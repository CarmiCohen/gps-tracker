# Architectural Answers to Sticky Service, Stopping, and First Run Workflow

## 1. Sticky Service (`START_STICKY`) Implementation Status

* **Sensor Mode (`SensorService`)**:
  * **Status**: ✅ Implemented (`START_STICKY`).
  * **Behavior**: `SensorService.onStartCommand()` returns `START_STICKY`. If the OS kills `SensorService` due to memory pressure or Low Memory Kill (LMK), Android automatically recreates the service with a `null` intent and resumes foreground telemetry sampling and sensor registration.
* **Monitor Mode (`MonitorSessionService`)**:
  * **Status**: ✅ Implemented (`START_STICKY`).
  * **Behavior**: Updated `MonitorSessionService.onStartCommand()` to return `START_STICKY`. If Android kills the monitor service while running in the background, the OS automatically restarts `MonitorSessionService` and re-establishes the WebSocket signaling client connection to resume remote trail ingestion without user intervention.

---

## 2. How to Stop the Application When Desired

* **Sensor App**:
  * **UI Method**: Tap the Settings Gear $\rightarrow$ Stop Service in `AdminActivity`, which issues `context.stopService(Intent(context, SensorService::class.java))`.
  * **Notification Method**: Dismissing or tapping the Stop action on the persistent foreground notification immediately stops the service and unregisters hardware sensors.
  * **System Method**: Force-stopping `com.jd.app.sensor` from Android Settings.
* **Monitor App**:
  * **UI Method**: Tap the Reset / Clear Target button in `MonitorActivity`. Clearing the target ID automatically stops `MonitorSessionService` via `context.stopService(...)` and closes the WebSocket connection.
  * **System Method**: Force-stopping `com.jd.app.monitor` from Android Settings.

---

## 3. How the First Run Begins

* **Sensor (First Run)**:
  1. On launch, `AdminActivity` requests runtime permissions (Fine Location, Notifications, Foreground Service Location on Android 14+).
  2. `ForensicIdentityManager.ensureIdentityGenerated(PREFIX_SENSOR)` automatically generates the Default Tracker ID `"T"`.
  3. Once permissions are granted, `AdminActivity` starts `SensorService` as a Foreground Service, initializing hardware sensors (Accelerometer, Gravity, GPS) and opening the forensic trace buffer.
* **Monitor (First Run)**:
  1. On launch, `MonitorActivity` presents the Pairing Wizard, pre-filled with Default Tracker ID `"T"`.
  2. Tapping "Pair Device" calls `viewModel.setTargetId("T")`.
  3. A reactive `LaunchedEffect(trackerId)` detects the paired target and automatically launches `MonitorSessionService` as an API 34+ `dataSync` Foreground Service, establishing a live connection to the signaling relay to ingest and display the Sensor's trail on the map.

---

## 4. Manual Start vs. Sticky Behavior

* **Manual Start**: By design, `AdminActivity` requires an explicit user action ("START SENSOR SERVICE") on first installation to prevent unauthorized sensor tracking before permissions are fully vetted and acknowledged.
* **Sticky Behavior (`START_STICKY`)**: This is an Android OS runtime recovery mechanism. It only triggers after the service has been successfully started at least once.

### How the Sticky Requirement Works in Practice
Once the service has been started (either manually by tapping the button or automatically on boot):
1. **Low Memory Kill (LMK) / OS Termination**: If Android kills `SensorService` in the background to free up memory while the app is active, the OS notes that the service returned `START_STICKY`.
2. **Automatic Restart**: Android automatically recreates and restarts `SensorService` in the background with a `null` intent, restoring sensor listeners and the foreground notification without any user intervention.

---

## 5. What Happens on Device Reboot? (`BootReceiver`)

If the phone is completely restarted (rebooted):
* The project includes a dedicated `BootReceiver` listening for `Intent.ACTION_BOOT_COMPLETED`.
* Upon device reboot, `BootReceiver` intercepts the system broadcast, verifies that required runtime permissions (Location, Notifications) are already granted, and automatically starts `SensorService` in the foreground immediately—without requiring the user to unlock the phone, open the app, or tap "START SENSOR SERVICE".
