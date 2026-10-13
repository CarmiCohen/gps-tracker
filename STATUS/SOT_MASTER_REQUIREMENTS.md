# SOT Master Requirements & Hardening Status (Oct13.7)

## ? Architectural Master Rules (248 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.192 Sequential Persistence Actor (R-ID 661)**: `OfflineRepository` MUST utilize a single-threaded actor (Channel-based) to process telemetry flushes, ensuring zero data loss and strictly sequential I/O during high-frequency bursts. (Oct13.3 - ISSUE-OCT13.3-1).
*   **1.193 Doze Step Verification (R-ID 662)**: Power state integration tests MUST utilize the `dumpsys deviceidle step` sequence to reliably force deep IDLE states across varying manufacturer power policies. (Oct13.3 - ISSUE-OCT13.3-3).
*   **1.194 Forensic Concurrency Protection (R-ID 663)**: Forensic drain operations MUST be serialized via a dedicated Mutex (`forensicMutex`) to prevent duplicate database insertions during concurrent background recovery and periodic drain cycles. (Oct13.5 - ISSUE-OCT13.3-2).
*   **1.195 Signature Collision Avoidance (R-ID 664)**: Forensic trace signatures MUST NOT utilize bit-shifting on timestamps that exceed 32 bits (Unix Epoch); deduplication MUST use multi-field composite keys (e.g., `Pair<Long, Int>`) to prevent hash collisions. (Oct13.5 - ISSUE-OCT13.5-2).
*   **1.196 Suspendable Task Isolation (R-ID 665)**: Repositories managing background IO loops MUST implement suspendable `stopBackgroundTasks()` hooks using `cancelAndJoin()` to guarantee strict execution isolation between instrumentation test runs. (Oct13.7 - ISSUE-OCT13.6-1).
*   **1.197 Deterministic Pointer Advancement (R-ID 666)**: Circular buffer drainers MUST advance the read pointer based on the total number of entries processed (`processedCount`), regardless of whether individual items failed validation or were filtered as duplicates, to prevent read-pointer "stalls." (Oct13.7 - ISSUE-OCT13.5-3).

...

## ? Core Hardening Baseline
*   **SOT ID 709**: Forensic Isolation & Pointer Stability - Implemented suspendable Job joining and PeekResult-based pointer advancement to resolve drain deficits (755/1000) and test leakage. (Oct13.7 - ISSUE-OCT13.5-3).
*   **SOT ID 708**: Forensic Pipeline Hardening - Implemented `forensicMutex` and local buffer isolation to resolve CRC corruption and duplication leaks. (Oct13.5 - ISSUE-OCT13.3-2).
*   **SOT ID 707**: Sequential Offline Persistence - Refactored OfflineRepository to use a Channel-based actor to resolve burst-load data loss. (Oct13.3 - ISSUE-OCT13.3-1).

...

---

## ? Verification Chapters
*   **Chapter 31.785 (Forensic Burst Integrity)**: PASSED - Verified 100% capture of 1,000 forensic traces at 100Hz under aggressive test-mode draining. (Oct13.7).
*   **Chapter 31.784 (Extreme Load Batching)**: PASSED - Verified that OfflineRepository handles 1,000 concurrent updates with zero data loss on physical hardware. (Oct13.5).
*   **Chapter 31.783 (Persistence Reliability)**: PASSED - Verified that OfflineRepository actor correctly persists 100% of telemetry updates during 100Hz bursts. (Oct13.3).
...
