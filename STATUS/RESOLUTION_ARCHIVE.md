# 🏛️ Resolution Archive - Sep.29.4

## 🏁 Issue #1378: S21 Hardware Verification & Forensic Probe Failure (Final Resolution)
*   **Resolved**: Sep.29.4
*   **Root Cause**: Compilation caching edge-case involving the Kotlin `const val FORENSIC_SPILL_ENTRY_SIZE`. After the size was increased to 128 bytes in `EngineConstants.kt`, incremental compilation missed updating `ForensicSpillBuffer.kt`. This caused `maxMsgLen` to be incorrectly computed as `<= 0` (due to the older, smaller inlined constant), leading to string truncation (empty strings) and the buffer falling back to `DEFAULT_TRACE_MSG` (`"FORENSIC_TRACE"`).
*   **Remediation**:
    *   **Compilation Reset**: Recompiled `ForensicSpillBuffer.kt` to force the inclusion of the updated `FORENSIC_SPILL_ENTRY_SIZE` (128 bytes). This immediately corrected the `msgLen` computation. All 21 instrumented tests on the S21 hardware now pass.
*   **Significance**: High (Hardware Validation & Test Reliability).
*   **SOT ID**: 547 (S21 Compilation Cache Probe Fix)

## 🏁 Issue #1378: S21 Hardware Verification & Forensic Hardening (Phase 1)
*   **Resolved**: Sep.29.3
*   **Root Cause**: Race conditions and buffer layout mismatches identified during high-performance hardware (S21) validation. Forensic probes were being overwritten or suppressed by concurrent background activity noise.
*   **Remediation**:
    *   **Buffer Schema**: Increased `FORENSIC_SPILL_ENTRY_SIZE` to 128 bytes in `EngineConstants.kt` to prevent metadata/message collisions.
    *   **Persistence**: Advanced `ForensicSpillBuffer` to version 5 and migrated to instance-level locking (`this`) for all read/write operations.
    *   **Signaling**: Implemented `force` bypass in `SignalingForensicLogger` to guarantee test probe capture.
*   **Significance**: High (Hardware Compatibility & Reliability).
*   **SOT ID**: 546 (S21 Verification & Probe Hardening)

## 🏁 Issue #1377: Production Codebase Stabilization & Tracking Alignment
*   **Resolved**: Sep.29.3
*   **Root Cause**: Routine version advancement and forensic tracking synchronization to maintain codebase integrity and auditability.
*   **Remediation**:
    *   **Build Config**: Advanced `versionName` to `Sep.29.3` in `app/build.gradle`.
    *   **Requirements**: Synchronized `SOT_MASTER_REQUIREMENTS.md` with SOT ID 545 and Chapter 31.178.
    *   **Tracking**: Updated `issues.md` dashboard and audit metrics.
*   **Significance**: Medium (Process Integrity).
*   **SOT ID**: 545 (Production Codebase Stabilization)

...
*(Full historical records maintained in SOT Archive)*