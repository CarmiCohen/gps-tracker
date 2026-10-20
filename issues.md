# Project Issues & Hardening Tracking (Rigorous Audit) - Oct20.33

## 🎯 Current Resumption Focus: A15 Deployment & HUD Optimization.

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT20.33-F1: VWR Connection Asymmetry (High).** Tracker reports `VWR` status in RED (Disconnected), while the Viewer shows Green. Indicates a critical failure in bidirectional signaling.
*   **ISSUE-OCT20.33-F2: HUD Layout Compression (Medium).** The telemetry HUD is vertically squeezed, making text illegible, while action buttons have excessive padding.
*   **ISSUE-OCT20.33-F3: Settings Gear Restoration (High).** Requirement change: The Settings gear is required on the Tracker to verify geofence (fence) state, reversing the partial R872 purge.
*   **ISSUE-OCT20.33-F4: UI Overlap (Version/Scale) (Low).** Version tag overlaps with the Map Scale bar in the bottom-right corner.
*   **ISSUE-OCT20.33-F5: Debug Artifact Leak (Low).** The `UI_READY` status label is visible in the PLATINUM build and must be suppressed.

---

## 💡 Strategic Simplification Ideas (Ideas: 62)
*   **SIMP-OCT20.32-S1: Settings Relay Abstraction (Medium).**
*   **SIMP-OCT20.32-S2: Role-Based Header Scrim (Low).**
*   **SIMP-OCT20.31-S1: Tracker UI Role-Lock (High).** (Under Review: See #OCT20.33-F3).

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT20.31-F1: Forbidden Tracker Authority & Stealth Breach.** Resolved Oct20.33 (Initial Purge).
*   **ISSUE-OCT20.32-F5: Home Point Synchronization Failure.** Resolved Oct20.33.
*   **ISSUE-OCT20.32-F4: Asymmetrical Connection State.** Resolved Oct20.33.
*   **ISSUE-OCT20.32-F3: HUD Header Scrim/Occlusion.** Resolved Oct20.33.
*   **ISSUE-OCT20.32-F2: Version String Alignment.** Resolved Oct20.33.
*   **ISSUE-OCT20.32-F1: Back Button Termination Bug.** Resolved Oct20.33.

---

## 📊 Hardening Progress Dashboard
- **Oct20.33: [SOT Count: 800 (Rules: 317), Open: H:2, M:1, L:2, Ideas: H:2, M:21, L:39, Testing: 8 (Sub-items: 16), QA: 370]**
