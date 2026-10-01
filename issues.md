# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.1.7

## 🎯 Current Resumption Focus: Forensic Overlays & System-Level Alerting
Implementation of system-wide overlays for high-priority theft alerts.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   *No high priority issues currently open.*

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🟡 Medium Priority
*   **Issue #SIMP-1407-1: Deprecate Global Storage APIs**
    *   *Significance*: **Medium**. (Completed in Oct.1.2). Purged all non-namespaced `save[Type]` and `get[Type]` methods from `SettingsRepository` for keys that require role-based isolation.

### 🔵 Low Priority
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1402-B: System-Wide Alarm Overlay Failure.** (Resolved Oct.1.7)
*   **Issue #1409: Connection Loss Siren Logic Mismatch.** (Resolved Oct.1.6)
*   **Issue #1410: Standardized Manual Silence Persistence.** (Resolved Oct.1.6)
*   **Issue #1412: Ribbon Visual Occlusion & Scale Spacing.** (Resolved Oct.1.6)
*   **Issue #MAP-SOT-01: Marker Pooling Implementation Mismatch.** (Resolved Oct.1.5)
*   **Issue #MAP-SOT-02: Missing Ghost Mode for Trails.** (Resolved Oct.1.5)
*   **Issue #MAP-SOT-03: Missing Stationary Anchor Visual Badge.** (Resolved Oct.1.5)
*   **Issue #1411: App Version Visibility Requirement.** (Resolved Oct.1.5)
*   **Issue #1408: Thermal & Convergence Audit.** (Resolved Oct.1.3)
*   **Issue #1407: Unified Storage Authority.** (Resolved Oct.1.2)
*   **Issue #1406: Role Identity Authority.** (Resolved Sep.30.43)
*   **Issue #1403: Siren Lockout Compliance.** (Resolved Sep.30.43)
*   **Issue #1404: Lockout Persistence.** (Resolved Sep.30.43)
*   **Issue #1405: Sequential Trigger Mute Protection.** (Resolved Sep.30.43)
*   **Issue #1402: Alarm Overlay Z-Index.** (Resolved Sep.30.43)
*   **Issue #1401: Connectivity Alarm Suppression.** (Resolved Sep.30.43)
*   **Issue #1390: Camera Action Event Flow.** (Resolved Sep.30.43)

---

## 📊 Hardening Progress Dashboard
- **Oct.1.7: [SOT Count: 247 (Rules: 94), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 345]**
- **Oct.1.6: [SOT Count: 246 (Rules: 93), Open: H:1, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 342]**
- **Oct.1.5: [SOT Count: 243 (Rules: 92), Open: H:4, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 335]**
- **Oct.1.3: [SOT Count: 240 (Rules: 89), Open: H:8, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 9, QA: 330]**
- **Oct.1.2: [SOT Count: 232 (Rules: 81), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 8, QA: 322]**
- **Sep.30.43: [SOT Count: 227 (Rules: 78), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 6, QA: 318]**
