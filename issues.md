# Project Issues & Hardening Tracking (Rigorous Audit) - Oct10.3

## 🎯 Current Resumption Focus: Forensic Integrity & Signal Decay Audit.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (0)

### 🟡 Medium Priority (0)

### 🔵 Low Priority (0)

---

## 💡 Strategic Simplification Ideas (Ideas: 16)
*   **SIMP-OCT12.17-S1: Periodic GPS Re-Zeroing (High).** Implement a silent background GPS provider restart every 6 hours if accuracy remains > 30m to clear internal chip thermal drift.
*   **SIMP-OCT12.18-S1: Pruning Scheduler (Medium).** Move background pruning to a dedicated `WorkManager` task to further isolate I/O pressure from the main service process.
*   **SIMP-OCT12.18-S2: Forensic Role Auto-Mapping (Low).** Centralize `AppRole` assignment in `BaseMonitorService` to eliminate manual `setActiveRole` calls in subclasses.
*   **SIMP-OCT13.1-S1: State Slicing Hardening (Medium).** Evaluate moving HUD-specific state flows into a dedicated `HudStateUseCase` to further reduce `MainViewModel` complexity.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **SIMP-OCT12.16-S1: Unified HUD Overlay.** Merged HeaderBar, GlobalStatusBar, and map overlays into a unified `OverlayManager` to prevent Z-order collisions and simplify state management. Resolved Oct10.3.
*   **SIMP-OCT11.20-S1: Mapping Verification.** Added comprehensive unit tests for `MainViewModel` mapping projections to ensure parity with engine tiers. Resolved Oct12.21.
*   **ISSUE-OCT12.16-6: Navigation Inset Buffer.** Hardened map overlays with explicit navigation bar inset padding to clear system gesture handles. Resolved Oct11.20.
*   **ISSUE-OCT12.13-5: Outdated Dead Code (UiStateCoordinator).** Purged redundant mapping logic to eliminate state projection divergence. Resolved Oct11.20.
*   **ISSUE-OCT12.14-3: HUD Error Icon Ambiguity.** Added "DATA ERR" contextual labels to the global status bar for forensic clarity. Resolved Oct11.20.
*   **ISSUE-OCT12.16-5: Engine Link Stability.** Hardened CommunicationManager.connect() with idempotency checks. Resolved Oct12.19.
*   **ISSUE-OCT12.13-2: Inconsistent Interval Authority.** Harmonized MainViewModel freshness logic with dynamic engine polling tiers. Resolved Oct12.19.
*   **ISSUE-OCT12.16-4: HeaderBar Portrait Density.** Increased navigation icon touch targets to 48dp. Resolved Oct12.19.
*   **ISSUE-OCT12.13-11: Temporal Jitter.** Hardened ForensicAuditor with role-isolation and gap protection (>15s). Resolved Oct12.18.
*   **ISSUE-OCT12.13-10: I/O Pressure & Pruning Latency.** Refactored MainRepository to use yielded chunked pruning. Resolved Oct12.18.

---

## 📊 Hardening Progress Dashboard
- **Oct10.3: [SOT Count: 385 (Rules: 232), Open: H:0, M:0, L:0, Ideas: H:1, M:8, L:7, Testing: 4 (Sub-items: 20), QA: 284]**
- **Oct12.21: [SOT Count: 384 (Rules: 231), Open: H:0, M:0, L:0, Ideas: H:2, M:7, L:7, Testing: 4 (Sub-items: 20), QA: 284]**
- **Oct11.20: [SOT Count: 383 (Rules: 230), Open: H:0, M:0, L:0, Ideas: H:2, M:7, L:8, Testing: 3 (Sub-items: 15), QA: 284]**
- **Oct12.19: [SOT Count: 381 (Rules: 228), Open: H:0, M:1, L:2, Ideas: H:2, M:7, L:7, Testing: 3 (Sub-items: 15), QA: 284]**
- **Oct12.18: [SOT Count: 378 (Rules: 225), Open: H:1, M:3, L:2, Ideas: H:2, M:7, L:7, Testing: 3 (Sub-items: 15), QA: 284]**
- **Oct12.17: [SOT Count: 376 (Rules: 223), Open: H:2, M:4, L:2, Ideas: H:2, M:6, L:6, Testing: 3 (Sub-items: 15), QA: 284]**
