# 📜 Resolution Archive

## 🟢 Resolved in Oct7.6
*   **Issue #SIMP-1007-16: JNI FastPath Expansion.**
    *   **Native Correlation**: Expanded `processVibrationBatch` (n19) to include `snr`, `thermal`, and `heap` snapshots, allowing native-layer correlation between vibration bursts and signal quality.
    *   **Efficiency**: Implemented caching for thermal and heap probes in `HardwareSuite.kt` to eliminate redundant system calls during 100Hz sensor processing.
    *   **Architecture**: Updated `VibrationBatch` and `JdHardwareManager` bridge to support the expanded data structure, ensuring zero-copy transition through `DirectByteBuffer`.

## 🟢 Resolved in Oct7.5
*   **Issue #SIMP-1007-15: Unified Snapshot Container (Completion).**
    *   **Engine Parity**: Completed migration of `EngineConnectionPoint` and `AlarmEvent` to use the unified `ForensicSnapshot` container.
    *   **App Parity**: Refactored `ConnectionPoint` and `LogEntry` in `Models.kt` to utilize the unified container for diagnostic probes.
    *   **Logic Consolidation**: Updated `contentEquals`, `reset`, and `duplicate` methods across all telemetry models to ensure atomic snapshot handling and prevent data loss during pipeline emission.
    *   **Persistence Mapping**: Verified Room entity mapping in `LogRepository` and `TelemetryMapper` to ensure flat database columns correctly interface with the domain container.

## 🟢 Resolved in Oct7.4
*   **Issue #SIMP-1007-15: Unified Snapshot Container (Architecture).**
...
