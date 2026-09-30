# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.30.1

## 🎯 Current Resumption Focus: Field & Soak Stability
Ensuring forensic probe reliability and baseline stability during extended field operations.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   *(All high-priority items resolved)*

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🔵 Medium Priority
*   **Issue #1381: Heartbeat Centralization**
    *   *Significance*: **Medium (Architectural Cleanup)**. Move the "Bypass Heartbeat" logic from `MonitorService` directly into `ConnectivitySuite`'s internal loops to keep the Service layer purely reactive and the Connectivity layer responsible for link health.

### 🔵 Low Priority
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1380: Peer Link Discovery & Navigation Hardening** (Resolved Sep.30.1)
    *   *Symptoms*: TRK LED on Viewer remained Red while VWR on Tracker was Cyan, indicating a one-way handshake failure. Diagnostics screen was occluded by the Settings overlay.
    *   *Remediation*: Implemented "Bypass Heartbeat" in `MonitorService.kt` to force telemetry transmission every 30s regardless of GPS fix status. Relaxed `SignalingValidator` to accept zero-coordinate packets as valid presence signals. Updated `SettingsOverlay` to explicitly dismiss when navigating to Diagnostics, resolving UI occlusion.
*   **Issue #1378: S21 & A15 Cross-Hardware Verification** (Resolved Sep.29.31)
    *   *Symptoms*: Forensic probe disappearance on budget (A15) and high-performance (S21) hardware during instrumented test suites.
    *   *Remediation*: Discovered cross-test state leakage resulting from `ForensicSpillBuffer` being a Singleton. Test suites running sequentially (such as `verifyExtendedSoakSimulation` followed by `verifySignalingLifecycleProbes`) caused race conditions on the unmanaged `totalCount` parameter. Implemented `resetBufferForTest()` across `@Before` hooks in `ProductionReadinessAuditTest` and `ForensicStressAuditTest` to ensure pristine test boundaries. Hardened `ForensicSpillBuffer` string persistence to guarantee byte offsets are cleanly overwritten. All 23 instrumented tests pass natively on both A15 and S21 devices.
*   **Issue #1377: Production Codebase Stabilization & Tracking Alignment** (Resolved Sep.29.3)
*   **Issue #S071: Stress Test UI Consolidation** (Resolved Sep.29.3)

---

## 📊 Hardening Progress Dashboard
- **Sep.30.1: [SOT Count: 211 (Rules: 68), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.29.31: [SOT Count: 211 (Rules: 68), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.29.30: [SOT Count: 209 (Rules: 67), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 23), QA: 302]**
- **Sep.29.3: [SOT Count: 207 (Rules: 66), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 23), QA: 300]**
- **Sep.27.9: [SOT Count: 171 (Rules: 41), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:4, Testing: 3 (Sub-items: 15), QA: 284]**
