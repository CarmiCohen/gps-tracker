# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.28.03 (vAug.28.03)
*   **Concern #752 Resolved**: **Persistent BaseEventQueue Leak (Deadlock Remediation)**. Soak testing of Aug.28.02 revealed that `BaseEventQueue` disposal warnings persisted due to a deadlock in the `ManagedNetworkCallback` unregistration utility. The utility was self-blocking when called from the Main Looper (during `Service.onDestroy`) because it attempted to post a synchronous task to the same looper. Hardened the abstraction to detect the calling thread and execute unregistration immediately if already on the Main Looper, ensuring deterministic native disposal (R752).

## 🟢 Aug.28.02 (vAug.28.02)
*   **Concern #751 Resolved**: **Managed Hardware Abstraction**. Resolved persistent native leaks (`BaseEventQueue` disposal failures) by implementing `ManagedNetworkCallback` and `ManagedLocationCallback`. These abstractions encapsulate the synchronous unregistration logic (Main Looper + CountDownLatch / Tasks.await) required for deterministic disposal on Samsung A15 hardware. All hardware-bound components (GpsManager, ConnectivitySuite, SystemStatusProvider) now utilize these unified utilities (R750).

## 🟢 Aug.28.01 (vAug.28.01)
*   **Concern #750 Resolved**: **Native Connectivity Leak**. Deployment regression testing confirmed that `BaseEventQueue` disposal warnings persisted due to `NetworkCallback` objects in `ConnectivitySuite` and `SystemStatusProvider` being garbage collected without deterministic unregistration. Hardened both components to perform synchronous unregistration on the Main Looper during shutdown/awaitClose, ensuring native disposal completes before the object lifecycle ends (R750).

## 🟢 Aug.28.00 (vAug.28.00)
*   **Concern #749 Resolved**: **Persistent BaseEventQueue Leak (SystemStatusProvider)**. Deployment testing of Aug.27.05 revealed that `BaseEventQueue` disposal warnings persisted after `TrackerService` termination. Identified that `SystemStatusProviderImpl` was running multiple hardware-bound `callbackFlow` implementations (Internet, Battery, Power) in the application scope without deterministic unregistration. Since these flows were shared in the external scope, they often orphaned native `ConnectivityManager` callbacks and `BroadcastReceivers` during UI detachment or service swaps. Hardened all flows to follow SOT 1.8, ensuring explicit unregistration in `awaitClose` and logging confirmation on A15 hardware (R749).

## 🟢 Aug.27.05 (vAug.27.05)
*   **Concern #748 Resolved**: **CallbackFlow BaseEventQueue Leak (GpsManager)**. Identified that `hardwareObservationFlow` in `GpsManager.kt` was performing asynchronous unregistration in its `awaitClose` block. Since `TrackerService` cancels the collection job during role-swaps, the native `BaseEventQueue` was not being disposed of before the callback object was reclaimed. Hardened the flow to synchronously await the `removeLocationUpdates` task, ensuring native disposal completes reliably (R748).

---
*For historical entries, see legacy logs.*
