# Forensic Handover (Sep.29.3 - #1378 IN PROGRESS)

## 🎯 Current System State
*   **Version**: Sep.29.3 | **Status**: S21 Verification Ongoing (20/21 Passed).
*   **Core Remediation**: 
    *   **Buffer Schema Hardening**: Increased `FORENSIC_SPILL_ENTRY_SIZE` to 128 bytes in `core/engine/.../EngineConstants.kt` (Line 52) and advanced `ForensicSpillBuffer` to **version 5** to prevent CRC overwrites and technical metadata collision.
    *   **Concurrency Hardening**: Switched `ForensicSpillBuffer.peekToEntities` to instance-level locking (`synchronized(this)`) in `app/.../ForensicSpillBuffer.kt` (Line 229) to prevent read/write races on high-performance S21 cores.
    *   **Rule 1.55 Enforcement**: Implemented a `force` parameter in `SignalingForensicLogger.kt` (Lines 61, 72) to ensure instrumented test probes bypass forensic timing windows.

## 🚀 Active Task Snapshot: Issue #1378
*   **Failing Test**: `ProductionReadinessAuditTest > verifySignalingLifecycleProbes` (Assertion failure at Line 112) consistently fails on Samsung S21 (`R5CRC14PG4F`).
*   **Forensic Investigation**: Probes `TEST_UP` and `TEST_FAIL` are logged via `force=true` but are not appearing in the audit peek results within the 3s polling window.
*   **Critical Resumption Nodes**: 
    1.  **Memory Visibility**: Investigate if `MappedByteBuffer` visibility delay on S21 requires an explicit `load()` or `force()` call before peeking in `ForensicSpillBuffer.kt`.
    2.  **Sanitization Collision**: Check `ForensicSanitizer.kt` (Line 16) to see if the randomized `testInterface` (e.g., `wlan49`) is being inadvertently scrubbed as a hardware identifier.
    3.  **Drainer Inhibition**: Confirm `LogRepository.setForensicStallSimulation(true)` is effectively blocking the async drainer loop in `LogRepository.kt` (Line 137) to prevent probe eviction before the audit peek.

---

## 🛡️ Core Architecture Blueprint
1.  **Forensic Integrity**: 128-byte schema (v5) with absolute synchronization for cross-hardware reliability.
2.  **Unified Service Authority**: `MonitorService` manages lifecycle; `ForensicSpillBuffer` handles persistence.
3.  **Traceability Rule**: Issue #1378 must be linked to all subsequent S21 stabilization commits.

---

## 📊 Hardening Progress Dashboard
- **Sep.29.3: [SOT Count: 207 (Rules: 66), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 16), QA: 296]**
