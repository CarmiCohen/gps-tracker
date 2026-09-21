# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*   **Issue #1156: Unused Forensic Abstraction**: `HardwareSuite.getAcousticSamples` returns a specialized `EngineAcousticSample` sequence, but it is currently not consumed by `HistoryManager.backfillAnalyticalGaps`, making the refactoring of this specific method dead code until integrated.
    *   *File*: `HardwareSuite.kt` (Line 571)
*   **Issue #1157: Telemetry Abstraction Integration (Idea 5)**: Refactor `HistoryManager.backfillAnalyticalGaps` and `HistoryManager.fillRealGap` to consume `EngineAcousticSample` directly from `HardwareSuite`, ensuring the forensic ribbon accurately reflects environmental noise without SNR ambiguity.
    *   *File*: `HistoryManager.kt`

### Unintended Side Effects & Thread Safety
*(No critical side-effects identified in current audit path).*

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1155: Acoustic-SNR Semantic Mismatch** (Resolved Sep.21.125)
    *   *Remediation*: Introduced `EngineAcousticSample` and refactored `HardwareSuite.getAcousticSamples` to return a sequence of this new type. This ensures environmental noise telemetry is semantically decoupled from satellite SNR in the forensic buffer (R-ID 393).

*   **Issue #1153: Forensic Sequence Race Condition** (Resolved Sep.21.125)
    *   *Remediation*: Refactored `CircularStateBuffer.forensicSequence` to use a multi-pass custom Sequence implementation that holds locks during flyweight transformation, preventing data corruption during high-frequency writes (R-ID 392).

*   **Issue #1154: Forensic Allocation Spike** (Resolved Sep.21.125)
    *   *Remediation*: Replaced `toList()` snapshot with a lazy, zero-allocation sequence iteration under internal lock (R-ID 392).

*   **Issue #1152: Flyweight Sequence Abstraction** (Resolved Sep.21.124)
    *   *Remediation*: Refactored `getSnrSamples`, `getSensorSamples`, and `getAcousticSamples` in `HardwareSuite` to use a unified `forensicSequence` utility in `CircularStateBuffer` (R-ID 391).

*   **Issue #1121: Telemetry Source Abstraction** (Resolved Sep.21.123)
    *   *Remediation*: Introduced `AlarmTelemetrySnapshot` and `AlarmServiceContext` to unify telemetry propagation (R-ID 390).

*   **Issue #1151: HardwareSuite Snapshot Unification** (Resolved Sep.21.122)
    *   *Remediation*: Unified `consumeLogicSnapshot` and `consumeForensicSnapshot` into a single private method to ensure symmetrical state management (R-ID 389).

*   **Issue #1123: Synchronous Thread Join in HardwareSuite Lifecycle** (Resolved Sep.21.121)
    *   *Remediation*: Removed synchronous `join()` from `stopAcousticMonitoring` to eliminate lifecycle stalls (R-ID 387).

*   **Issue #1143: Unified Vibration Authority** (Resolved Sep.21.121)
    *   *Remediation*: Consolidated `adaptiveVibrationFloor` authority in `HardwareSuite.kt` (R-ID 388).

*   **Issue #1146: GPS Data Loss in TrackerService Tick Conflation** (Resolved Sep.21.120)
    *   *Remediation*: Replaced single-point GPS conflation with `ConcurrentLinkedQueue` buffer (R-ID 386).

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 393 (Rules: 80, IDs: 393), Resolved: 1146, Open: 2, Testing: 2 (Sub-items: 10), Ideas: 17, QA: 282]**
