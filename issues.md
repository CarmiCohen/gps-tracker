# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.1.8

## 🎯 Current Resumption Focus: State Persistence & Peer Convergence
Hardening of cross-device acknowledgment state and idempotent trigger evaluation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   **Resource Management**: Monitor for `WindowManager` leaks on extreme low-memory devices during long-duration overlay alerts.

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🟡 Medium Priority
*   **Issue #SIMP-1407-1: Deprecate Global Storage APIs**
    *   *Significance*: **Medium**. (Completed).

### 🔵 Low Priority
*   **Issue #1175: Real-time Only Path**
    *   *Significance*: **Strategic**. Consider removing backlog sync and forensic backfilling.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1410: Viewer Persistence (Recurring Alarms).** (Resolved Oct.1.8)
*   **Issue #1402-B: System-Wide Alarm Overlay Failure.** (Resolved Oct.1.7)
*   **Issue #1409: Connection Loss Siren Logic Mismatch.** (Resolved Oct.1.6)
*   **Issue #1410-B: Standardized Manual Silence Persistence.** (Resolved Oct.1.6)
*   **Issue #1412: Ribbon Visual Occlusion & Scale Spacing.** (Resolved Oct.1.6)
*   **Issue #MAP-SOT-01: Marker Pooling Implementation Mismatch.** (Resolved Oct.1.5)
*   **Issue #MAP-SOT-02: Missing Ghost Mode for Trails.** (Resolved Oct.1.5)
*   **Issue #MAP-SOT-03: Missing Stationary Anchor Visual Badge.** (Resolved Oct.1.5)
*   **Issue #1411: App Version Visibility Requirement.** (Resolved Oct.1.5)

---

## 📊 Hardening Progress Dashboard
- **Oct.1.8: [SOT Count: 248 (Rules: 95), Open: H:1, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 11, QA: 348]**
- **Oct.1.7: [SOT Count: 247 (Rules: 94), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 345]**
- **Oct.1.6: [SOT Count: 246 (Rules: 93), Open: H:1, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 342]**
