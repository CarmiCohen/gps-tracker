# 📜 Resolution Archive

## 🟢 Resolved in Oct8.8
*   **Issue #SIMP-1012-2: Native Proximity Scaling.**
    *   **JNI Migration**: Migrated proximity debouncing and health index calculation to JNI via `ProximityBatch`.
    *   **Health Centralization**: Centralized proximity health authority, ensuring `HardwareSuite` strictly follows native decisions for stationary duration and thermal load scaling.
*   **Issue #SIMP-1012-1: Forensic Retrieval Optimization.**
    *   **Zero-Allocation Infrastructure**: Refactored `CircularStateBuffer` and `HardwareSuite` to use `inline` callback-based iteration (`forEachMatch`, `forEachSnrSample`, etc.).
    *   **Performance Parity**: Achieved zero-allocation parity (R-ID 392) by eliminating `Sequence` and `Iterator` overhead during high-frequency telemetry retrieval.

## 🟢 Resolved in Oct8.4
*   **Issue #SIMP-1011-2: Acoustic JNI Offloading.**
    *   **JNI Migration**: Migrated `AudioRecord` iterative math (RMS and Peak) to JNI via `AcousticBatch`.
    *   **Latency Optimization**: Reduced JVM math overhead in the 44.1kHz audio path.
    *   **Determinism**: Integrated native spike evaluation with vibration-aware alpha adjustment.
*   **Issue #SIMP-1011-3: Forensic Buffer Consolidation.**
    *   **Memory Efficiency**: Consolidated `EngineSnrSample`, `EngineAcousticSample`, and `EngineSensorSnapshot` into a single `ForensicSample` container.
    *   **Buffer Hardening**: Reduced heap fragmentation and allocation pressure by using a unified circular buffer (`forensicBuffer`) for all high-frequency telemetry data.

## 🟢 Resolved in Oct8.3
*   **Issue #SIMP-1011-1: Native GNSS Batching.**
    *   **JNI Offloading**: Migrated GNSS status evaluation (satellites in view, used in fix, average SNR) to JNI via `GnssHealthBatch`.
    *   **JVM Decoupling**: Reduced JVM overhead in the `onSatelliteStatusChanged` callback by performing batch math natively.
    *   **Robustness**: Implemented automatic fallback to manual calculation if the native library is unavailable.

## 🟢 Resolved in Oct8.2
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.**
    *   **Pipeline Finalization**: Instrumented `LocationProcessor` and `MonitorService` to ensure behavioral rejections (Jamming, Acoustic, Tamper) are promoted into the unified `LocationPendingReason`.
    *   **Telemetry Parity**: Ensured immediate state parity in the evaluation monolith before alarm analysis and remote signaling, resolving the "Lagging Health" defect for remote viewers.
    *   **Strategic Simplification**: Completed the migration of all environment and behavioral health authority to `SentinelValidator`.
