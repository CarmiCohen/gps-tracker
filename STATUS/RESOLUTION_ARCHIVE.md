# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.28.06 (vAug.28.06)
*   **Concern #755 Resolved**: **GNSS & Network Unregistration Hardening**. Standardized GNSS unregistration by implementing `ManagedGnssStatusCallback` in `ManagedHardware.kt` and refactoring `GpsManager.kt`. Increased unregistration timeouts to 2000ms for all managed hardware listeners to tolerate high Main Looper congestion during teardown, effectively silencing native `BaseEventQueue` disposal warnings and resolving unregistration timeouts (R755).

## 🟢 Aug.28.05 (vAug.28.05)
*   **Concern #754 Resolved**: **Managed Sensor Abstraction (Leak Suppression)**. Standardized hardware listener management in `AppSensorManager` by introducing `ManagedSensorListener` and `ManagedDisplayListener` abstractions in `ManagedHardware.kt`. This eliminates manual, redundant `CountDownLatch` logic and ensures that native event queues are synchronously and deterministically disposed on the hardware thread before termination, silencing persistent `BaseEventQueue` disposal warnings (R754).

## 🟢 Aug.28.04 (vAug.28.04)
*   **Concern #753 Resolved**: **Broadcast Hardware Abstraction (Leak Suppression)**. Soak testing revealed that while connectivity deadlocks were resolved, `BaseEventQueue.dispose` warnings persisted during shutdown. Identified that anonymous `BroadcastReceiver` instances for Battery and Power monitoring were the primary culprits. Implemented `ManagedBroadcastReceiver` to standardize and harden safe unregistration. Refactored `SystemStatusProvider` and `CommandRouter` to use this abstraction, ensuring deterministic native resource cleanup and complete silencing of hardware-linked disposal warnings (R753).

## 🟢 Aug.28.03 (vAug.28.03)
*   **Concern #752 Resolved**: **Persistent BaseEventQueue Leak (Deadlock Remediation)**. Soak testing of Aug.28.02 revealed that `BaseEventQueue` disposal warnings persisted due to a deadlock in the `ManagedNetworkCallback` unregistration utility. The utility was self-blocking when called from the Main Looper (during `Service.onDestroy`) because it attempted to post a synchronous task to the same looper. Hardened the abstraction to detect the calling thread and execute unregistration immediately if already on the Main Looper, ensuring deterministic native disposal (R752).

## 🟢 Aug.28.02 (vAug.28.02)
*   **Concern #751 Resolved**: **Managed Hardware Abstraction**. Resolved persistent native leaks (`BaseEventQueue` disposal failures) by implementing `ManagedNetworkCallback` and `ManagedLocationCallback`. These abstractions encapsulate the synchronous unregistration logic (Main Looper + CountDownLatch / Tasks.await) required for deterministic disposal on Samsung A15 hardware. All hardware-bound components (GpsManager, ConnectivitySuite, SystemStatusProvider) now utilize these unified utilities (R750).

## 🟢 Aug.28.01 (vAug.28.01)
*   **Concern #750 Resolved**: **Native Connectivity Leak**. Deployment regression testing confirmed that `BaseEventQueue` disposal warnings persisted due to `NetworkCallback` objects in `ConnectivitySuite` and `SystemStatusProvider` being garbage collected without deterministic unregistration. Hardened both components to perform synchronous unregistration on the Main Looper during shutdown/awaitClose, ensuring native disposal completes before the object lifecycle ends (R750).

---
*For historical entries, see legacy logs.*
