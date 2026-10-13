# SOT Master Requirements & Hardening Status (Oct13.5)

## 🟢 Architectural Master Rules (237 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.192 Sequential Persistence Actor (R-ID 661)**: `OfflineRepository` MUST utilize a single-threaded actor (Channel-based) to process telemetry flushes, ensuring zero data loss and strictly sequential I/O during high-frequency bursts. (Oct13.3 - ISSUE-OCT13.3-1).
*   **1.193 Doze Step Verification (R-ID 662)**: Power state integration tests MUST utilize the `dumpsys deviceidle step` sequence to reliably force deep IDLE states across varying manufacturer power policies. (Oct13.3 - ISSUE-OCT13.3-3).
*   **1.194 Forensic Concurrency Protection (R-ID 663)**: Forensic drain operations MUST be serialized via a dedicated Mutex (`forensicMutex`) to prevent duplicate database insertions during concurrent background recovery and periodic drain cycles. (Oct13.5 - ISSUE-OCT13.3-2).
*   **1.195 Signature Collision Avoidance (R-ID 664)**: Forensic trace signatures MUST NOT utilize bit-shifting on timestamps that exceed 32 bits (Unix Epoch); deduplication MUST use multi-field composite keys (e.g., `Pair<Long, Int>`) to prevent hash collisions. (Oct13.5 - ISSUE-OCT13.5-2).

...

## 🟢 Core Hardening Baseline
*   **SOT ID 708**: Forensic Pipeline Hardening - Implemented `forensicMutex` and local buffer isolation to resolve CRC corruption and duplication leaks. (Oct13.5 - ISSUE-OCT13.3-2).
*   **SOT ID 707**: Sequential Offline Persistence - Refactored OfflineRepository to use a Channel-based actor to resolve burst-load data loss. (Oct13.3 - ISSUE-OCT13.3-1).
*   **SOT ID 706**: Doze Detection Hardening - Updated PowerIntegrationAuditTest with iterative stepping to ensure reliable idle state detection. (Oct13.3 - ISSUE-OCT13.3-3).

...

---

## 🟢 Verification Chapters
*   **Chapter 31.784 (Extreme Load Batching)**: PASSED - Verified that OfflineRepository handles 1,000 concurrent updates with zero data loss on physical hardware. (Oct13.5).
*   **Chapter 31.783 (Persistence Reliability)**: PASSED - Verified that OfflineRepository actor correctly persists 100% of telemetry updates during 100Hz bursts. (Oct13.3).
*   **Chapter 31.782 (Power Integration)**: PASSED - Verified that AndroidPowerStateProvider reliably detects Doze mode on Samsung/Pixel devices using the stepped audit sequence. (Oct13.3).
...
