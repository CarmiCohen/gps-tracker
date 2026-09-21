# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*(No critical gaps identified. Refactoring of forensic history is complete).*

### Unintended Side Effects & Thread Safety
*(No critical side-effects identified in current audit path).*

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1158: GNSS Sampling Logic Consolidation** (Resolved Sep.21.128)
    *   *Remediation*: Encapsulated GNSS sampling policy (standard vs throttled) and auditing triggers in a nested `GnssPolicyEngine` within `HardwareSuite.kt`. This decouples the hardware callback from throttling rules and ensures symmetric auditing of jitter across all performance tiers (R-ID 394).

*   **Issue #1156: Unused Forensic Abstraction** (Resolved Sep.21.127)
    *   *Remediation*: Refactored `HistoryManager.backfillAnalyticalGaps` to consume the specialized `EngineAcousticSample` sequence from `HardwareSuite.getAcousticSamples`, eliminating dead code and ensuring environmental noise is captured with semantic precision (R-ID 393).

*   **Issue #1157: Telemetry Abstraction Integration (Idea 5)** (Resolved Sep.21.127)
    *   *Remediation*: Refactored `HistoryManager.fillRealGap` and `TelemetryAggregator` to consume `EngineAcousticSample` directly. This completes the decoupling of environmental noise (dB) from satellite SNR in the forensic ribbon history (R-ID 393).

*   **Issue #1155: Acoustic-SNR Semantic Mismatch** (Resolved Sep.21.125)
    *   *Remediation*: Introduced `EngineAcousticSample` and refactored `HardwareSuite.getAcousticSamples` to return a sequence of this new type (R-ID 393).

*   **Issue #1153: Forensic Sequence Race Condition** (Resolved Sep.21.125)
    *   *Remediation*: Refactored `CircularStateBuffer.forensicSequence` to use a multi-pass custom Sequence implementation with locking (R-ID 392).

*   **Issue #1154: Forensic Allocation Spike** (Resolved Sep.21.125)
    *   *Remediation*: Replaced `toList()` snapshot with a lazy, zero-allocation sequence iteration (R-ID 392).

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 394 (Rules: 80, IDs: 394), Resolved: 1149, Open: 0, Testing: 2 (Sub-items: 10), Ideas: 16, QA: 282]**
