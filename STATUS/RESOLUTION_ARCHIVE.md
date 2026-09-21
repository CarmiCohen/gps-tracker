# Resolution Archive & Forensic Traceability Log

## 🟢 Resolved Issues & Refactoring Record

### Sep.21.128
*   **Issue #1158: GNSS Sampling Logic Consolidation**
    *   *Remediation*: Encapsulated GNSS sampling policy (standard vs throttled) and auditing triggers in a nested `GnssPolicyEngine` within `HardwareSuite.kt`. This decouples the hardware callback from throttling rules and ensures symmetric auditing of jitter across all performance tiers (R-ID 394).

### Sep.21.127
*   **Issue #1156: Unused Forensic Abstraction**
    *   *Remediation*: Refactored `HistoryManager.backfillAnalyticalGaps` to consume the specialized `EngineAcousticSample` sequence from `HardwareSuite.getAcousticSamples`, eliminating dead code and ensuring environmental noise is captured with semantic precision (R-ID 393).
*   **Issue #1157: Telemetry Abstraction Integration (Idea 5)**
    *   *Remediation*: Refactored `HistoryManager.fillRealGap` and `TelemetryAggregator` to consume `EngineAcousticSample` directly. This completes the decoupling of environmental noise (dB) from satellite SNR in the forensic ribbon history (R-ID 393).

### Sep.21.125
*   **Issue #1155: Acoustic-SNR Semantic Mismatch**
    *   *Remediation*: Introduced `EngineAcousticSample` and refactored `HardwareSuite.getAcousticSamples` to return a sequence of this new type (R-ID 393).
*   **Issue #1153: Forensic Sequence Race Condition**
    *   *Remediation*: Refactored `CircularStateBuffer.forensicSequence` to use a multi-pass custom Sequence implementation with locking (R-ID 392).
*   **Issue #1154: Forensic Allocation Spike**
    *   *Remediation*: Replaced `toList()` snapshot with a lazy, zero-allocation sequence iteration (R-ID 392).
