# SOT Master Requirements & Hardening Status (Oct13.4)

## 🟢 Architectural Master Rules (235 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.176 Forensic Checksum Integrity (R-ID 645)**: All forensic traces stored in the off-heap spill-buffer MUST be protected by a CRC32 checksum at the entry level to detect storage-level corruption before DB insertion. (Oct12.12 - Issue #SIMP-1012-3).
*   **1.177 Offline Batch Persistence (R-ID 646)**: Telemetry updates stored during network loss MUST be buffered in memory and flushed to SQLite in batches (min 5 entries or 10s interval) to minimize disk IOPS and write-lock contention. (Oct12.12 - Issue #SIMP-1012-4).
*   **1.178 HUD Legibility & Contrast (R-ID 647)**: All real-time telemetry overlays (HUD) MUST utilize a high-contrast background plate (min 60% opacity) or 2px text shadows to ensure legibility across all map tile variants (Satellite, Street, Dark). (Oct12.16 - Issue #ISSUE-OCT12.16-1).
*   **1.179 Automatic GPS Recovery (R-ID 648)**: The engine MUST implement an automatic recovery path for terminal hardware locks, resetting the acquisition state after 10 minutes of persistent failure. (Oct12.17 - Issue #ISSUE-OCT12.13-3).
*   **1.180 Signaling Latch Protection (R-ID 649)**: Signaling providers MUST implement connection-error event handling and safety timeouts (max 35s) to prevent the system from remaining in a permanent 'Connecting' state. (Oct12.17 - Issue #ISSUE-OCT12.13-7).
*   **1.181 GPS Registration Debounce (R-ID 650)**: Hardware polling interval transitions MUST be debounced by at least 2000ms to prevent chip-level acquisition flutter. (Oct12.17 - Issue #ISSUE-OCT12.13-4).
*   **1.182 Temporal Jitter Role-Isolation (R-ID 651)**: Forensic stability audits MUST isolate jitter metrics per AppRole and ignore intervals exceeding 15s to prevent system suspension (Doze) from being misidentified as hardware clock instability. (Oct12.18 - Issue #ISSUE-OCT12.13-11).
*   **1.183 Yielded Chunked Pruning (R-ID 652)**: Database maintenance (pruning) MUST be performed in yielded chunks without monolithic transactions to prevent I/O pressure from causing UI frame drops or telemetry persistence stalls. (Oct12.18 - Issue #ISSUE-OCT12.13-10).
*   **1.184 Idempotent Signaling Handover (R-ID 653)**: Signaling connection requests MUST be idempotent; if a connection is active or in-progress with identical parameters, redundant socket reconstruction MUST be suppressed to prevent relay "reset-bombing" during network flapping. (Oct12.19 - Issue #ISSUE-OCT12.16-5).
*   **1.185 Context-Aware Freshness (R-ID 654)**: UI staleness indicators MUST explicitly account for the active engine polling tier (e.g., 45s Screen-Off, 60s Stationary) to prevent false-positive failure alerts during intentional acquisition decay. (Oct12.19 - Issue #ISSUE-OCT12.13-2).
*   **1.186 High-Density Touch Targets (R-ID 655)**: Navigation icons in high-density layouts (Portrait HeaderBar) MUST maintain a minimum touch target size of 48dp to ensure usability for safety-critical operations. (Oct12.19 - Issue #ISSUE-OCT12.16-4).
*   **1.187 Centralized State Projection (R-ID 656)**: UI state mapping logic MUST be centralized within the ViewModel (or dedicated UseCases) to ensure all telemetry projections utilize the latest verified polling thresholds and architectural fixes. (Oct11.20 - Issue #ISSUE-OCT12.13-5).
*   **1.188 Contextual HUD Diagnostics (R-ID 657)**: Safety-critical status indicators (HUD) MUST provide explicit contextual labels (e.g., "DATA ERR") when health checks fail, ensuring users can distinguish between link loss and telemetry staleness without opening deeper logs. (Oct11.20 - Issue #ISSUE-OCT12.14-3).
*   **1.189 Mapping Test Coverage (R-ID 658)**: All centralized UI mapping projections MUST be covered by unit tests that verify tier-specific freshness logic and coordinate smoothing to prevent state regression. (Oct12.21 - Issue #SIMP-OCT11.20-S1).
*   **1.190 Unified HUD Orchestration (R-ID 659)**: All global telemetry overlays, navigation controls, and map badges MUST be orchestrated by a single `OverlayManager` to ensure deterministic Z-order stacking and prevent input collisions between fragmented UI layers. (Oct10.3 - Issue #SIMP-OCT12.16-S1).
*   **1.191 Test Data Fidelity (R-ID 660)**: Automated tests for kinematic projections MUST utilize high-fidelity mock data (e.g., non-zero speeds) to ensure all EMA and tier-specific logic branches are correctly traversed. (Oct10.3 - Issue #SIMP-OCT11.20-S1).
*   **1.192 Sequential Persistence Actor (R-ID 661)**: `OfflineRepository` MUST utilize a single-threaded actor (Channel-based) to process telemetry flushes, ensuring zero data loss and strictly sequential I/O during high-frequency bursts. (Oct13.3 - ISSUE-OCT13.3-1).
*   **1.193 Doze Step Verification (R-ID 662)**: Power state integration tests MUST utilize the `dumpsys deviceidle step` sequence to reliably force deep IDLE states across varying manufacturer power policies. (Oct13.3 - ISSUE-OCT13.3-3).

...

## 🟢 Core Hardening Baseline
*   **SOT ID 707**: Sequential Offline Persistence - Refactored OfflineRepository to use a Channel-based actor to resolve burst-load data loss. (Oct13.3 - ISSUE-OCT13.3-1).
*   **SOT ID 706**: Doze Detection Hardening - Updated PowerIntegrationAuditTest with iterative stepping to ensure reliable idle state detection. (Oct13.3 - ISSUE-OCT13.3-3).
*   **SOT ID 705**: Mapping Test Hardening - Verified EMA alpha branches and coordinate smoothing with high-fidelity test data (>0.5 m/s). (Oct10.3 - Issue #SIMP-OCT11.20-S1).
*   **SOT ID 704**: Unified HUD - Merged HeaderBar, GlobalStatusBar, and map overlays into a single OverlayManager to prevent Z-order collisions. (Oct10.3 - Issue #SIMP-OCT12.16-S1).
*   **SOT ID 703**: Mapping Verification - Implemented comprehensive unit tests for Dashboard, HUD, and Map projections. (Oct12.21 - Issue #SIMP-OCT11.20-S1).

...

---

## 🟢 Verification Chapters
*   **Chapter 31.783 (Persistence Reliability)**: PASSED - Verified that OfflineRepository actor correctly persists 100% of telemetry updates during 100Hz bursts. (Oct13.3).
*   **Chapter 31.782 (Power Integration)**: PASSED - Verified that AndroidPowerStateProvider reliably detects Doze mode on Samsung/Pixel devices using the stepped audit sequence. (Oct13.3).
*   **Chapter 31.781 (Test Hardening)**: PASSED - Verified that MainViewModelMappingTest successfully exercises EMA alpha branches for stationary vs moving states. (Oct10.3).
*   **Chapter 31.780 (UI Orchestration)**: PASSED - Verified that UnifiedHUDOverlay correctly stacks Header, Status, and Map tools. (Oct10.3).
...
