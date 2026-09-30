# SOT Master Requirements & Hardening Status (Sep.30.40)

## 🏗️ Architectural Master Rules (74 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity)
*   **1.61 Stateful Camera Triggers (R726/S558)**: Imperative map camera animations (zoom, centering) MUST be guarded by stateful trigger tracking within the controller. Triggers MUST be implemented as cumulative counters, and the controller MUST only execute the animation when the counter explicitly increments, preventing re-execution loops during UI state refreshes (Issue #1383).
*   **1.62 Scale-Aware Ribbon Layouts (R727/S559)**: Forensic ribbons and time rulers MUST utilize reserved bottom-padding and orientation-aware height metrics to prevent timestamp truncation and ensure legibility across budget hardware viewports (Issue #1384).
*   **1.63 Centralized Behavioral Authority (R-ID 548/S560)**: The high-level behavioral state of the tracker (`TrackerState`) MUST be determined exclusively within the engine's primary tick loop (`MonitorService`) via `TrackerStateManager`. Downstream consumers (HUD, Signaling, Persistence) MUST utilize the state value captured in the `SystemEvaluationSnapshot` to ensure global consistency and prevent velocity-state desynchronization (Issue #1386).
*   **1.64 Peer Relay Argument Robustness (R-ID 549/S561)**: Signaling relay handlers MUST adaptively support both single-argument and multi-argument (routingId-prefixed) payloads to ensure link stability across varying Socket.io relay server configurations (Issue #1385).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 561**: Peer Relay Argument Robustness - Refactored `CommunicationManager` to support multi-argument payloads, resolving the Peer Link stall caused by relay server argument prepending. (Resolved Sep.30.40).
*   **SOT ID 560**: Centralized Behavioral Authority - Migrated `TrackerState` calculation to the engine tick to eliminate HUD velocity inconsistencies. (Resolved Sep.30.40).
*   **SOT ID 562**: Stealth Authority Enforcement - Guarded Red-Screen promotion in `MainViewModel` to prevent Tracker-mode UI leakage. (Resolved Sep.30.40).
*   **SOT ID 559**: Scale-Aware Ribbon Layouts - Refactored `ForensicRibbonContainer` to support reserved vertical space for time ruler timestamps, ensuring legibility on SM-A155F. (Resolved Sep.30.6).
*   **SOT ID 558**: Stateful Camera Triggers - Implemented trigger tracking in `MapController` to prevent autonomous zoom loops triggered by reactive UI state updates. (Resolved Sep.30.6).

---

## 🏁 Verification Chapters
*   **Chapter 31.194 (Behavioral Authority Audit)**: PASSED - Verified that `TrackerState` in HUD and Signaling are perfectly synchronized with 0.0 km/h "PARKING" states. (Sep.30.40)
*   **Chapter 31.193 (Peer Link Robustness Audit)**: PASSED - Verified connection stability with relay servers that prepend routing IDs to payloads. (Sep.30.40)
*   **Chapter 31.192 (Tracker Stealth Audit)**: PASSED - Verified that Trackers remain silent/dark even during active Geofence violations. (Sep.30.40)
*   **Chapter 31.191 (Ribbon Legibility Audit)**: PASSED - Verified on SM-A155F that Time Ruler timestamps are fully visible and labels are not truncated. (Sep.30.6)
