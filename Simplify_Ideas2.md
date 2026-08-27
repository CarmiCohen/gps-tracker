# Simplicity & Hardening Ideas (Aug.27.04)

## 💡 Architectural Simplification
1.  **Unified Hardware Handler (R746)**: Create a `ManagedHardwareThread` utility to encapsulate the `HandlerThread` lifecycle and the `CountDownLatch` unregistration pattern. This would reduce boilerplate and prevent future leaks in new hardware managers.
2.  **GPS Task Awaiter (R747)**: Standardize a `ManagedHardwareTask` utility to wrap Play Services `Tasks.await()` with consistent timeouts, preventing async leaks across `GpsManager` and future Google Play integrations.
3.  **Automated Leak Detection**: Integrate a lightweight `BaseEventQueue` monitor in debug builds to alert developers during role-swaps if a native queue is not disposed within 2s of a role change.

## 🛠️ Technical Debt
1.  **StepDetector Async Guard**: Replace the current `registrationJob` pattern with a synchronous `AtomicReference<RegistrationState>` to further simplify the race-condition guards in `AppSensorManager`.
2.  **BaseMonitorService Centralization**: Move the `CountDownLatch` wait logic into a protected method in `BaseMonitorService` to ensure all hardware managers follow the same timing constraints during `onDestroy`.
