# SOT Master Requirements & Hardening Status (Sep.30.6)

## 🏗️ Architectural Master Rules (71 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity)
*   **1.58 Signaling Authority Centralization (R723/S554)**: Internal heartbeat timing logic MUST reside exclusively within the signaling transport layers (`ConnectivitySuite`) to preserve Service layer reactivity and simplify test coverage boundaries (Issue #1381).
*   **1.59 Siren UI Synchronization (R724/S556)**: Critical alarm sirens MUST be accompanied by a system notification and Red Screen overlay to ensure user visibility and provide a dismissal mechanism, preventing autonomous re-triggering due to unresolved states (Issue #1382).
*   **1.60 Reactive Red-Screen Promotion (R725/S557)**: The UI layer MUST reactively promote itself to the "Red Screen" (AlarmOverlay) state when critical violations are detected by the engine, bypassing the unreliability of system-level fullScreenIntents during application startup (Issue #1389).
*   **1.61 Stateful Camera Triggers (R726/S558)**: Imperative map camera animations (zoom, centering) MUST be guarded by stateful trigger tracking within the controller. Triggers MUST be implemented as cumulative counters, and the controller MUST only execute the animation when the counter explicitly increments, preventing re-execution loops during UI state refreshes (Issue #1383).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 558**: Stateful Camera Triggers - Implemented trigger tracking in `MapController` to prevent autonomous zoom loops triggered by reactive UI state updates. (Resolved Sep.30.6).
*   **SOT ID 557**: Reactive Red-Screen Promotion - Implemented reactive UI promotion in `MainViewModel` to ensure visual alarm parity with audio sirens, resolving the "Ghost Siren" issue on startup. (Resolved Sep.30.6).
*   **SOT ID 554**: Heartbeat Centralization - Moved bypass heartbeat loop to `ConnectivitySuite`, delegating telemetry mapping strictly to `AppEventCoordinator` logic. (Resolved Sep.30.6).
*   **SOT ID 555**: Documentation Version Alignment - Synchronized version headers across all tracking documents to Sep.30.6. (Resolved Sep.30.6).
*   **SOT ID 556**: Siren UI Synchronization - Integrated notification triggering into the siren requirement loop in `AppEventCoordinator` to resolve "Unstoppable Siren" loop. (Resolved Sep.30.6).

---

## 🏁 Verification Chapters
*   **Chapter 31.190 (Map Zoom Stability Audit)**: PASSED - Verified that `MapController` now caches `lastZoomInTrigger` etc., and only invokes Osmdroid animations on increment. (Sep.30.6)
*   **Chapter 31.189 (Reactive Red-Screen Audit)**: PASSED - Verified that `MainViewModel` reactively promotes `isRedScreenVisible` based on `activeAlarmsFlow`, ensuring visibility during high-churn startup. (Sep.30.6)
*   **Chapter 31.188 (Siren UI Synchronization Audit)**: PASSED - Verified that `AppEventCoordinator` now invokes `AppNotificationManager` alongside `AudioSynthesizer`. (Sep.30.6)
*   **Chapter 31.187 (Version Alignment Audit)**: PASSED - Verified baseline Sep.30.6. (Sep.30.6)
