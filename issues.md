# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.30.6

## 🎯 Current Resumption Focus: Field & Soak Stability
Ensuring forensic probe reliability and baseline stability during extended field operations.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   *All high-priority hardening issues for this cycle are RESOLVED.*

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🔵 Low Priority
*   **Issue #1390: Camera Action Event Flow**
    *   *Significance*: **Low (Architectural Hygiene)**. Replace multiple cumulative trigger counters in `MapViewState` with a single `SharedFlow<CameraAction>` to decouple imperative map commands from the persistent UI state and reduce state churn.
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #Audit-Sep.30.6: Field Soak & Stealth Validation.** (Resolved Sep.30.6)
    *   *Remediation*: Audited `TrackerStateManager` and `AppNotificationManager` to confirm 65s PARKING hysteresis and absolute stealth (R872) on Tracker hardware. Verified HUD/Signaling parity via engine-tick authority.
*   **Issue #1385: Peer Link & Diagnostic LED Stall.** (Resolved Sep.30.6)
    *   *Remediation*: Refactored `CommunicationManager.kt` relay handlers to support multi-argument payloads (routingId + data). This fixed the parse errors caused by relay server argument prepending, restoring the TRK/DAT/VWR link.
*   **Issue #1391: Alarm Leakage on Tracker (Stealth Violation).** (Resolved Sep.30.6)
    *   *Remediation*: Enforced **R872 (Stealth Authority)** in `MainViewModel`. Guarded Reactive Red-Screen promotion and siren engagement triggers to ensure they only manifest in Viewer mode.
*   **Issue #1386: Tracker HUD Velocity State Inconsistency.** (Resolved Sep.30.6)
    *   *Remediation*: Centralized behavioral state authority in the engine tick (`MonitorService.kt`) via `TrackerStateManager`. Propagated state via `SystemEvaluationSnapshot` to ensure global parity.
*   **Issue #1384: Ribbon Time Ruler Legibility.** (Resolved Sep.30.6)
*   **Issue #1383: Tracker Map Autonomous Zoom-In.** (Resolved Sep.30.6)
*   **Issue #1389: Reactive Red-Screen / Missing Alarm UI on Startup.** (Resolved Sep.30.6)
*   **Issue #1382: Spontaneous & Unstoppable Viewer Siren.** (Resolved Sep.30.6)

---

## 📊 Hardening Progress Dashboard
- **Sep.30.6: [SOT Count: 224 (Rules: 74), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:2, Testing: 6 (Sub-items: 29), QA: 305]**
- **Sep.30.6: [SOT Count: 224 (Rules: 74), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:2, Testing: 6 (Sub-items: 29), QA: 304]**
