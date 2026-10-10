# 📜 Resolution Archive

## 🟢 Resolved in Oct10.1
*   **Issue #BUILD-RESTORE: KAPT/Hilt Metadata Recovery.**
    *   **Recovery**: Successfully exited the Oct8.16 "Error module" build loop by rolling back to the Oct8.1 stable baseline (64faffd).
    *   **Integrity Verification**: Confirmed `:app:assembleDebug` parity and structural consistency of core interfaces (`Locatable`, `DomainEvent`).
    *   **Initialization**: Established Oct10.1 as the new hardening branch for controlled re-integration of native offloading.

## 🟢 Resolved in Oct8.1
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.**
    *   **Sentinel Hardening**: Updated `LocationSentinel.checkPhysicalTamperInternal` to ensure all behavioral `TAMPER` rejections set a corresponding `LocationPendingReason.JAMMER_SUSPICION`.
    *   **Unified Health Evaluation**: Finalized the promotion path from `SentinelResult` to `ProcessedLocation` and into the telemetry signaling pipeline.
    *   **Architecture Consolidation**: Completed the purge of redundant GNSS evaluation in `HardwareSuite`, centralizing logic in `SentinelValidator`.

## 🟢 Resolved in Oct7.11
*   **Issue #SIMP-1007-17: Strategic Simplification.**
    *   **Consolidation**: Centralized GNSS health evaluation and behavioral anomalies into `SentinelValidator.evaluateLocationPendingReason`.
...
