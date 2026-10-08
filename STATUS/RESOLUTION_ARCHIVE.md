# 📜 Resolution Archive

## 🟢 Resolved in Oct8.1
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.**
    *   **Sentinel Hardening**: Updated `LocationSentinel.checkPhysicalTamperInternal` to ensure all behavioral `TAMPER` rejections (Tilt, Shock, Baro, Light, Proximity) set a corresponding `LocationPendingReason.JAMMER_SUSPICION`.
    *   **Unified Health Evaluation**: Finalized the promotion path from `SentinelResult` to `ProcessedLocation` and into the telemetry signaling pipeline, ensuring consistent behavioral health visibility for remote viewers.
    *   **Architecture Consolidation**: Completed the purge of redundant GNSS evaluation in `HardwareSuite`, centralizing all health logic in `SentinelValidator` (R-ID 680).

## 🟢 Resolved in Oct7.11
*   **Issue #SIMP-1007-17: Strategic Simplification.**
    *   **Consolidation**: Centralized GNSS health evaluation (Signal Loss, Gaps, Stalls) and behavioral anomalies (Jamming, Acoustic Violations) into `SentinelValidator.evaluateLocationPendingReason`.
    *   **Logic Migration**: Purged redundant manual status evaluation from `HardwareSuite.kt`, replacing it with delegation to the centralized evaluator.
    *   **Pipeline Promotion**: Instrumented `LocationProcessor.processGpsPoint` to promote behavioral rejections from the sentinel result into the unified `LocationPendingReason`.
    *   **Priority Resolution**: Integrated `getHigherPriorityReason` into `TelemetryAggregator` and `TelemetryMapper` to ensure the most critical health issue is signaled when multiple conditions overlap (e.g., Jamming vs. Signal Loss).

## 🟢 Resolved in Oct7.10
*   **Issue #SIMP-1010-3: SNR Decay Modeling.**
...
