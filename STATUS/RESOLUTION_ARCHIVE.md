# 📜 Resolution Archive

## 🟢 Resolved in Oct10.8
*   **Issue #SIMP-1014-2: JNI Consolidation.**
    *   **Native Pressure**: Implemented `n24` in `jdhardware-jni.cpp` for native Memory and Storage pressure evaluation with hysteresis logic.
    *   **Unified Batching**: Finalized native implementation of `n21` (GNSS), `n22` (Acoustic), and `n23` (Proximity), replacing Kotlin fallbacks for production environments.
    *   **Buffer Safety**: Increased `sharedStateBuffer` to 2048 bytes in `JdHardwareManager.kt` to accommodate large GNSS satellite batches (64 SVs) and multi-sensor diagnostics.
    *   **Forensic Throughput**: Verified zero-allocation capture path in `MonitorService.kt` using `CircularStateBuffer` flyweights and `logForensicTraceOptimized` primitives.
    *   **Build Integration**: Enabled `externalNativeBuild` in `app/build.gradle` for CMake-driven JNI compilation.

## 🟢 Resolved in Oct10.7
*   **Issue #SIMP-1011-3: Proximity Decoupling.**
    *   **Consolidation**: Centralized proximity debouncing and health index calculation fallback in `JdHardwareManager.processProximityBatchNative`.
    *   **Decoupling**: Migrated raw proximity environment heuristics (including display flicker guarding and stationary scaling) from `HardwareSuite.kt` to the manager.
    *   **Fallback**: Implemented a robust Kotlin fallback within the manager to ensure functional parity for non-native environments.
    *   **Pressure Path (SIMP-1014-2 Partial)**: Implemented Kotlin fallback for `processSystemPressureNative` with full hysteresis support for Memory and Storage gates, preparing for full JNI consolidation.

## 🟢 Resolved in Oct10.6
*   **Issue #SIMP-1011-2: Acoustic Decoupling.**
...
