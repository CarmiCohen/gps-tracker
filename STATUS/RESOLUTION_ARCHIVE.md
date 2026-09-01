# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.01.15 (vSep.01.15)
*   **Issue #888 RESOLVED**: **Specific Sensor Unregistration Hardening (R888)**.
    *   **Problem**: While Issue #887 addressed global unregistration leaks, `HardwareProvider.kt` contained a direct `unregisterListener(this, detector)` call in its step-detector recovery logic. This bypassed the 4000ms safety latch and fallback mechanisms, leaving the app vulnerable to `BaseEventQueue` leaks during specific sensor cycling on high-load devices.
    *   **Remediation**: Refactored `ManagedSensorListener` in `ManagedHardware.kt` to support specific sensor unregistration with full hardening (Latch/Timeout/Fallback). Updated `HardwareProvider.kt` to utilize this managed implementation, ensuring consistency across all hardware disposal paths. (Sep.01.15).

## 🟢 Sep.01.14 (vSep.01.14)
*   **Issue #887 RESOLVED**: **Native BaseEventQueue Leak Remediation (R887)**.
    *   **Problem**: Persistent Logcat warnings indicated that a resource failed to call `BaseEventQueue.dispose`. Investigation revealed that high Main-thread contention (Davey stalls) on Samsung A15 devices caused hardware unregistration tasks (Sensors, GNSS, Connectivity) to time out, leaving native listeners active when callback objects were GC'd.
    *   **Remediation**: Hardened `ManagedHardware.kt` by standardizing unregistration timeouts to 4000ms. Implemented a "Direct Fallback" mechanism: if the synchronization latch times out, the unregistration is immediately attempted on the current thread as a last resort, ensuring native resources are released even if the target thread is stalled.

## 🟢 Sep.01.13 (vSep.01.13)
*   **Issue #886 RESOLVED**: **Monitor::Inflate Timing Race (R886)**.
    *   **Problem**: Sequential initialization (R884) ensured the native library was resident, but a timing race remained where GNSS stack registration occurred before the Samsung framework finished internalizing the library image.
    *   **Remediation**: Added a 500ms post-initialization settling window in `TrackerService` and `ViewerService` before initiating hardware registration.
*   **Issue #885 RESOLVED**: **Level 8 Hydration Davey Remediation (R885)**.
    *   **Problem**: Monolithic overlay hydration caused 1s+ Davey stalls on SM-A155F.
    *   **Remediation**: Decomposed hydration into 11 levels (staggered by 800ms) to distribute JIT compilation load across multiple frames.

## 🟢 Sep.01.11 (vSep.01.12)
...
