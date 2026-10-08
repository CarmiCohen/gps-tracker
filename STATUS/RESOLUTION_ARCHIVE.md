# 📜 Resolution Archive

## 🟢 Resolved in Oct8.11
*   **Issue #SIMP-1013-3: JNI Stationary Authority.**
    *   **Native Authority**: Fully offloaded load-aware movement authority to JNI, eliminating JVM floating-point overhead in the stationary path.
    *   **Deterministic Gating**: Implemented dynamic gate coercion and CPU-load scaling (2.0x) natively in `jdhardware-jni.cpp` (n12).
*   **Issue #SIMP-1013-2: Storage Flush Hysteresis.**
    *   **Native Authority**: Offloaded storage pressure evaluation and pruning criteria to JNI via `StoragePressureBatch` (n25).
    *   **Boundary Stabilization**: Implemented a 10MB native hysteresis window (`STORAGE_HYSTERESIS_OFFSET_MB`) to prevent "IO Thrashing" during boundary oscillations of available space.
    *   **Command Integration**: Refactored `IntegrityMonitor` and `MonitorService` to utilize `CommandEvent.TriggerStoragePrune`, ensuring authoritative native triggers drive aggressive log maintenance.

## 🟢 Resolved in Oct8.11
*   **Issue #SIMP-1013-1: Memory Pressure Hysteresis.**
    *   **Native Decision Logic**: Offloaded GC flush criteria to JNI via `MemoryPressureBatch` (n24).
    *   **Thrashing Mitigation**: Implemented a 20MB native hysteresis window (`MEMORY_HYSTERESIS_OFFSET_MB`) to prevent rapid state oscillations at the `CRITICAL` boundary.
    *   **Event-Driven Flush**: Refactored `MonitorService` and `IntegrityMonitor` to trigger aggressive memory recovery via centralized `CommandEvent.TriggerMemoryFlush`.

## 🟢 Resolved in Oct8.11
*   **Issue #SIMP-1012-3: Forensic Stability Audit.**
    *   **Jammer Discrimination**: Implemented SNR-based signal health evaluation in `ForensicAuditor` to distinguish between active jamming and signal blockage.
    *   **Zero-Allocation Auditing**: Leveraged `forEachSnrSample` in `HardwareSuite` to analyze high-frequency SNR trails without JVM heap churn (R-ID 684).
    *   **Health Authority**: Integrated forensic signal verdicts into the centralized `LocationPendingReason` pipeline, ensuring high-assurance recovery diagnostics.

## 🟢 Resolved in Oct8.11
*   **Issue #SIMP-1012-2: Native Proximity Scaling.**
    *   **JNI Migration**: Migrated proximity debouncing and health index calculation to JNI via `ProximityBatch`.
    *   **Health Centralization**: Centralized proximity health authority, ensuring `HardwareSuite` strictly follows native decisions for stationary duration and thermal load scaling.
*   **Issue #SIMP-1012-1: Forensic Retrieval Optimization.**
    *   **Zero-Allocation Infrastructure**: Refactored `CircularStateBuffer` and `HardwareSuite` to use `inline` callback-based iteration (`forEachMatch`, `forEachSnrSample`, etc.).
    *   **Performance Parity**: Achieved zero-allocation parity (R-ID 392) by eliminating `Sequence` and `Iterator` overhead during high-frequency telemetry retrieval.

## 🟢 Resolved in Oct8.11
*   **Issue #SIMP-1011-2: Acoustic JNI Offloading.**
    *   **JNI Migration**: Migrated `AudioRecord` iterative math (RMS and Peak) to JNI via `AcousticBatch`.
    *   **Latency Optimization**: Reduced JVM math overhead in the 44.1kHz audio path.
*   **Issue #SIMP-1011-3: Forensic Buffer Consolidation.**
    *   **Memory Efficiency**: Consolidated `EngineSnrSample`, `EngineAcousticSample`, and `EngineSensorSnapshot` into a single `ForensicSample` container.

## 🟢 Resolved in Oct8.11
*   **Issue #SIMP-1011-1: Native GNSS Batching.**
    *   **JNI Offloading**: Migrated GNSS status evaluation (satellites in view, used in fix, average SNR) to JNI via `GnssHealthBatch`.

## 🟢 Resolved in Oct8.11
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.**
    *   **Pipeline Finalization**: Instrumented `LocationProcessor` and `MonitorService` to ensure behavioral rejections (Jamming, Acoustic, Tamper) are promoted into the unified `LocationPendingReason`.
