# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.01.16 (vSep.01.16)
*   **Issue #889 RESOLVED**: **ManagedHardware Boilerplate Reduction (R889)**.
    *   **Problem**: Multiple managed callback classes in `ManagedHardware.kt` duplicated complex unregistration logic (Main-thread checking, latch-based synchronization, 4000ms timeouts, and direct fallback mechanisms). This redundancy increased maintenance risk and the potential for inconsistent hardening across different hardware types.
    *   **Remediation**: Extracted the shared unregistration logic into a `ManagedUnregistrationHelper` object. Refactored `ManagedNetworkCallback`, `ManagedGnssStatusCallback`, `ManagedSensorListener`, and `ManagedDisplayListener` to delegate their disposal to this helper. This ensures absolute consistency in how native resource leaks are prevented while significantly reducing boilerplate code. (Sep.01.16).

## 🟢 Sep.01.15 (vSep.01.15)
*   **Issue #888 RESOLVED**: **Specific Sensor Unregistration Hardening (R888)**.
    *   **Problem**: While Issue #887 addressed global unregistration leaks, `HardwareProvider.kt` contained a direct `unregisterListener(this, detector)` call in its step-detector recovery logic. This bypassed the 4000ms safety latch and fallback mechanisms, leaving the app vulnerable to `BaseEventQueue` leaks during specific sensor cycling on high-load devices.
    *   **Remediation**: Refactored `ManagedSensorListener` in `ManagedHardware.kt` to support specific sensor unregistration with full hardening (Latch/Timeout/Fallback). Updated `HardwareProvider.kt` to utilize this managed implementation, ensuring consistency across all hardware disposal paths. (Sep.01.15).

## 🟢 Sep.01.14 (vSep.01.14)
*   **Issue #887 RESOLVED**: **Native BaseEventQueue Leak Remediation (R887)**.
    *   **Problem**: Persistent Logcat warnings indicated that a resource failed to call `BaseEventQueue.dispose`. Investigation revealed that high Main-thread contention (Davey stalls) on Samsung A15 devices caused hardware unregistration tasks (Sensors, GNSS, Connectivity) to time out, leaving native listeners active when callback objects were GC'd.
    *   **Remediation**: Hardened `ManagedHardware.kt` by standardizing unregistration timeouts to 4000ms. Implemented a "Direct Fallback" mechanism: if the synchronization latch times out, the unregistration is immediately attempted on the current thread as a last resort, ensuring native resources are released even if the target thread is stalled.

...
