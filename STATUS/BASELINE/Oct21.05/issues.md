# Project Issues & Hardening Tracking (Rigorous Audit) - Oct21.05

## 🎯 Current Resumption Focus: Forensic Audit.

## 🔴 Open Gaps & Unfinished Integration Points
*   *(No high-priority UI or connectivity gaps remaining in current cycle).*

---

## 💡 Strategic Simplification Ideas (Ideas: 67)
*   **SIMP-OCT21.05-S1: Dynamic Version Injection in Tests (Medium).** Refactor `MainViewModelMappingTest` to pull the version string from a central configuration or `BuildConfig` rather than using hardcoded literals to prevent release-blocking regressions.
*   **SIMP-OCT21.04-S1: Scale Bar Configuration Decoupling (Low).** Move osmdroid scale bar offsets and alignment logic to a dedicated `MapConfig` object to keep `MapComponents.kt` pure and free of imperative initialization.
*   **SIMP-OCT21.03-S1: Debug Label Centralization (Low).** Move all debug-only UI elements to a specialized `DebugOverlay` or `DiagnosticDecorator` to avoid polluting main business logic layouts with `BuildConfig.DEBUG` branches.
*   **SIMP-OCT21.01-S1: Unified Peer Heartbeat (Medium).** Consolidate `viewer_pulse`, `viewer_status`, and `location_update` into a single peer-presence state machine in `ConnectivitySuite` to reduce code duplication and forensic noise.
*   **SIMP-OCT21.00-S1: Density-Aware HUD Scaling (Medium).** Implement dynamic padding based on LocalDensity to better handle A15 high-DPI screens. (Significance: Medium)

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT21.05-F1: Unit Test Version Mismatch.** Resolved Oct21.05. Synchronized `MainViewModelMappingTest` with the current release version.
*   **ISSUE-OCT20.33-F4: UI Overlap (Version/Scale).** Resolved Oct21.04. Explicitly anchored ScaleBarOverlay to bottom-right corner to resolve layout conflict with version tag.
*   **ISSUE-OCT20.33-F5: Debug Artifact Leak.** Resolved Oct21.03. Wrapped `UI_READY` status label in `BuildConfig.DEBUG` check to suppress it in production builds.
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
- **Oct21.05: [SOT Count: 806 (Rules: 314), Open: H:0, M:0, L:0, Ideas: H:2, M:24, L:41, Testing: 6 (Sub-items: 27), QA: 380]**
- **Oct21.04: [SOT Count: 805 (Rules: 314), Open: H:0, M:0, L:0, Ideas: H:2, M:23, L:41, Testing: 9 (Sub-items: 17), QA: 379]**
