# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*   **Issue #1123: Synchronous Thread Join in HardwareSuite Lifecycle**
    *   *Problem*: `stopAcousticMonitoring` performs a synchronous `acousticThread?.join(1000)` while holding `acousticLock`. This blocks service lifecycle transitions.
    *   *File*: `HardwareSuite.kt` (line 540)
    *   *Risk*: Service start/stop latency and potential ANRs.

*   **Issue #1143: Divergent Vibration Floor Calculation (Dual-State Logic)**
    *   *Problem*: `HardwareSuite` and `LocationSentinel` independently calculate `adaptiveVibrationFloor`, leading to potential divergence in stationary detection.
    *   *File*: `HardwareSuite.kt`, `LocationSentinel.kt`
    *   *Risk*: Inconsistent polling intervals or anchor locking.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1146: GPS Data Loss in TrackerService Tick Conflation** (Resolved Sep.21.120)
    *   *Remediation*: Replaced single-point GPS conflation with `ConcurrentLinkedQueue` buffer in `TrackerService.kt`. `processTick` now drains and processes all intermediate fixes to prevent telemetry data loss and maintain forensic precision. (R-ID 386)

*   **Issue #1126: Thread Visibility Hardening in HardwareSuite** (Resolved Sep.20.25)
    *   *Remediation*: Applied `@Volatile` markers to all critical timing and state variables in `HardwareSuite.kt`. (R-ID 385)

*   **Issue #1137: Missing Forensic State Reset in TrackerService** (Resolved Sep.20.22)
    *   *Remediation*: Explicitly zeroed all forensic sampling state variables in `TrackerService.resetServiceTimers()`. (R-ID 384)

*   **Issue #1149: Completed Fast-Path Light Spike Integration** (Resolved Sep.20.18)
    *   *Remediation*: Captured `lastFastPathLightSpikeTs` and propagated it to `LocationProcessor`. (R-ID 380)

*   **Issue #1150: Harmonized Light Baseline Synchronization** (Resolved Sep.20.18)
    *   *Remediation*: Implemented periodic baseline re-synchronization in `TrackerService.processTick()`. (R-ID 381)

*   **Issue #1124: Selective Forensic Audit Reset** (Resolved Sep.20.15)
    *   *Remediation*: Updated `HardwareSuite` and `ForensicAuditor` to support targeted role resets. (R-ID 376)

*   **Issue #1127/1128/1133/1135: Hardware Lifecycle Hardening** (Resolved Sep.20.15)
    *   *Remediation*: Implemented `clearLifecycleLeftovers()` in `HardwareSuite.kt`. (R-ID 377)

*   **Issue #1138/1147: Telemetry Propagation Hardening** (Resolved Sep.20.15)
    *   *Remediation*: Expanded persistence schema (v76) to include diagnostic flags. (R-ID 378)

*   **Issue #1122: False GNSS Jitter Spike on Suite Restart** (Resolved Sep.20.15)
    *   *Remediation*: Explicitly zeroed jitter source timestamp in `ForensicAuditor.reset()`. (R-ID 379)

*   **Issue #1132: ForensicAuditor Thread Safety Hardening** (Resolved Sep.20.18)
    *   *Remediation*: Implemented internal synchronization for `RoleState` within `ForensicAuditor.kt`. (R-ID 382)

*   **Issue #1142: Stale Integrity Vitality Timestamps** (Resolved Sep.20.20)
    *   *Remediation*: Reset all vitality timestamps in `IntegrityMonitor.resetStats()`. (R-ID 383)

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 386 (Rules: 80, IDs: 386), Resolved: 1138, Open: 2, Testing: 2 (Sub-items: 10), Ideas: 19, QA: 282]**
