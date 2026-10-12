# SOT Master Requirements & Hardening Status (Oct12.17)

## 🏗️ Architectural Master Rules (223 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.176 Forensic Checksum Integrity (R-ID 645)**: All forensic traces stored in the off-heap spill-buffer MUST be protected by a CRC32 checksum at the entry level to detect storage-level corruption before DB insertion. (Oct12.12 - Issue #SIMP-1012-3).
*   **1.177 Offline Batch Persistence (R-ID 646)**: Telemetry updates stored during network loss MUST be buffered in memory and flushed to SQLite in batches (min 5 entries or 10s interval) to minimize disk IOPS and write-lock contention. (Oct12.12 - Issue #SIMP-1012-4).
*   **1.178 HUD Legibility & Contrast (R-ID 647)**: All real-time telemetry overlays (HUD) MUST utilize a high-contrast background plate (min 60% opacity) or 2px text shadows to ensure legibility across all map tile variants (Satellite, Street, Dark). (Oct12.16 - Issue #ISSUE-OCT12.16-1).
*   **1.179 Automatic GPS Recovery (R-ID 648)**: The engine MUST implement an automatic recovery path for terminal hardware locks, resetting the acquisition state after 10 minutes of persistent failure. (Oct12.17 - Issue #ISSUE-OCT12.13-3).
*   **1.180 Signaling Latch Protection (R-ID 649)**: Signaling providers MUST implement connection-error event handling and safety timeouts (max 35s) to prevent the system from remaining in a permanent 'Connecting' state. (Oct12.17 - Issue #ISSUE-OCT12.13-7).
*   **1.181 GPS Registration Debounce (R-ID 650)**: Hardware polling interval transitions MUST be debounced by at least 2000ms to prevent chip-level acquisition flutter. (Oct12.17 - Issue #ISSUE-OCT12.13-4).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 695**: GPS Recovery - Implemented 10m auto-reset for terminal hardware locks. (Oct12.17 - Issue #ISSUE-OCT12.13-3).
*   **SOT ID 694**: Signaling Resilience - Hardened Socket.io connection logic with error events and safety timeouts. (Oct12.17 - Issue #ISSUE-OCT12.13-7).
*   **SOT ID 693**: Threshold Harmonization - Increased UI staleness thresholds to 60s to accommodate long polling intervals. (Oct12.17 - Issue #ISSUE-OCT12.13-1).
*   **SOT ID 692**: HUD Legibility - Added requirement for contrast plates on HUD overlays to support high-detail map backgrounds. (Oct12.16 - Issue #ISSUE-OCT12.16-1).

...

---

## 🏁 Verification Chapters
*   **Chapter 31.775 (UI Alignment)**: PASSED - Verified HUD contrast (75%) and version tag padding (42dp) prevent tile-clash and scale-bar overlap on SM-G990E. (Oct12.17).
*   **Chapter 31.774 (Engine Stability)**: PASSED - Verified automatic recovery from terminal GPS locks and mitigation of signaling connection stalls. (Oct12.17).
*   **Chapter 31.773 (UI Audit)**: FAILED - Verified that HUD telemetry and version tags overlap with map overlays or lack sufficient contrast on SM-G990E. (Oct12.16 - Superceded by 31.775).
*   **Chapter 31.772 (Integrity Audit)**: PASSED - Verified CRC32 corruption detection correctly identifies malformed traces. (Oct12.12).
...
