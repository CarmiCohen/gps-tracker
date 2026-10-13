# SOT Master Requirements & Hardening Status (Oct12.8)

## ? Architectural Master Rules (251 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.192 Sequential Persistence Actor (R-ID 661)**: `OfflineRepository` MUST utilize a single-threaded actor (Channel-based) to process telemetry flushes, ensuring zero data loss and strictly sequential I/O during high-frequency bursts. (Oct13.3 - ISSUE-OCT13.3-1).
*   **1.193 Doze Step Verification (R-ID 662)**: Power state integration tests MUST utilize the `dumpsys deviceidle step` sequence to reliably force deep IDLE states across varying manufacturer power policies. (Oct13.3 - ISSUE-OCT13.3-3).
*   **1.194 Forensic Concurrency Protection (R-ID 663)**: Forensic drain operations MUST be serialized via a dedicated Mutex (`forensicMutex`) to prevent duplicate database insertions during concurrent background recovery and periodic drain cycles. (Oct13.5 - ISSUE-OCT13.3-2).
*   **1.195 Signature Collision Avoidance (R-ID 664)**: Forensic trace signatures MUST NOT utilize bit-shifting on timestamps that exceed 32 bits (Unix Epoch); deduplication MUST use multi-field composite keys (e.g., `Pair<Long, Int>`) to prevent hash collisions. (Oct13.5 - ISSUE-OCT13.5-2).
*   **1.196 Suspendable Task Isolation (R-ID 665)**: Repositories managing background IO loops MUST implement suspendable `stopBackgroundTasks()` hooks using `cancelAndJoin()` to guarantee strict execution isolation between instrumentation test runs. (Oct13.7 - ISSUE-OCT13.6-1).
*   **1.197 Deterministic Pointer Advancement (R-ID 666)**: Circular buffer drainers MUST advance the read pointer based on the total number of entries processed (`processedCount`), regardless of whether individual items failed validation or were filtered as duplicates, to prevent read-pointer "stalls." (Oct13.7 - ISSUE-OCT13.5-3).
*   **1.198 Reactive GPS Re-Zeroing (R-ID 667)**: The `HardwareSuite` MUST implement a reactive `gpsRestartTrigger` to silently unregister and re-register the location provider every 6 hours if accuracy remains > 30m, ensuring chip thermal drift is cleared without fragile state manipulation. (Oct13.8 - SIMP-OCT12.17-S1).
*   **1.199 Staggered UI Freshness (R-ID 668)**: UI staleness and signal loss thresholds MUST be scaled to at least 90s (`VIEWER_SIGNAL_LOSS_THRESHOLD_MS`) to accommodate the 45s staggered polling interval used in Screen-Off/Power-Save modes and prevent false alarm triggers. (Oct13.8 - ISSUE-OCT12.13-1).
*   **1.200 Constraint-Aware Pruning Scheduler (R-ID 669)**: Database pruning MUST be executed via a dedicated `WorkManager` task (`PruningWorker`) with `DeviceIdle` and `StorageNotLow` constraints to prevent foreground I/O contention and ensure reliable retention management. (Oct12.8 - SIMP-OCT12.18-S1).

...

## ? Core Hardening Baseline
*   **SOT ID 712**: Pruning Scheduler Integration - Migrated proactive pruning to WorkManager to improve UI responsiveness and respect system power constraints. (Oct12.8 - SIMP-OCT12.18-S1).
*   **SOT ID 711**: UI Staleness & Signal Loss Hardening - Increased thresholds to 90s to support 45s staggered polling. (Oct13.8 - ISSUE-OCT12.13-1).
*   **SOT ID 710**: Reactive GPS Re-Zeroing - Implemented silent background GPS provider restart via reactive trigger. (Oct13.8 - SIMP-OCT12.17-S1).

...

---

## ? Verification Chapters
*   **Chapter 31.787 (Pruning Scheduler Resilience)**: PASSED - Verified that database pruning triggers correctly via WorkManager under simulated device idle conditions. (Oct12.8).
*   **Chapter 31.786 (Staggered UI Freshness)**: PASSED - Verified that increasing VIEWER_SIGNAL_LOSS_THRESHOLD_MS to 90s prevents false red-screen alerts during 45s screen-off polling. (Oct13.8).
...
