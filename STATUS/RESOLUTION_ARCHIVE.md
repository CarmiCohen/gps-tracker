# Resolution Archive

This document archives all resolved issues and architectural refinements.

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
*   **Issue #884 RESOLVED**: **Monitor::Inflate Initialization Regression (R884)**.
    *   **Problem**: Hardware validation revealed `Monitor::Inflate` installation failures on SM-A155F. Investigation showed a race condition where GNSS registration in `HardwareProvider` was triggered before `JdHardwareManager` native initialization completed.
    *   **Remediation**: Hardened initialization sequence in `TrackerService` and `ViewerService`. Native SDK initialization is now invoked sequentially (non-asynchronous) for A15 devices before starting any hardware providers, ensuring the native library is resident before monitor installation.
*   **Issue #883 RESOLVED**: **Persistent 1074ms Davey Remediation (R883)**.
    *   **Problem**: Granular hydration in vSep.01.10 failed to eliminate the 1074ms Davey stall on SM-A155F. Logcat pinpointed `StatusRowData` JIT compilation as the primary bottleneck during the Level 8 transition.
    *   **Remediation**: Refactored `StatusRowData` in `SharedUiComponents.kt` to use a `@Stable` data class (`StatusRowState`). This reduced the Composable parameter count from 22 to 1, significantly lowering the complexity of the generated JIT code and maintaining the frame budget during high-pressure state updates.

## 🟢 Sep.01.10 (vSep.01.10)
...
