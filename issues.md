# Project Issues & Hardening Tracking (Rigorous Audit) - Oct12.17

## 🎯 Current Resumption Focus: Forensic Integrity & Signal Decay Audit.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (2)
*   **ISSUE-OCT12.13-11: Temporal Jitter.** Logcat reveals extreme jitter (1390s) in viewer temporal tracking. Potential `elapsedRealtime` inconsistency on some hardware requires investigation into `AndroidTimeProvider`.
*   **ISSUE-OCT12.16-5: Engine Link Stability.** Although stalls are mitigated, active link stability during cellular handover requires further validation.

### 🟡 Medium Priority (4)
*   **ISSUE-OCT12.13-2: Inconsistent Interval Authority.** `MainViewModel` freshness logic is not fully synchronized with `ServiceBehaviorUseCase`.
*   **ISSUE-OCT12.13-5: Outdated Dead Code (UiStateCoordinator).** Duplicated mapping logic lacks critical Oct11.7 fixes.
*   **ISSUE-OCT12.13-10: I/O Pressure & Pruning Latency.** Database pruning spikes of up to 3.4s causing minor UI frame drops.
*   **ISSUE-OCT12.16-4: HeaderBar Portrait Density.** Top navigation icons have insufficient touch target spacing and size.

### 🔵 Low Priority (2)
*   **ISSUE-OCT12.14-3: HUD Error Icon Ambiguity.** Red exclamation icon at top-right lacks labels or contextual hints.
*   **ISSUE-OCT12.16-6: Navigation Inset Buffer.** Version tag lacks vertical buffer from system navigation bar.

---

## 💡 Strategic Simplification Ideas (Ideas: 14)
*   **SIMP-OCT12.17-S1: Periodic GPS Re-Zeroing (High).** Implement a silent background GPS provider restart every 6 hours if accuracy remains > 30m to clear internal chip thermal drift.
*   **SIMP-OCT12.16-S1: Unified HUD Overlay (High).** Merge HeaderBar, GlobalStatusBar, and map overlays into a single OverlayManager.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **ISSUE-OCT12.16-1: HUD Telemetry Illegibility.** Increased background opacity to 0.75f. Resolved Oct12.17.
*   **ISSUE-OCT12.13-1: GPS UI Staleness Mismatch.** Increased thresholds to 60s. Resolved Oct12.17.
*   **ISSUE-OCT12.13-3: GPS Hardware Lock Recovery.** Implemented 10m auto-reset. Resolved Oct12.17.
*   **ISSUE-OCT12.13-7: Signaling Latch Stall.** Hardened Socket.io connection logic. Resolved Oct12.17.
*   **ISSUE-OCT12.13-6: Viewer Position Bug.** Fixed smoothing logic in MainViewModel. Resolved Oct12.17.
*   **ISSUE-OCT12.16-3: Map Scale UI Overlap.** Shifted version tag padding. Resolved Oct12.17.

---

## 📊 Hardening Progress Dashboard
- **Oct12.17: [SOT Count: 376 (Rules: 223), Open: H:2, M:4, L:2, Ideas: H:2, M:6, L:6, Testing: 3 (Sub-items: 15), QA: 284]**
- **Oct12.16: [SOT Count: 373 (Rules: 220), Open: H:7, M:8, L:3, Ideas: H:1, M:6, L:6, Testing: 3 (Sub-items: 15), QA: 284]**
- **Oct12.14: [SOT Count: 373 (Rules: 220), Open: H:7, M:8, L:3, Ideas: H:1, M:6, L:5, Testing: 3 (Sub-items: 15), QA: 284]**
- **Oct12.13: [SOT Count: 369 (Rules: 219), Open: H:6, M:5, L:1, Ideas: H:0, M:6, L:5, Testing: 3 (Sub-items: 15), QA: 284]**
