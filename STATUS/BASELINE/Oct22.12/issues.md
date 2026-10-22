# Project Issues & Hardening Tracking (Rigorous Audit) - Oct22.12

## 🎯 Current Resumption Focus: Telemetry Sync & Violation Propagation.

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Failure (High).** 
    - Ping values are missing on both devices even when connected via factory defaults.
    - Sensor data (Noise/Lux) is not propagating to the Viewer (shows 0dB/0lx while Tracker has active readings).
    - Acoustic violations visible on Tracker are not mirrored on the Viewer.

---

## 💡 Strategic Simplification Ideas (Ideas: 69)
*   **SIMP-OCT22.10-S1: BackHandler Composition Local (Low).** Consolidate back button interception into a reusable composition local or custom modifier to reduce boilerplate in overlay components.
*   **SIMP-OCT22.0-S1: Settings Validation Centralization (Medium).** Move identity uniqueness and regex validation from `SettingsRepository` and `UiEventCoordinator` into a pure `ConfigValidator` utility to ensure consistent enforcement across UI and automated imports.
*   **SIMP-OCT21.05-S1: Dynamic Version Injection in Tests (Medium).** Refactor `MainViewModelMappingTest` to pull the version string from central configuration.
*   **SIMP-OCT21.04-S1: Scale Bar Configuration Decoupling (Low).** Move osmdroid scale bar offsets to `MapConfig`.
*   **SIMP-OCT21.03-S1: Debug Label Centralization (Low).** Move all debug-only UI elements to a specialized `DebugOverlay`.
*   **SIMP-OCT21.01-S1: Unified Peer Heartbeat (Medium).** Consolidate `viewer_pulse`, `viewer_status`, and `location_update`.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT22.0-F3: Connection State Asymmetry & Telemetry Leak.** Resolved Oct22.12. Enforced strict ID symmetry in `SignalingValidator` for all signaling events (pulses, pings, pongs) and blocked cross-viewer traffic in Viewer mode. (SOT ID 812).
*   **ISSUE-OCT22.0-F2: Setup Exit Authority & Lifecycle Failure.** Resolved Oct22.10. Implemented `BackHandler` for `SettingsOverlay` and `PhoneSetupOverlay` to prevent app exit. Added Close icon to `PhoneSetupOverlay` header. (SOT ID 811).
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
- **Oct22.12: [SOT Count: 812 (Rules: 320), Open: H:1, M:0, L:0, Ideas: H:2, M:25, L:42, Testing: 10 (Sub-items: 41), QA: 385]**
- **Oct22.10: [SOT Count: 811 (Rules: 319), Open: H:2, M:0, L:0, Ideas: H:2, M:25, L:42, Testing: 10 (Sub-items: 41), QA: 385]**
- **Oct22.0: [SOT Count: 810 (Rules: 317), Open: H:3, M:0, L:0, Ideas: H:2, M:25, L:41, Testing: 10 (Sub-items: 41), QA: 385]**
