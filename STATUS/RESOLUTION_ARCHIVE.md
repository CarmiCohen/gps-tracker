# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.01.18 (vSep.01.18)
*   **Issue #891 IDENTIFIED: Persistent `BaseEventQueue.dispose` warning during teardown.**
    *   **Problem**: Hardware validation of `vSep.01.18` on SM-A155F confirmed that native disposal failures persist despite `ManagedLocationCallback` hardening and settling delays.
    *   **Status**: Open. Pending investigation into third-party library leaks (OSMDroid/Maps) or missed sensor listener paths. (Sep.01.18).

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
