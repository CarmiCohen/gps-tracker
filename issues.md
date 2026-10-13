# Project Issues & Hardening Tracking (Rigorous Audit) - Oct13.3

## 🎯 Current Resumption Focus: Offline Persistence & Forensic Integrity.

## ⚠️ Open Gaps & Unfinished Integration Points

### 🔴 High Priority (1)
*   **ISSUE-OCT13.3-1: OfflineRepository Data Loss (High).** Smoke tests detected loss of 4/50 updates during high-frequency bursts. Suspected race condition in asynchronous `flushBuffer` triggering or silent SQLite insertion failures.
*   **ISSUE-OCT13.3-2: ForensicSpillBuffer CRC Mismatch (High).** Logcat reports checksum failures in forensic traces. Indicates potential memory-mapped buffer corruption or synchronization issues between optimized writes and entity peeking.

### 🟡 Medium Priority (1)
*   **ISSUE-OCT13.3-3: PowerIntegrationAudit Failure (Medium).** Doze state detection failed on Samsung test device (R5CRC14PG4F). Likely due to aggressive manufacturer power policies or timing jitter in `force-idle` propagation.

### 🔵 Low Priority (0)

---

## 💡 Strategic Simplification Ideas (Ideas: 17)
*   **SIMP-OCT13.3-S1: Sequential Persistence Actor (High).** Replace multiple `scope.launch` flushes in `OfflineRepository` with a single `Channel`-based actor to guarantee sequential, non-overlapping disk I/O and prevent trigger "swallowing".
*   **SIMP-OCT12.17-S1: Periodic GPS Re-Zeroing (High).** Implement a silent background GPS provider restart every 6 hours if accuracy remains > 30m to clear internal chip thermal drift.
*   **SIMP-OCT12.18-S1: Pruning Scheduler (Medium).** Move background pruning to a dedicated `WorkManager` task to further isolate I/O pressure from the main service process.
*   **SIMP-OCT12.18-S2: Forensic Role Auto-Mapping (Low).** Centralize `AppRole` assignment in `BaseMonitorService` to eliminate manual `setActiveRole` calls in subclasses.
*   **SIMP-OCT13.1-S1: State Slicing Hardening (Medium).** Evaluate moving HUD-specific state flows into a dedicated `HudStateUseCase` to further reduce `MainViewModel` complexity.

---

## ✅ Resolved Traceability & Metadata Issues
*   **SIMP-OCT12.19-S1: Offline Repository Batching.** Implemented memory-mapped buffering and bulk insertion. (Audit pending resolution of ISSUE-OCT13.3-1).
*   **SIMP-OCT11.20-S1: Mapping Test Hardening.** Hardened unit tests for `MainViewModel` smoothing projections by ensuring test data fidelity (non-stationary speeds) to trigger correct EMA branches. Resolved Oct10.3.
*   **SIMP-OCT12.16-S1: Unified HUD Overlay.** Merged HeaderBar, GlobalStatusBar, and map overlays into a unified `OverlayManager` to prevent Z-order collisions and simplify state management. Resolved Oct10.3.

---

## 📈 Hardening Progress Dashboard
- **Oct13.3: [SOT Count: 388 (Rules: 235), Open: H:2, M:1, L:0, Ideas: H:2, M:8, L:7, Testing: 6 (Sub-items: 30), QA: 286]**
- **Oct10.3: [SOT Count: 386 (Rules: 233), Open: H:0, M:0, L:0, Ideas: H:1, M:8, L:7, Testing: 5 (Sub-items: 25), QA: 285]**
- **Oct12.21: [SOT Count: 384 (Rules: 231), Open: H:0, M:0, L:0, Ideas: H:2, M:7, L:7, Testing: 4 (Sub-items: 20), QA: 284]**
