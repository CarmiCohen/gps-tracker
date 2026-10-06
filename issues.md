# Project Issues & Hardening Tracking (Rigorous Audit) - Oct6.1

## 🎯 Current Resumption Focus: Issue #AUDIT-1006-2: Background Service Transition Latency Audit.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   **Issue #AUDIT-1006-2: Background Service Transition Latency Audit.** Perform regression testing on background service transition latency following the removal of root-level UI invalidation triggers. Ensure the app transitions to/from background services (like AlarmOverlayService) without perceptible lag or state desync. (Target: Oct6.1).

---

## 💡 Strategic Simplification Ideas (Ideas: 0)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

### 🟡 Medium Priority
*   (All medium-priority simplification ideas resolved).

### 🔵 Low Priority
*   (All low-priority simplification ideas resolved).

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #AUDIT-1006-1: AlarmOverlayService State Leak Audit & Refinement.** Resolved Oct6.1. Refactored `AlarmOverlayService` to initialize `SimpleUiStateProvider` and its associated flows at the service level rather than inside the composition. This prevents flow instability and potential memory leaks during service lifecycle transitions. (R-ID 288).
*   **Issue #SIMP-1426-4: Map Hardening & Boilerplate Reduction.** Resolved Oct.5.21. Migrated `initialCenter` and coordinate smoothing triggers into `MainViewModel`'s `mapMapViewState` logic. Refactored `TrackerScreen` and `ViewerScreen` signatures to accept `UiStateProvider`, further reducing parameter distribution boilerplate by ~80 lines. Verified all leaf components collect from specialized flows to prevent root-level invalidation. (R1426-4, R-ID 287).
*   **Issue #SIMP-1426-3: Unified State Provider.** Resolved Oct.5.20. Introduced `UiStateProvider` interface in `MainUiState.kt` and implemented it in `MainViewModel`. Refactored all leaf components (`LogOverlay`, `SettingsOverlay`, `PhoneSetupOverlay`, `AlarmOverlay`, `GlobalStatusBar`, `RibbonsOverlay`, `TrackerDashboard`, `ViewerDashboard`, `AppMapContainer`, `DiagnosticsScreen`) to consume the unified provider. Eliminated ~150 lines of redundant parameter distribution logic. (R1426-3, R1.116).
*   **Issue #SIMP-1426-2: Leaf-Level Convergence.** Resolved Oct.5.15. Migrated state collection for all overlays and dashboards to leaf-level Flow collection. Adhered to Rule 1.110 to isolate root-level UI from telemetry bursts. (R1426-2, R1.115).
*   **Issue #SIMP-1426-1: Leaf Effect Convergence.** Resolved Oct.5.12. Centralized "System Readiness" and "Issue Count" logic in `SessionUiState`. (R1426).
*   **Issue #1426 Performance Audit: Side-Effect Latency Verification.** Resolved Oct.5.11. (R1426, R1.111).
*   **Issue #1426: Composable Effect Aggregator.** Resolved Oct.5.10. (R1426, R1.114).
*   **Issue #1295: Redundant Stream Observer Audit.** Resolved Oct.5.9. (R1295).
*   **Issue #1293: Lifecycle-Aware Tick Orchestrator.** Resolved Oct.5.8. (R1293).
*   **Issue #1450: JNI Math Batching.** Resolved Oct.5.7. (R1450).
*   **Issue #1328: Event Bus Backpressure & UI Performance Hardening.** Resolved Oct.5.6. (R1328, R1422).
*   **Issue #SIMP-1510-1: Native FastPath Convergence (Phase 2).** Resolved Oct.5.5. (R1510-2).
*   **Issue #1344: Forensic Diagnostic Expansion.** Resolved Oct.5.2. (R1344).

---

## 📊 Hardening Progress Dashboard
- **Oct6.1: [SOT Count: 284 (Rules: 141), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 41, QA: 455]**
- **Oct.5.21: [SOT Count: 283 (Rules: 140), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 40, QA: 450]**
- **Oct.5.20: [SOT Count: 282 (Rules: 139), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 40, QA: 450]**
- **Oct.5.15: [SOT Count: 281 (Rules: 138), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 39, QA: 445]**
- **Oct.5.12: [SOT Count: 280 (Rules: 137), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 39, QA: 430]**
- **Oct.5.11: [SOT Count: 279 (Rules: 137), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 38, QA: 425]**
- **Oct.5.10: [SOT Count: 278 (Rules: 137), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 37, QA: 420]**
