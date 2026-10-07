# Project Issues & Hardening Tracking (Rigorous Audit) - Oct7.1

## 🎯 Current Resumption Focus: Protocol Optimization & Wire-Level Efficiency.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   **Issue #QA-1006-12: Android 15 (API 35) Deployment & Forensic Audit**. (Oct6.21)
    *   **CRITICAL FIX**: Resolved `SQLiteConstraintException` on `connection_history.vibeIdx` NOT NULL constraint (Oct6.21).
    *   **Signaling Efficiency Audit**: Resolved 0% Conflation Savings for logs. Hardened `SmartSignalingDispatcher` with dynamic window extension for log bursts (Oct6.23).
    *   Validate signaling architecture on Android 15.
    *   Monitor radio efficiency and background channel recovery.

---

## 💡 Strategic Simplification Ideas (Ideas: 0)
*   *(No open ideas)*

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1006-14: Telemetry Field Pruning.** Resolved Oct7.1. Marked engine-internal evaluation and scratchpad fields in `LocationUpdate` as `@Transient` to reduce JSON wire size.
*   **Issue #SIGN-1006-13: Consolidate Conflation Strategies.** Resolved Oct6.23. Migrated logic from `SignalingMessageConflator` to `SmartSignalingDispatcher`.
*   **Issue #SIGN-1006-12: SignalingPipeline Abstraction.** Resolved Oct6.20.
*   **Issue #AUDIT-1006-11: Protobuf Stream Compression (Gzip).** Resolved Oct6.15.

---

## 📊 Hardening Progress Dashboard
- **Oct7.1: [SOT Count: 306 (Rules: 161), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 57, QA: 577]**
- **Oct6.23: [Signaling Efficiency: Fixed 0% log conflation savings. Consolidated conflation logic into pipeline internal handlers (Issue #SIGN-1006-13).]**
- **Oct6.21: [Defect Identified: SQLiteConstraintException. Pipeline hardening deployed.]**
- **Oct6.20: [SOT Count: 304 (Rules: 160), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 55, QA: 575]**
