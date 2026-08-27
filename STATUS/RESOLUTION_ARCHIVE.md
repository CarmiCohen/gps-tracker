# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.27.04 (vAug.27.04)
*   **Concern #747 Resolved**: **Persistent BaseEventQueue Leak (GpsManager Task Race)**. Deployment regression confirmed that `BaseEventQueue.dispose` warnings persisted because `fusedLocationClient.removeLocationUpdates()` returns an asynchronous Task. Hardened `GpsManager.stop()` to synchronously await these tasks using `Tasks.await()`, ensuring native disposal finishes before the hardware thread is terminated (R747).

## 🟢 Aug.27.03 (vAug.27.03)
*   **Concern #746 Resolved**: **Multi-Source BaseEventQueue Leak**. Identified that hardening `AppSensorManager` alone was insufficient as `GpsManager` and asynchronous `StepDetector` registration races were also orphaning native `EventQueue` resources. Standardized the "Unregister-on-Thread" pattern across all hardware managers and utilized `CountDownLatch` in `AppSensorManager.stop()` to guarantee that native listeners are disposed of before the controlling `HandlerThread` is terminated, eliminating "BaseEventQueue.dispose" warnings (R746).

## 🟢 Aug.27.02 (vAug.27.02)
*   **Concern #745 Resolved**: **Persistent BaseEventQueue Leak (AppSensorManager)**. Deployment on A15 hardware confirmed that the "BaseEventQueue.dispose" warning persisted despite GpsManager hardening. Identified that `AppSensorManager` was quitting its `sensorThread` before the system could finalize listener unregistration. Hardened `stop()` to queue unregistration on the `sensorHandler` and wait for the thread to join, ensuring deterministic disposal of the native event queue (R745).

## 🟢 Aug.27.01 (vAug.27.01)
*   **Concern #744 Resolved**: **Persistent EventQueue Leak**. Identified that the `LocationCallback` in `GpsManager.hardwareObservationFlow` was escaping the disposal sequence due to the 5-second lingering subscription of `WhileSubscribed(5000)`. Hardened `GpsManager` to explicitly track and synchronously unregister the `activeLocationCallback` during `stop()`, ensuring native resources are released before the hardware thread is quit (R744).

## 🟢 Aug.26.19 (vAug.26.19)
*   **Concern #742 Hardening**: **Managed Hardware Callbacks**. Identified that anonymous `LocationCallback` in `GpsManager.restartLocationUpdates()` and escaped async `stepDetector` registrations in `AppSensorManager` were triggering `BaseEventQueue` leaks. Implemented explicit lifecycle tracking and cancellation for these transient registrations. Centralized native hardware bridge release in `BaseMonitorService` to ensure deterministic disposal during role-swaps (R742).

## 🟢 Aug.26.18 (vAug.26.18)
*   **Concern #742 Resolved**: **Recurrent EventQueue Leak**. Identified that `GpsManager` was redundantly registering `GnssStatus.Callback` within its `callbackFlow`, leading to overlapping native event queues during polling interval changes. Decoupled the callback lifecycle from the flow and tied it strictly to the synchronized `start()`/`stop()` lifecycle of the singleton. Hardened permission checks to ensure safety during registration (R742).

## 🟢 Aug.26.17 (vAug.26.17)
*   **Concern #738 Resolved**: **EventQueue Resource Leak**. Hardened lifecycle management in `AppSensorManager` and `GpsManager` by synchronizing `start()`/`stop()` transitions and implementing strict state re-checks in asynchronous registration blocks. This prevents race conditions where native listeners could be registered after cleanup, resolving the `BaseEventQueue.dispose` failure warning (R738).

## 🟢 Aug.26.16 (vAug.26.16)
*   **Concern #739 Resolved**: **Hydration Performance Stall (A15)**. Decomposed Map Hydration into 4 distinct phases (Levels 4-7). This spreads Map Engine, Trails, Markers, and Final Overlays over multiple frames using IdleHandler and staggered delays, eliminating the 1.4s main-thread stall on A15 hardware (R739).

## 🟢 Aug.26.15 (vAug.26.15)
*   **Concern #740 Resolved**: **System Issue Counter Mismatch**. Synchronized `PhoneSetupOverlay` items with `MainUiState.systemIssuesCount`. Added Step 0 (Precise Location) and corrected completion flag for Step 5 (Auto-start) to ensure UI parity (R740).

## 🟢 Aug.26.14 (vAug.26.14)
*   **Concern #737 Resolved**: **Identity Sanitization Persistence**. Verified fix on `Aug.26.14`. The dismissal state now correctly persists through cold starts, eliminating redundant UI prompts (R976).

## 🟢 Aug.26.13 (vAug.26.13)
*   **Concern #737 Resolved**: **Identity Sanitization Persistence**. Hardened the identity sanitization lifecycle by persisting the warning dismissal state. This eliminates "re-init" noise where the sanitization overlay would reappear on every cold start even after being dismissed (R976).

## 🟢 Aug.26.12 (vAug.26.12)
*   **Issue #736 Hardening**: **Compilation Error Remediation**. Resolved a non-exhaustive `when` expression in `CommandRouter.kt` caused by a duplicate and incorrectly inherited `ClearTrails` declaration in `Models.kt`.

## 🟢 Aug.26.11 (vAug.26.11)
*   **Issue #735 Hardening**: **Setup Overlay Bypass**. Implemented a developer-mode bypass for the `PhoneSetupOverlay` to allow automated soak tests to proceed without manual permission granting.

## 🟢 Aug.26.10 (vAug.26.10)
*   **Deployment Verification**: Formally verified **Issue #723 (StackLog Leak)** and **Issue #320 (Hardware Handshake)** on SM-A155F hardware. 

---
*For historical entries, see legacy logs.*
