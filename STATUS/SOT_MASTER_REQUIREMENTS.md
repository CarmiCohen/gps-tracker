# SOT Master Requirements & Hardening Status (Oct13.20)

## 🏗️ Architectural Master Rules (252 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.200 Constraint-Aware Pruning Scheduler (R-ID 669)**: Database pruning MUST be executed via a dedicated `WorkManager` task (`PruningWorker`) with `DeviceIdle` and `StorageNotLow` constraints to prevent foreground I/O contention and ensure reliable retention management. (Oct12.8 - SIMP-OCT12.18-S1).
*   **1.201 Managed Signaling Lifecycle (R-ID 670)**: Signaling dispatchers MUST NOT utilize delicate coroutine APIs (e.g., `isClosedForSend`) for lifecycle checks; state management MUST be handled via managed `Job` tracking and explicit exception handling (`ClosedReceiveChannelException`) to prevent silent resource leaks during service re-initialization. (Oct13.20 - ISSUE-OCT13.20-H1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 713**: Signaling Lifecycle Hardening - Replaced delicate API usages with robust job-managed transitions. (Oct13.20 - ISSUE-OCT13.20-H1).
*   **SOT ID 712**: Pruning Scheduler Integration - Migrated proactive pruning to WorkManager to improve UI responsiveness and respect system power constraints. (Oct12.8 - SIMP-OCT12.18-S1).

...

---

## 🔍 Verification Chapters
*   **Chapter 31.788 (Signaling Lifecycle)**: PASSED - Verified that `SmartSignalingDispatcher` correctly terminates and restarts background loops without channel-state race conditions. (Oct13.20).
*   **Chapter 31.787 (Pruning Scheduler Resilience)**: PASSED - Verified that database pruning triggers correctly via WorkManager under simulated device idle conditions. (Oct12.8).
...
