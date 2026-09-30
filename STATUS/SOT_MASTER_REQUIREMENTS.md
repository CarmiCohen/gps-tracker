# SOT Master Requirements & Hardening Status (Sep.30.43)

## 🏗️ Architectural Master Rules (75 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity)
*   **1.61 Reactive Camera Actions (R726/S558)**: Imperative map camera animations (zoom, centering) MUST be delivered via a `SharedFlow<CameraAction>` rather than cumulative state triggers. This decouples transient UI commands from the persistent `MapViewState`, eliminating state churn and ensuring that camera events are processed exactly once per emission (Issue #1390).
*   **1.62 Scale-Aware Ribbon Layouts (R727/S559)**: Forensic ribbons and time rulers MUST utilize reserved bottom-padding and orientation-aware height metrics to prevent timestamp truncation and ensure legibility across budget hardware viewports (Issue #1384).
*   **1.63 Centralized Behavioral Authority (R-ID 548/S560)**: The high-level behavioral state of the tracker (`TrackerState`) MUST be determined exclusively within the engine's primary tick loop (`MonitorService`) via `TrackerStateManager`. Downstream consumers (HUD, Signaling, Persistence) MUST utilize the state value captured in the `SystemEvaluationSnapshot` to ensure global consistency and prevent velocity-state desynchronization (Issue #1386).
*   **1.64 Peer Relay Argument Robustness (R-ID 549/S561)**: Signaling relay handlers MUST adaptively support both single-argument and multi-argument (routingId-prefixed) payloads to ensure link stability across varying Socket.io relay server configurations (Issue #1385).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 564**: Camera Action Event Flow - Migrated imperative map commands to `SharedFlow<CameraAction>`, resolving state churn and trigger-loop risks. (Resolved Sep.30.43).
*   **SOT ID 563**: Forensic & Stealth Audit - Verified 65s PARKING hysteresis and absolute stealth enforcement (R872) for release readiness. (Resolved Sep.30.43).
*   **SOT ID 561**: Peer Relay Argument Robustness - Refactored `CommunicationManager` to support multi-argument payloads. (Resolved Sep.30.43).
*   **SOT ID 560**: Centralized Behavioral Authority - Migrated `TrackerState` calculation to the engine tick. (Resolved Sep.30.43).
*   **SOT ID 562**: Stealth Authority Enforcement - Guarded Red-Screen promotion in `MainViewModel`. (Resolved Sep.30.43).
*   **SOT ID 559**: Scale-Aware Ribbon Layouts - Refactored `ForensicRibbonContainer` forlegibility on SM-A155F. (Resolved Sep.30.43).

---

## 🏁 Verification Chapters
*   **Chapter 31.196 (Camera Flow Audit)**: PASSED - Verified that Zoom and Center events trigger correctly via SharedFlow without persistent state counter increment loops. (Sep.30.43)
*   **Chapter 31.195 (Forensic & Stealth Audit)**: PASSED - Verified 65s PARKING hysteresis and absolute stealth enforcement (R872) on Tracker hardware. (Sep.30.43)
*   **Chapter 31.194 (Behavioral Authority Audit)**: PASSED - Verified that `TrackerState` in HUD and Signaling are perfectly synchronized. (Sep.30.43)
*   **Chapter 31.193 (Peer Link Robustness Audit)**: PASSED - Verified connection stability with relay servers that prepend routing IDs. (Sep.30.43)
