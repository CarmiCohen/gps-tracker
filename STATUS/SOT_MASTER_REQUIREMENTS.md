# SOT Master Requirements & Hardening Status (Oct14.1)

## ✅ Architectural Master Rules (252 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.200 Constraint-Aware Pruning Scheduler (R-ID 669)**: Database pruning MUST be executed via a dedicated `WorkManager` task (`PruningWorker`) with `DeviceIdle` and `StorageNotLow` constraints to prevent foreground I/O contention and ensure reliable retention management. (Oct14.1 - SIMP-OCT12.18-S1).
*   **1.201 Managed Signaling Lifecycle (R-ID 670)**: Signaling dispatchers MUST NOT utilize delicate coroutine APIs (e.g., `isClosedForSend`) for lifecycle checks; state management MUST be handled via managed `Job` tracking and explicit exception handling (`ClosedReceiveChannelException`) to prevent silent resource leaks during service re-initialization. (Oct14.1 - ISSUE-OCT13.20-H1).

...

## ✅ Core Hardening Baseline
*   **SOT ID 715**: Build Integrity Restoration - Restored critical engine thresholds and synchronized Activity-level launchers via `UiEffect` bus to finalize the Pure Event UI model. (Oct14.1 - ISSUE-OCT13.21-H1, SIMP-OCT13.21-S2).
*   **SOT ID 714**: Code Hygiene & Signature Pruning - Standardized UI signatures to pass single `onMainEvent` handlers, reducing prop-drilling and architectural churn. (Oct14.1 - ISSUE-OCT13.20-L1).
*   **SOT ID 713**: Signaling Lifecycle Hardening - Replaced delicate API usages with robust job-managed transitions. (Oct14.1 - ISSUE-OCT13.20-H1).

...

---

## 🔍 Verification Chapters
*   **Chapter 31.790 (Build Restoration)**: PASSED - Verified that the project builds successfully after restoring `EngineConstants.kt` and refactoring `MainAppContent.kt` to a parameterless signature. (Oct14.1).
*   **Chapter 31.789 (Code Hygiene)**: PASSED - Verified that `ViewerScreen` and `TrackerScreen` function correctly with pruned parameter signatures and that unused engine constants have been safely retired. (Oct14.1).
...
