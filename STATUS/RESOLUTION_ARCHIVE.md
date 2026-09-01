# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.01.23 (vSep.01.23)
*   **Issue #891 RESOLVED: Strict Teardown Sequencing & Settling Expansion (R891)**.
    *   **Problem**: `vSep.01.18` and `vSep.01.22` validation on SM-A155F showed persistent native disposal warnings (`BaseEventQueue.dispose`).
    *   **Remediation**: Implemented and verified deterministic unregistration sequencing in `HardwareProvider.stop()`. Location/GNSS pipes are now explicitly closed before sensors and display listeners. Expanded the settling window to 800ms. Hardware validation in `vSep.01.23` confirmed zero disposal warnings during teardown. (Sep.01.23).

## 🟢 Sep.01.22 (vSep.01.22)
*   **Issue #891 IMPLEMENTED: Teardown Sequencing (R891)**.
    *   **Remediation**: Initial implementation of sequencing rules and settling window expansion. (Sep.01.22).

## 🟢 Sep.01.17 (vSep.01.17)
*   **Issue #890 RESOLVED: Persistent Native Leak & Teardown Hardening (R890)**.
    *   **Problem**: Logcat warnings `A resource failed to call BaseEventQueue.dispose` persisted during `HardwareProvider` teardown.
    *   **Remediation**: Hardened `ManagedLocationCallback` to use the `ManagedUnregistrationHelper`. Introduced a settling window in `HardwareProvider.stop()`. (Sep.01.17).

---
*For older resolutions, see prior sub-versions.*
