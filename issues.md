# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.29.3

## 🎯 Current Resumption Focus: S21 Hardware Verification
Ensuring forensic probe reliability and baseline stability on the S21 hardware tier.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (Field & Soak Testing Readiness)
*   **Issue #1378: S21 Forensic Probe Failure**
    *   *Symptoms*: `verifySignalingLifecycleProbes` fails on S21 despite `force` bypass and buffer logic hardening (v5, 128-byte). Probes are not being retrieved from the spill-buffer within the audit window.
    *   *Investigation Node*: Suspected `MappedByteBuffer` visibility latency or race condition under high S21 I/O throughput.
    *   *Files*: `ForensicSpillBuffer.kt`, `ProductionReadinessAuditTest.kt`, `LogRepository.kt`.

---

## 💡 Strategic Simplification Ideas (Ideas: 1)

### 🔵 Low Priority
*   **Issue #1175: Real-time Only Path (Pivot Option)**
    *   *Significance*: **Strategic (Maintenance Tradeoff)**. Consider removing backlog sync and forensic backfilling to dramatically reduce codebase complexity.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1378: S21 Device Verification & Codebase Advancement** (In Progress Sep.29.3)
    *   *Remediation*: Advanced versioning baseline to Sep.29.3. Implemented `force` parameter in `SignalingForensicLogger` to ensure test probe recording under high-frequency background activity (R720). Hardened `ForensicSpillBuffer` with version 5 schema (128-byte entries) and instance-level synchronization.
*   **Issue #1377: Production Codebase Stabilization & Tracking Alignment** (Resolved Sep.29.2)
*   **Issue #S071: Stress Test UI Consolidation** (Resolved Sep.29.1)
*   **Issue #071-G: Missing Stress Test Integration** (Resolved Sep.28.30)
*   **Issue #1376: Production Codebase Stabilization & Tracking Alignment** (Resolved Sep.28.29)

---

## 📊 Hardening Progress Dashboard
- **Sep.29.3: [SOT Count: 207 (Rules: 66), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 21), QA: 296]**
- **Sep.29.2: [SOT Count: 206 (Rules: 65), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 16), QA: 295]**
- **Sep.29.1: [SOT Count: 205 (Rules: 65), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 16), QA: 294]**
- **Sep.28.30: [SOT Count: 204 (Rules: 65), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 16), QA: 293]**
