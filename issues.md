# Project Issues & Hardening Tracking (Rigorous Audit) - Sep.29.4

## 🎯 Current Resumption Focus: S21 Hardware Verification
Ensuring forensic probe reliability and baseline stability on the S21 hardware tier.

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

*   **Issue #1378: S21 Forensic Probe Failure** (Resolved Sep.29.4)
    *   *Symptoms*: `verifySignalingLifecycleProbes` fails on S21 despite `force` bypass and buffer logic hardening (v5, 128-byte). Probes are not being retrieved from the spill-buffer within the audit window.
    *   *Remediation*: The issue was determined to be a compilation caching edge-case involving the Kotlin `const val FORENSIC_SPILL_ENTRY_SIZE`. Because the size was recently increased to 128 bytes in `EngineConstants.kt`, incremental compilation missed updating `ForensicSpillBuffer.kt`. This caused `maxMsgLen` to be incorrectly computed as `<= 0`, leading to string truncation. Modifying and recompiling `ForensicSpillBuffer.kt` successfully cleared the stale cache and passed the test on the S21 device. All 21 tests are now consistently passing.
*   **Issue #1377: Production Codebase Stabilization & Tracking Alignment** (Resolved Sep.29.3)
*   **Issue #S071: Stress Test UI Consolidation** (Resolved Sep.29.3)
*   **Issue #071-G: Missing Stress Test Integration** (Resolved Sep.29.3)
*   **Issue #1376: Production Codebase Stabilization & Tracking Alignment** (Resolved Sep.29.3)

---

## 📊 Hardening Progress Dashboard
- **Sep.29.4: [SOT Count: 207 (Rules: 66), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 21), QA: 297]**
- **Sep.29.3: [SOT Count: 207 (Rules: 66), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 21), QA: 296]**
- **Sep.29.3: [SOT Count: 206 (Rules: 65), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 16), QA: 295]**
- **Sep.29.3: [SOT Count: 205 (Rules: 65), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 3 (Sub-items: 16), QA: 294]**
- **Sep.29.3: [SOT Count: 204 (Rules: 65), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 3 (Sub-items: 16), QA: 293]**