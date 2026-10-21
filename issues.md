# Project Issues & Hardening Tracking (Rigorous Audit) - Oct21.02

## 🎯 Current Resumption Focus: UI Cleanup (Overlap & Artifacts).

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT20.33-F4: UI Overlap (Version/Scale) (Low).** Version tag overlaps with the Map Scale bar in the bottom-right corner.
*   **ISSUE-OCT20.33-F5: Debug Artifact Leak (Low).** The `UI_READY` status label is visible in the PLATINUM build and must be suppressed.

---

## 💡 Strategic Simplification Ideas (Ideas: 64)
*   **SIMP-OCT21.01-S1: Unified Peer Heartbeat (Medium).** Consolidate `viewer_pulse`, `viewer_status`, and `location_update` into a single peer-presence state machine in `ConnectivitySuite` to reduce code duplication and forensic noise.
*   **SIMP-OCT21.00-S1: Density-Aware HUD Scaling (Medium).** Implement dynamic padding based on LocalDensity to better handle A15 high-DPI screens. (Significance: Medium)
*   **SIMP-OCT20.32-S1: Settings Relay Abstraction (Medium).**
*   **SIMP-OCT20.32-S2: Role-Based Header Scrim (Low).**
*   **SIMP-OCT20.31-S1: Tracker UI Role-Lock (High).** (Under Review: See #OCT20.33-F3).

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT20.33-F3: Settings Gear Restoration.** Resolved Oct21.02. Reversed R872 purge of Settings gear on Trackers for diagnostic verification.
*   **ISSUE-OCT20.33-F1: VWR Connection Asymmetry.** Resolved Oct21.01. Fixed routing ID logic and added viewer_status handling.
*   **ISSUE-OCT20.33-F2: HUD Layout Compression.** Resolved Oct21.00. Optimized button padding and row spacing for A15 compatibility.
*   **ISSUE-OCT20.31-F1: Forbidden Tracker Authority & Stealth Breach.** Resolved Oct21.00 (Initial Purge).
*   **ISSUE-OCT20.32-F5: Home Point Synchronization Failure.** Resolved Oct21.00.
*   **ISSUE-OCT20.32-F4: Asymmetrical Connection State.** Resolved Oct21.00.
*   **ISSUE-OCT20.32-F3: HUD Header Scrim/Occlusion.** Resolved Oct21.00.
*   **ISSUE-OCT20.32-F2: Version String Alignment.** Resolved Oct21.00.
*   **ISSUE-OCT20.32-F1: Back Button Termination Bug.** Resolved Oct21.00.

---

## 📊 Hardening Progress Dashboard
- **Oct21.02: [SOT Count: 803 (Rules: 313), Open: H:0, M:0, L:2, Ideas: H:2, M:23, L:39, Testing: 9 (Sub-items: 17), QA: 377]**
- **Oct21.01: [SOT Count: 802 (Rules: 313), Open: H:1, M:0, L:2, Ideas: H:2, M:23, L:39, Testing: 9 (Sub-items: 17), QA: 376]**
- **Oct21.00: [SOT Count: 801 (Rules: 312), Open: H:2, M:0, L:2, Ideas: H:2, M:22, L:39, Testing: 9 (Sub-items: 17), QA: 375]**
