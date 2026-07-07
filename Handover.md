# Forensic Handover - v9.2.2 (Intelligent Uncertainty UX)

## 📌 Status: Stable / Build PASS / UX Enrichment Implemented
This cycle remediates the opacity of the "Location Pending" state by providing specific contextual reasons for Bayesian uncertainty expansion.

### 🟢 Completed: Issue #326 (Intelligent Uncertainty UX Mapping)
*   **Engine Hardening**: 
    *   Added `GPS_GAP` to `LocationPendingReason` enum in `EngineModels.kt` to distinguish environmental signal loss from hardware stalls.
    *   Implemented `getHigherPriorityReason` in `TelemetryAggregator.kt` to ensure critical forensic markers (e.g., `JAMMER_SUSPICION`) are not overwritten by lower-priority reasons during ribbon aggregation.
    *   Updated `MainAlarmLogic.kt` to propagate the specific `locationPendingReason` name into the `technicalDetails` of forensic violation reports.
*   **Service Logic**:
    *   Updated `TrackerService.kt` to identify and broadcast `GPS_GAP` when the elapsed time since the last valid fix exceeds thresholds, ensuring the UI accurately reflects the cause of uncertainty growth.
*   **Documentation Remediation**:
    *   Resolved an ID collision where Issue #326 was incorrectly attributed to Requirement R917 (Update Smoothness). 

### 🟢 Completed: Requirement R990 (Stationary Anchor Hard-Lock)
*   **Critical Remediation (Issue #018)**: 
    *   Implemented coordinate clamping in `LocationProcessor.kt` using `parkingAnchorPoint`. (v9.2.1)

### 🛠 Instructions for Resumption
1.  **Verification of #326**: Enter a tunnel or area with poor GPS. Verify that the HUD display transitions from "±X" to the specific reason string (e.g., "GPS GAP") and that the orange "P" (Pending) indicator appears.
2.  **Aggregation Test**: Induce a Jammer Suspicion followed by a simple Signal Loss. Verify that the historical ribbon record preserves the `JAMMER_SUSPICION` due to its higher priority.
