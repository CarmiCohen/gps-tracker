# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.30.50

## 🎯 Current Resumption Focus: Alarm System Hardening
Resolving re-triggering loops and ensuring SOT compliance for siren lockout and prefix consistency.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   **Issue #1406: Role Prefix Mismatch (Critical Bug).**
    *   *Significance*: **Functional Failure**. `CommandRouter` utilizes `"V_"` for Viewer role state, while `AppAlarmManager` and `AlertUseCase` utilize `"VR_"`. Acknowledgments are saved to orphaned keys, causing the engine to re-trigger alarms immediately as it never sees the "Stop" event.
*   **Issue #1403: Siren Lockout Duration Mismatch (SOT Deviation).**
    *   *Significance*: **SOT Compliance**. Requirement mandates 30s lockout; implementation uses 15s (`SIREN_RESUME_COOLDOWN_MS`).
*   **Issue #1404: Alarm Lockout Persistence Loss.**
    *   *Significance*: **User Experience**. `AppAlarmManager` wipes `lastSirenStopRt` on reboot/restart, causing previously muted alarms to fire again on service recovery.
*   **Issue #1405: Sequential Trigger Mute Failure.**
    *   *Significance*: **Logic Flaw**. New "Special" alarm types can break an existing siren lockout, leading to "unmutable" alarm sequences.

---

## 💡 Strategic Simplification Ideas (Ideas: 1)

### 🔵 Low Priority
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1402: Alarm Overlay Z-Index.** (Resolved Sep.30.43)
*   **Issue #1401: Connectivity Alarm Suppression.** (Resolved Sep.30.43)
*   **Issue #1390: Camera Action Event Flow.** (Resolved Sep.30.43)
*   **Issue #Audit-Sep.30.43: Field Soak & Stealth Validation.** (Resolved Sep.30.43)
*   **Issue #1385: Peer Link & Diagnostic LED Stall.** (Resolved Sep.30.43)
*   **Issue #1391: Alarm Leakage on Tracker (Stealth Violation).** (Resolved Sep.30.43)
*   **Issue #1386: Tracker HUD Velocity State Inconsistency.** (Resolved Sep.30.43)
*   **Issue #1384: Ribbon Time Ruler Legibility.** (Resolved Sep.30.43)
*   **Issue #1383: Tracker Map Autonomous Zoom-In.** (Resolved Sep.30.43)

---

## 📊 Hardening Progress Dashboard
- **Sep.30.50: [SOT Count: 227 (Rules: 78), Open: H:4, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 6, QA: 312]**
- **Sep.30.43: [SOT Count: 227 (Rules: 75), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 6, QA: 308]**
