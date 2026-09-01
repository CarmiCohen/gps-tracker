# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.01.22 (vSep.01.22)
*   **Issue #891 RESOLVED: Strict Teardown Sequencing & Settling Expansion (R891)**.
    *   **Problem**: `vSep.01.18` validation failed on SM-A155F as native disposal warnings (`BaseEventQueue.dispose`) persisted. This indicated that the 500ms settling window was insufficient for Samsung's FusedLocationProvider implementation, or that unregistration was being cut short by thread termination.
    *   **Remediation**: Implemented strict unregistration sequencing in `HardwareProvider.stop()`—closing Location and GNSS pipes *before* lower-bandwidth sensors and display listeners. Increased the teardown settling window from 500ms to 800ms. Added forensic timing to `ManagedUnregistrationHelper` to identify specific component latencies during lifecycle transitions. (Sep.01.22).

## 🟢 Sep.01.17 (vSep.01.17)
*   **Issue #890 RESOLVED: Persistent Native Leak & Teardown Hardening (R890)**.
    *   **Problem**: Logcat warnings `A resource failed to call BaseEventQueue.dispose` persisted during `HardwareProvider` teardown. Analysis revealed `ManagedLocationCallback` was missing the 4000ms latch/fallback pattern, and the native layer was being cut off by `HandlerThread.quitSafely()` before completing disposal.
    *   **Remediation**: Hardened `ManagedLocationCallback` to use the `ManagedUnregistrationHelper`. Introduced a 500ms settling window in `HardwareProvider.stop()` to allow the Android framework to finalize native cleanup before thread termination. (Sep.01.17).

## 🟢 Sep.01.16 (vSep.01.16)
*   **Issue #889 RESOLVED**: **ManagedHardware Boilerplate Reduction (R889)**.
    *   **Problem**: Multiple managed callback classes in `ManagedHardware.kt` duplicated complex unregistration logic.
    *   **Remediation**: Extracted shared unregistration logic into `ManagedUnregistrationHelper`. Refactored all managed callbacks to delegate disposal to this helper. (Sep.01.16).

---
*For older resolutions, see prior sub-versions.*
