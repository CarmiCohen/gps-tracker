# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*(No critical logic gaps identified in current audit path).*

### Unintended Side Effects & Thread Safety
*(No critical side-effects identified in current audit path).*

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1152: Flyweight Sequence Abstraction** (Resolved Sep.21.124)
    *   *Remediation*: Refactored `getSnrSamples`, `getSensorSamples`, and `getAcousticSamples` in `HardwareSuite` to use a unified `forensicSequence` utility in `CircularStateBuffer`. This eliminated redundant flyweight management and boilerplate code, ensuring thread-safe forensic sampling via temporary snapshotting (R-ID 391).

*   **Issue #1121: Telemetry Source Abstraction** (Resolved Sep.21.123)
    *   *Remediation*: Introduced `AlarmTelemetrySnapshot` and `AlarmServiceContext` DTOs to encapsulate telemetry inputs for the alarm engine. Refactored `AppAlarmManager.evaluateAlarms` and both monitoring services to use this pattern, ensuring strict isolation between local hardware state and remote telemetry (R-ID 390).

*   **Issue #1151: HardwareSuite Snapshot Unification** (Resolved Sep.21.122)
    *   *Remediation*: Unified `consumeLogicSnapshot` and `consumeForensicSnapshot` into a single private `privateConsumeSnapshot` method to eliminate duplicate state extraction boilerplate and guarantee symmetrical peak resets (R-ID 389).

*   **Issue #1123: Synchronous Thread Join in HardwareSuite Lifecycle** (Resolved Sep.21.121)
    *   *Remediation*: Removed synchronous `acousticThread.join(1000)` from `stopAcousticMonitoring()`. Resource exclusivity is now maintained via the join-before-start pattern in `startAcousticMonitoring()` (R-ID 387).

*   **Issue #1143: Divergent Vibration Floor Calculation (Dual-State Logic)** (Resolved Sep.21.121)
    *   *Remediation*: Unified the vibration floor authority in `HardwareSuite.kt`. The high-frequency `adaptiveVibrationFloor` is now snapshotted and propagated to `LocationSentinel` via `TrackerService` (R-ID 388).

*   **Issue #1146: GPS Data Loss in TrackerService Tick Conflation** (Resolved Sep.21.120)
    *   *Remediation*: Replaced single-point GPS conflation with `ConcurrentLinkedQueue` buffer in `TrackerService.kt` (R-ID 386).

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
- **Current Audit Baseline: [SOT: 391 (Rules: 80, IDs: 391), Resolved: 1143, Open: 0, Testing: 2 (Sub-items: 10), Ideas: 16, QA: 282]**
