# SOT Master Requirements & Hardening Status (Oct12.15)

## 🏗️ Architectural Master Rules (220 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.176 Forensic Checksum Integrity (R-ID 645)**: All forensic traces stored in the off-heap spill-buffer MUST be protected by a CRC32 checksum at the entry level to detect storage-level corruption before DB insertion. (Oct12.12 - Issue #SIMP-1012-3).
*   **1.177 Offline Batch Persistence (R-ID 646)**: Telemetry updates stored during network loss MUST be buffered in memory and flushed to SQLite in batches (min 5 entries or 10s interval) to minimize disk IOPS and write-lock contention. (Oct12.12 - Issue #SIMP-1012-4).
*   **1.178 HUD Legibility & Contrast (R-ID 647)**: All real-time telemetry overlays (HUD) MUST utilize a high-contrast background plate (min 60% opacity) or 2px text shadows to ensure legibility across all map tile variants (Satellite, Street, Dark). (Oct12.15 - Issue #ISSUE-OCT12.15-1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 692**: HUD Legibility - Added requirement for contrast plates on HUD overlays to support high-detail map backgrounds. (Oct12.15 - Issue #ISSUE-OCT12.15-1).
*   **SOT ID 691**: Batch Persistence - Implemented background batch-flush loop in OfflineRepository to reduce SQLite contention. (Oct12.12 - Issue #SIMP-1012-4).

...

---

## 🏁 Verification Chapters
*   **Chapter 31.773 (UI Audit)**: FAILED - Verified that HUD telemetry and version tags overlap with map overlays or lack sufficient contrast on SM-G990E. (Oct12.15 - Issue #ISSUE-OCT12.15-1, #ISSUE-OCT12.15-5).
*   **Chapter 31.772 (Integrity Audit)**: PASSED - Verified CRC32 corruption detection correctly identifies malformed traces. (Oct12.12).
...
