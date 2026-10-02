# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.2.1

## 🎯 Current Resumption Focus: State Persistence & Peer Convergence
Hardening of cross-device acknowledgment state and idempotent trigger evaluation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   **Issue #1415: CPU-Load Compensation for Sensors**
    *   *Significance*: **High**. 100% CPU saturation during stress tests causes LIS2DLC12 jitter on A15, triggering false `TAMPER` alerts. Need to gate evaluations by `cpuLoad`.
*   **Issue #1416: Memory Pressure Mitigation**
    *   *Significance*: **High**. High-frequency sensor audit (250Hz) and sustained alerts cause cumulative heap growth. Need aggressive memory-flush/throttling for A15.
*   **Issue #1417: Jitter-Resistant Connectivity Transitions**
    *   *Significance*: **High**. 500ms relay lag causes oscillation between `RELAY_OFFLINE` and `SIGNAL_LOSS`. Need 3s temporal hysteresis.
*   **Resource Management (R-ID 582)**: Monitor for `WindowManager` leaks on extreme low-memory devices during long-duration overlay alerts.

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
*   **Issue #1414: Dashboard UNKNOWN state (Local Tracker).** Resolved Oct.1.8. Fixed local state routing to MainViewModel (R-ID 589).
*   **Issue #1413: Mode-Based Alert Violation (Stealth Regression).** Resolved Oct.1.8. Restricted AlarmOverlay to Viewer Mode only (R-ID 588).
*   **Issue #1410: Engine Thread-Safety (Fatal CME).** Resolved Oct.1.8. Migrated to ConcurrentHashMap and synchronized mutations (R-ID 585).
*   **Issue #1410: Viewer Persistence (Recurring Alarms).** Resolved Oct.1.8. Integrated global lastAlarmAckTs sync (R-ID 579).
*   **Issue #1402-B: System-Wide Alarm Overlay Failure.** Resolved Oct.1.7. Hardened disposal in Oct.1.8 (R-ID 582).
*   **Issue #1409: Connection Loss Siren Logic Mismatch.** Resolved Oct.1.6.
*   **Issue #1410-B: Standardized Manual Silence Persistence.** Resolved Oct.1.6.
*   **Issue #1412: Ribbon Visual Occlusion & Scale Spacing.** Resolved Oct.1.6.
*   **Issue #MAP-SOT-01: Marker Pooling Implementation Mismatch.** Resolved Oct.1.5.
*   **Issue #MAP-SOT-02: Missing Ghost Mode for Trails.** Resolved Oct.1.5.
*   **Issue #MAP-SOT-03: Missing Stationary Anchor Visual Badge.** Resolved Oct.1.5.
*   **Issue #1411: App Version Visibility Requirement.** Resolved Oct.1.5.

---

## 📊 Hardening Progress Dashboard
- **Oct.2.1: [SOT Count: 251 (Rules: 102), Open: H:4, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 12, QA: 352]**
- **Oct.1.8: [SOT Count: 251 (Rules: 102), Open: H:4, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 12, QA: 352]**
- **Oct.1.7: [SOT Count: 247 (Rules: 94), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 345]**
- **Oct.1.6: [SOT Count: 246 (Rules: 93), Open: H:1, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 342]**
