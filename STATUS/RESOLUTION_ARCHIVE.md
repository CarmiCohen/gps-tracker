# 📜 Resolution Archive

## 🟢 Resolved in Oct7.7
*   **Issue #SIMP-1007-16: Native Anomaly Propagation.**
    *   **Flag Convergence**: Successfully propagated `isSuspiciousNoise` and `isMemoryPressureThrottled` from JNI `VibrationBatch` into the `ForensicSnapshot` and the global `LocationUpdate` monolith.
    *   **Telemetry Hardening**: Integrated anomaly flags into Protobuf (`app_settings.proto`) and JSON mappings to ensure Viewer-side visibility of hardware-level interference and memory stress.
    *   **HUD Integration**: Exposed anomaly state in `DashboardHealthState` and `HudHealthState` for real-time diagnostic reporting.
*   **Issue #SIMP-1007-17: Native Memory Pressure Throttling.**
    *   **Sentinel Guard**: Updated `LocationSentinel.shouldThrottlePolling` to force a throttled GPS interval whenever native heap pressure is detected, preempting background OOM events.

## 🟢 Resolved in Oct7.6
*   **Issue #SIMP-1007-16: JNI FastPath Expansion.**
    *   **Native Correlation**: Expanded `processVibrationBatch` (n19) to include `snr`, `thermal`, and `heap` snapshots, allowing native-layer correlation between vibration bursts and signal quality.
    *   **Efficiency**: Implemented caching for thermal and heap probes in `HardwareSuite.kt` to eliminate redundant system calls during 100Hz sensor processing.
    *   **Architecture**: Updated `VibrationBatch` and `JdHardwareManager` bridge to support the expanded data structure, ensuring zero-copy transition through `DirectByteBuffer`.

## 🟢 Resolved in Oct7.5
*   **Issue #SIMP-1007-15: Unified Snapshot Container (Completion).**
...
