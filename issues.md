# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.5.9

## 🎯 Current Resumption Focus: Issue #1426 Audit.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   (All current high-priority gaps resolved).

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   (All medium-priority simplification ideas resolved).

### 🔵 Low Priority
*   **Issue #1426: Composable Effect Aggregator**
    *   *Significance*: **Low (Simplicity)**. Centralize all `LaunchedEffect(Unit)` observers in `MainAppContent` into a single wrapper to reduce boilerplate in the root UI file.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #1295: Redundant Stream Observer Audit.** Resolved Oct.5.9. Implemented comprehensive interval relaxation for reactive streams and periodic loops during verified stationary periods (`isUltraLongStationary`). 
    *   Relaxed `IntegrityMonitor` hardware heartbeat to 60s.
    *   Relaxed `MonitorService` forensic sampling background timeout to 5s.
    *   Relaxed `ConnectivitySuite` peer heartbeat to 5m.
    *   Synchronized `MonitorService` tick interval with relaxed GPS polling (5m) to minimize CPU wakeups and radio churn during long-term immobility. (R1295).
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator.** Resolved Oct.5.8. Refactored `BaseMonitorService` tick and heartbeat loops into `TickOrchestrator`. (R1293).
*   **Issue #1450: JNI Math Batching.** Resolved Oct.5.7. (R1450).
*   **Issue #1328: Event Bus Backpressure & UI Performance Hardening.** Resolved Oct.5.6. (R1328, R1422).
*   **Issue #SIMP-1510-1: Native FastPath Convergence (Phase 2).** Resolved Oct.5.5. (R1510-2).
*   **Issue #1344: Forensic Diagnostic Expansion.** Resolved Oct.5.2. (R1344).

---

## 📊 Hardening Progress Dashboard
- **Oct.5.9: [SOT Count: 277 (Rules: 136), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 37, QA: 420]**
- **Oct.5.8: [SOT Count: 276 (Rules: 135), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:2, Testing: 36, QA: 415]**
- **Oct.5.7: [SOT Count: 275 (Rules: 134), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:3, Testing: 35, QA: 410]**
