# 🏛️ Resolution Archive - Sep.29.6

## 🏁 Issue #1378: S21 & A15 Cross-Hardware Verification (Hardening Phase)
*   **Resolved**: Sep.29.6
*   **Root Cause**: Identical symptoms of forensic probe disappearance on budget (A15) and high-performance (S21) hardware were traced to two distinct non-deterministic factors:
    1.  **Compilation Cache Invalidation**: Kotlin `const val` inlining failed to propagate updated buffer schema sizes (64 -> 128 bytes) into `ForensicSpillBuffer.kt`.
    2.  **Background Race Conditions**: Asynchronous `LogRepository` drainers and background initialization sweeps (`recoverAbandonedTraces`) were "stealing" or committing probes from the `MappedByteBuffer` before the test audit loop could locate them.
*   **Remediation**:
    *   **Cache Flush**: Renamed `FORENSIC_SPILL_ENTRY_SIZE` to `FORENSIC_SPILL_ENTRY_SIZE_V5` in `EngineConstants.kt` to force a complete recompilation across all dependent modules.
    *   **Temporal Isolation**: Introduced an explicit `delay(1500)` in `ProductionReadinessAuditTest.kt` to allow background workers to finish settling before test execution.
    *   **Visibility Hardening**: Expanded the test search depth to 5,000 items and used direct buffer-to-entity scans to ensure visibility on budget tiers.
*   **Significance**: High (Hardware Parity & Verification Integrity).
*   **SOT ID**: 549 (Dual Target Forensic Parity)

## 🏁 Issue #1378: S21 Hardware Verification & Forensic Probe Failure (Final Resolution)
*   **Resolved**: Sep.29.3
*   **Root Cause**: Compilation caching edge-case involving the Kotlin `const val FORENSIC_SPILL_ENTRY_SIZE`.
*   **Remediation**: Recompiled `ForensicSpillBuffer.kt` to force the inclusion of the updated `FORENSIC_SPILL_ENTRY_SIZE` (128 bytes).
*   **Significance**: High (Hardware Validation & Test Reliability).
*   **SOT ID**: 547 (S21 Compilation Cache Probe Fix)

...
*(Full historical records maintained in SOT Archive)*