# System Source of Truth (SoT) - Aug.20.10 (Anchor Hardening & Build Restoration)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Stationary Anchor Hardening (R238)**: (Added Aug.20.10) The system MUST prevent stationary anchors from "chasing" GPS drift. Coordinate-averaging convergence MUST be restricted to points within the 50% "dead zone" of the current breakout threshold. Points entering the scoring (transition) zone MUST be rejected for averaging to ensure the anchor center remains fixed while breakout probability accumulates. (Issue #238). **Status: Implemented.**
*   **HUD State Centralization (R226)**: (Updated Aug.20.10) The system MUST utilize a unified `HudState` data class for all UI status indicators. DISPATCHERS.Default MUST be used for state combination, and all UI components MUST consume the centralized object to prevent reactive drift between technical telemetry and status badges. (Issue #226/240). **Status: Implemented.**
*   **Telemetry Mapping Consolidation (R225)**: (Added Aug.20.07) The system MUST utilize a unified `ForensicMapper` for all 1:1 parity transformations of high-frequency metadata. (Issue #225). **Status: Implemented.**
*   **Coordinate Stabilization Authority (R224)**: (Updated Aug.20.06) The system MUST prevent visual coordinate "snaps" during GPS revival. Reset threshold is set to 100m. (Issue #224). **Status: Implemented.**
*   **Build Integrity Hardening (R235)**: (Added Aug.20.09) The system MUST maintain explicit dependency declarations for `DataStore`, `Protobuf`, and `Socket.io` in the `:app` module to ensure build reproducibility. (Issue #235). **Status: Implemented.**

### 2. UI & Map Authority
*   **Diagnostics Visibility Authority (R233)**: (Added Aug.20.09) The system MUST ensure the `DiagnosticsScreen` is never obscured by initial setup overlays. (Issue #233). **Status: Implemented.**
*   **UI Hardening: Button Clipping (R232)**: (Added Aug.20.08) The system MUST utilize flexible heights (`heightIn(min=...)`) and optimized font sizes (`13.sp`) for action buttons on budget hardware (SM-A155F). (Issue #232). **Status: Implemented.**
