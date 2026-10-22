# Project Issues & Hardening Tracking (Rigorous Audit) - Oct22.0

## 🎯 Current Resumption Focus: Setup & Config Hardening.

## 🔴 Open Gaps & Unfinished Integration Points
*   *(No high-priority UI or connectivity gaps remaining in current cycle).*

---

## 💡 Strategic Simplification Ideas (Ideas: 68)
*   **SIMP-OCT22.0-S1: Settings Validation Centralization (Medium).** Move identity uniqueness and regex validation from `SettingsRepository` and `UiEventCoordinator` into a pure `ConfigValidator` utility to ensure consistent enforcement across UI and automated imports.
*   **SIMP-OCT21.05-S1: Dynamic Version Injection in Tests (Medium).** Refactor `MainViewModelMappingTest` to pull the version string from central configuration.
*   **SIMP-OCT21.04-S1: Scale Bar Configuration Decoupling (Low).** Move osmdroid scale bar offsets to `MapConfig`.
*   **SIMP-OCT21.03-S1: Debug Label Centralization (Low).** Move all debug-only UI elements to a specialized `DebugOverlay`.
*   **SIMP-OCT21.01-S1: Unified Peer Heartbeat (Medium).** Consolidate `viewer_pulse`, `viewer_status`, and `location_update`.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT22.0-F1: Setup Configuration Failure.** Resolved Oct22.0. Fixed ID persistence block in `SettingsUseCase`, added explicit Exit/Close authority to `SettingsOverlay`, and implemented `LoadDefaults` for factory reset. Promoted Relay URL visibility and contrast. (Formerly ISSUE-OCT21.07-F1).
*   **ISSUE-OCT21.05-F2: Asymmetric Connectivity & Geofence Sync Failure.** Resolved Oct21.06. Integrated `viewer_status` pulses and proactive Geofence synchronization.
*   **ISSUE-OCT21.05-F1: Unit Test Version Mismatch.** Resolved Oct21.05.
*   **ISSUE-OCT20.33-F4: UI Overlap (Version/Scale).** Resolved Oct21.04.
*   **ISSUE-OCT20.33-F5: Debug Artifact Leak.** Resolved Oct21.03.
*   **ISSUE-OCT20.33-F3: Settings Gear Restoration.** Resolved Oct21.02.
*   **ISSUE-OCT20.33-F1: VWR Connection Asymmetry.** Resolved Oct21.01.
*   **ISSUE-OCT20.33-F2: HUD Layout Compression.** Resolved Oct21.00.

---

## 📊 Hardening Progress Dashboard
- **Oct22.0: [SOT Count: 810 (Rules: 317), Open: H:0, M:0, L:0, Ideas: H:2, M:25, L:41, Testing: 7 (Sub-items: 32), QA: 385]**
- **Oct21.06: [SOT Count: 807 (Rules: 314), Open: H:0, M:0, L:0, Ideas: H:2, M:24, L:41, Testing: 6 (Sub-items: 28), QA: 381]**
