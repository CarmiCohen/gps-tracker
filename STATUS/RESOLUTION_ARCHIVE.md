# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.28.00 (vAug.28.00)
*   **Concern #749 Resolved**: **Persistent BaseEventQueue Leak (SystemStatusProvider)**. Deployment testing of Aug.27.05 revealed that `BaseEventQueue` disposal warnings persisted after `TrackerService` termination. Identified that `SystemStatusProviderImpl` was running multiple hardware-bound `callbackFlow` implementations (Internet, Battery, Power) in the application scope without deterministic unregistration. Since these flows were shared in the external scope, they often orphaned native `ConnectivityManager` callbacks and `BroadcastReceivers` during UI detachment or service swaps. Hardened all flows to follow SOT 1.8, ensuring explicit unregistration in `awaitClose` and logging confirmation on A15 hardware (R749).

## 🟢 Aug.27.05 (vAug.27.05)
*   **Concern #748 Resolved**: **CallbackFlow BaseEventQueue Leak (GpsManager)**. Identified that `hardwareObservationFlow` in `GpsManager.kt` was performing asynchronous unregistration in its `awaitClose` block. Since `TrackerService` cancels the collection job during role-swaps, the native `BaseEventQueue` was not being disposed of before the callback object was reclaimed. Hardened the flow to synchronously await the `removeLocationUpdates` task, ensuring native disposal completes reliably (R748).

## 🟢 Aug.27.04 (vAug.27.04)
*   **Concern #747 Resolved**: **Persistent BaseEventQueue Leak (GpsManager Task Race)**. Deployment regression confirmed that `BaseEventQueue.dispose` warnings persisted because `fusedLocationClient.removeLocationUpdates()` returns an asynchronous Task. Hardened `GpsManager.stop()` to synchronously await these tasks using `Tasks.await()`, ensuring native disposal finishes before the hardware thread is terminated (R747).

## 🟢 Aug.27.03 (vAug.27.03)
*   **Concern #746 Resolved**: **Multi-Source BaseEventQueue Leak**. Identified that hardening `AppSensorManager` alone was insufficient as `GpsManager` and asynchronous `StepDetector` registration races were also orphaning native `EventQueue` resources. Standardized the "Unregister-on-Thread" pattern across all hardware managers and utilized `CountDownLatch` in `AppSensorManager.stop()` to guarantee that native listeners are disposed of before the controlling `HandlerThread` is terminated, eliminating "BaseEventQueue.dispose" warnings (R746).

## 🟢 Aug.27.02 (vAug.27.02)
*   **Concern #745 Resolved**: **Persistent BaseEventQueue Leak (AppSensorManager)**. Deployment on A15 hardware confirmed that the "BaseEventQueue.dispose" warning persisted despite GpsManager hardening. Identified that `AppSensorManager` was quitting its `sensorThread` before the system could finalize listener unregistration. Hardened `stop()` to queue unregistration on the `sensorHandler` and wait for the thread to join, ensuring deterministic disposal of the native event queue (R745).

## 🟢 Aug.27.01 (vAug.27.01)
*   **Concern #744 Resolved**: **Persistent EventQueue Leak**. Identified that the `LocationCallback` in `GpsManager.hardwareObservationFlow` was escaping the disposal sequence due to the 5-second lingering subscription of `WhileSubscribed(5000)`. Hardened `GpsManager` to explicitly track and synchronously unregister the `activeLocationCallback` during `stop()`, ensuring native resources are released before the hardware thread is quit (R744).

---
*For historical entries, see legacy logs.*
