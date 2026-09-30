# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.29.3

## 🎯 Current Resumption Focus: Field & Soak Stability
Ensuring forensic probe reliability and baseline stability during extended field operations.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   *(All high-priority items resolved)*

---

## 💡 Strategic Simplification Ideas (Ideas: 1)

### 🔵 Low Priority
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1381: Heartbeat Centralization** (Resolved Sep.29.3)
    *   *Significance*: **Medium (Architectural Cleanup)**. 
    *   *Remediation*: Removed the "Bypass Heartbeat" manual trigger from `MonitorService`. Delegated local telemetry mapping to `AppEventCoordinator` which now continuously provides `localStatusFlyweight` to `ConnectivitySuite`. Added a dedicated `startHeartbeatLoop()` inside `ConnectivitySuite` to pulse the cached local status every 30s when the link is quiet, ensuring the Service layer remains purely reactive while `ConnectivitySuite` actively manages link health and presence signaling.
*   **Issue #1380: Peer Link Discovery & Navigation Hardening** (Resolved Sep.29.3)
    *   *Symptoms*: TRK LED on Viewer remained Red while VWR on Tracker was Cyan, indicating a one-way handshake failure. Diagnostics screen was occluded by the Settings overlay.
    *   *Remediation*: Implemented "Bypass Heartbeat" in `MonitorService.kt` to force telemetry transmission every 30s regardless of GPS fix status. Relaxed `SignalingValidator` to accept zero-coordinate packets as valid presence signals. Updated `SettingsOverlay` to explicitly dismiss when navigating to Diagnostics, resolving UI occlusion.
*   **Issue #1378: S21 & A15 Cross-Hardware Verification** (Resolved Sep.29.3)
    *   *Symptoms*: Forensic probe disappearance on budget (A15) and high-performance (S21) hardware during instrumented test suites.
    *   *Remediation*: Discovered cross-test state leakage resulting from `ForensicSpillBuffer` being a Singleton. Test suites running sequentially (such as `verifyExtendedSoakSimulation` followed by `verifySignalingLifecycleProbes`) caused race conditions on the unmanaged `totalCount` parameter. Implemented `resetBufferForTest()` across `@Before` hooks in `ProductionReadinessAuditTest` and `ForensicStressAuditTest` to ensure pristine test boundaries. Hardened `ForensicSpillBuffer` string persistence to guarantee byte offsets are cleanly overwritten. All 23 instrumented tests pass natively on both A15 and S21 devices.
*   **Issue #1377: Production Codebase Stabilization & Tracking Alignment** (Resolved Sep.29.3)
*   **Issue #S071: Stress Test UI Consolidation** (Resolved Sep.29.3)

---

## 📊 Hardening Progress Dashboard
- **Sep.29.3: [SOT Count: 211 (Rules: 68), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.29.3: [SOT Count: 211 (Rules: 68), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.29.3: [SOT Count: 211 (Rules: 68), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 23), QA: 304]**
- **Sep.29.3: [SOT Count: 209 (Rules: 67), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 23), QA: 302]**
- **Sep.29.3: [SOT Count: 207 (Rules: 66), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 23), QA: 300]**
- **Sep.29.3: [SOT Count: 171 (Rules: 41), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:4, Testing: 3 (Sub-items: 15), QA: 284]**
