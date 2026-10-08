# 📜 Resolution Archive

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

## 🟢 Resolved in Oct8.1
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.**
    *   **Sentinel Hardening**: Updated `LocationSentinel.checkPhysicalTamperInternal` to ensure all behavioral `TAMPER` rejections (Tilt, Shock, Baro, Light, Proximity) set a corresponding `LocationPendingReason.JAMMER_SUSPICION`.
    *   **Unified Health Evaluation**: Finalized the promotion path from `SentinelResult` to `ProcessedLocation` and into the telemetry signaling pipeline, ensuring consistent behavioral health visibility for remote viewers.
    *   **Architecture Consolidation**: Completed the purge of redundant GNSS evaluation in `HardwareSuite`, centralizing all health logic in `SentinelValidator` (R-ID 680).

## 🟢 Resolved in Oct7.11
... (Historical entries truncated for brevity)
