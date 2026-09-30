# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.30.6

## 🎯 Current Resumption Focus: Field & Soak Stability
Ensuring forensic probe reliability and baseline stability during extended field operations.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   **Issue #1384: Ribbon Time Ruler Legibility.**
    *   *Symptoms*: Time ruler is unreadable. Needs UI/Rendering audit via screenshot analysis.
*   **Issue #1385: Peer Link & Diagnostic LED Stall.**
    *   *Symptoms*: Viewer LEDs (GPS, TRK, DAT) are Red. Tracker LEDs are GPS (Red), DAT (Red), VWR (Cyan). Indicates complete connection failure between devices.
*   **Issue #1386: Tracker HUD Velocity State Inconsistency.**
    *   *Symptoms*: HUD shows "MOVING" while speed is constant 0.0 km/h.

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🔵 Low Priority
*   **Issue #1390: Camera Action Event Flow**
    *   *Significance*: **Low (Architectural Hygiene)**. Replace multiple cumulative trigger counters in `MapViewState` with a single `SharedFlow<CameraAction>` to decouple imperative map commands from the persistent UI state and reduce state churn.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1383: Tracker Map Autonomous Zoom-In.** (Resolved Sep.30.6)
    *   *Remediation*: Implemented **Stateful Trigger Tracking** in `MapController`. The controller now caches the last-seen cumulative trigger counts from `MapViewState`, ensuring that imperative Osmdroid camera animations (zoom/centering) only execute when the trigger explicitly increments, preventing re-execution loops during UI state refreshes.
*   **Issue #1389: Ghost Siren / Missing Alarm UI on Startup.** (Resolved Sep.30.6)
    *   *Remediation*: Implemented **Reactive Red-Screen Promotion** in `MainViewModel`. The UI now automatically triggers the `AlarmOverlay` when violations are detected, bypassing the unreliability of system-level `fullScreenIntent` delivery on Android 14/15. Fixed the data flow gap in `StateSubscriptionUseCase` to ensure active alarms are correctly propagated to the view.
*   **Issue #1382: Spontaneous & Unstoppable Viewer Siren.** (Resolved Sep.30.6)
    *   *Remediation*: Synchronized `AudioSynthesizer` siren loop with `AppNotificationManager` in `AppEventCoordinator`. Critical alarms now trigger the Red Screen overlay and system notification, allowing users to acknowledge and dismiss violations to prevent autonomous re-triggering.
*   **Issue #1387: Documentation Version Mismatch.** (Resolved Sep.30.6)
    *   *Remediation*: Synchronized version headers across `issues.md`, `Handover.md`, and `STATUS/` directory.
*   **Issue #1388: App Deployment Version Inconsistency.** (Resolved Sep.30.6)
    *   *Remediation*: Updated `versionName` in `app/build.gradle` to `Sep.30.6`.
*   **Issue #1381: Heartbeat Centralization** (Resolved Sep.30.6)
    *   *Remediation*: Moved bypass heartbeat loop to `ConnectivitySuite`, delegating telemetry mapping strictly to `AppEventCoordinator`.

---

## 📊 Hardening Progress Dashboard
- **Sep.30.6: [SOT Count: 220 (Rules: 71), Open: H:3, M:0, L:0, Ideas: H:0, M:0, L:2, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.30.6: [SOT Count: 219 (Rules: 70), Open: H:4, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.30.6: [SOT Count: 219 (Rules: 68), Open: H:4, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.30.6: [SOT Count: 218 (Rules: 68), Open: H:7, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
