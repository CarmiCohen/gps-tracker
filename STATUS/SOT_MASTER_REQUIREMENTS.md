# SOT Master Requirements & Hardening Status (Oct14.1)

## ✅ Architectural Master Rules (252 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.200 Constraint-Aware Pruning Scheduler (R-ID 669)**: Database pruning MUST be executed via a dedicated `WorkManager` task (`PruningWorker`) with `DeviceIdle` and `StorageNotLow` constraints to prevent foreground I/O contention and ensure reliable retention management. (Oct14.1 - SIMP-OCT12.18-S1).
*   **1.201 Managed Signaling Lifecycle (R-ID 670)**: Signaling dispatchers MUST NOT utilize delicate coroutine APIs (e.g., `isClosedForSend`) for lifecycle checks; state management MUST be handled via managed `Job` tracking and explicit exception handling (`ClosedReceiveChannelException`) to prevent silent resource leaks during service re-initialization. (Oct14.1 - ISSUE-OCT13.20-H1).

...

## ✅ Core Hardening Baseline
*   **SOT ID 717**: Comprehensive Hardening Audit - Validated project stability through 97 total tests (43 engine, 29 app unit, 25 instrumented) to confirm Oct14.1 production readiness. (Oct14.1 - ISSUE-OCT14.3-H1).
*   **SOT ID 716**: Hardening & Stress Verification - Restored and verified full audit suite (Forensic, Storage, Power, Heat) to ensure Oct14.1 stability. (Oct14.1 - ISSUE-OCT14.2-H1).

...

---

## 🔍 Verification Chapters
*   **Chapter 31.792 (Audit Finalization)**: PASSED - Verified all hardening and stress tests pass on target hardware and JVM. (Oct14.1).
*   **Chapter 31.791 (Hardening Verification)**: PASSED - Verified all hardening and stress tests (25 instrumented, 43 engine unit) pass after signature restoration. (Oct14.1).
...
