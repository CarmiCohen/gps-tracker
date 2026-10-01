# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.1.2

## 🎯 Current Resumption Focus: Horizontal Hardening & Release Stability
Stability of Alarm Muting, Communication Contracts, and UI State Authority.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*(No high-priority gaps remain open)*

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
*   **Issue #1407: Unified Storage Authority.** (Resolved Oct.1.2)
    *   *Audit*: Verified role-based isolation for `MAX_ACCURACY_KEY`, `CLOCK_DRIFT_REF_KEY`, and sensor baselines. 
    *   *Remediation*: Purged legacy global field fall-throughs and `routeToNamespaced` logic from `SettingsRepository`. Fixed `CLOCK_DRIFT_REF_KEY` leakage in `MonitorService`.
*   **Issue #1406: Role Identity Authority.** (Resolved Sep.30.43)
*   **Issue #1403: Siren Lockout Compliance.** (Resolved Sep.30.43)
*   **Issue #1404: Lockout Persistence.** (Resolved Sep.30.43)
*   **Issue #1405: Sequential Trigger Mute Protection.** (Resolved Sep.30.43)
*   **Issue #1402: Alarm Overlay Z-Index.** (Resolved Sep.30.43)
*   **Issue #1401: Connectivity Alarm Suppression.** (Resolved Sep.30.43)
*   **Issue #1390: Camera Action Event Flow.** (Resolved Sep.30.43)

---

## 📊 Hardening Progress Dashboard
- **Oct.1.2: [SOT Count: 232 (Rules: 81), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 8, QA: 322]**
- **Oct.1.1: [SOT Count: 232 (Rules: 81), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 8, QA: 322]**
- **Sep.30.43: [SOT Count: 227 (Rules: 78), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 6, QA: 318]**
