# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

1. **Issue #1074: Telemetry Backfill QA Task**
    *   *Finding*: QA Verification of telemetry backfill convergence (R-ID 17 / Signaling Continuity).
    *   *Status*: **Open**.

---

## 🟢 Resolved Traceability & Metadata Issues (Audit: Sep.17.00)

1. **Issue #1073: Event Log Erasure Defect**
    *   *Finding*: Erasing the event log button does not erase the log.
    *   *Action*: Handled `UiEvent.ClearLogs` explicitly inside `MainViewModel.kt` to trigger `repository.clearLogs()`.
    *   *Status*: **Resolved**.

2. **Issue #1051: Signaling Conflation Traceability**
    *   *Finding*: `CommunicationManager.kt` utilized hardcoded literals (100ms/20ms) in `emitLocationConflated`.
    *   *Action*: Migrated these to `SIGNALING_CONFLATION_DELAY_MS` and `SIGNALING_CONFLATION_DELAY_VIOLATION_MS` in `EngineConstants.kt` to comply with R312.
    *   *Status*: **Resolved**.

3. **Issue #1060: Metadata & Traceability Consolidation**
    *   *Audit Findings*: RIGOROUSLY VERIFIED. Header comments across all core services and providers are aligned with **R-ID 348**. 
    *   *Status*: **Resolved**.

4. **Issue #1052: ProductionReadinessAuditTest.kt Reference Inconsistency**
    *   *Audit Findings*: RIGOROUSLY VERIFIED. Incorrect references to R-ID 343 and Sep.15.14 removed. Test suite correctly cites **R-ID 348**.
    *   *Status*: **Resolved**.

5. **Issue #1072: Static State Leakage in Test Fakes**
    *   *Audit Findings*: Verified reset mechanism in `@Before` block of `ProductionReadinessAuditTest.kt`.
    *   *Status*: **Resolved**.

6. **Issue #1050: Deterministic Doze State Simulation**
    *   *Audit Findings*: Verified `PowerStateProvider` abstraction and Hilt module replacement in audit tests.
    *   *Status*: **Resolved**.

7. **Issue #1055/1056: Unified Performance Tier & Hydration**
    *   *Audit Findings*: Broadened throttling and staggered hydration now correctly leverage the `PerformanceTier` enum for both A15 and S21FE devices.
    *   *Status*: **Resolved**.

8. **Issue #1071: Process Death Resilience Validation**
    *   *Audit Findings*: Verified that `FakePowerStateProvider` maintains state across policy re-instantiation in tests via static companion variables.
    *   *Status*: **Resolved**.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 352 (Rules: 71, IDs: 352), Resolved: 1091, Open: 1, Testing: 0, Ideas: 18, QA: 281]**
