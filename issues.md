# Project Issues & Hardening Tracking (Rigorous Audit) - Oct15.10

## 🎯 Current Resumption Focus: Unit Test Integrity Verified (ISSUE-OCT15.10-F1).

## 🟢 Open Gaps & Unfinished Integration Points
*   *No active gaps.*

---

## 💡 Strategic Simplification Ideas (Ideas: 29)
*   **SIMP-OCT15.10-S1: Test Utility Decoupling (Low).** Many tests currently rely on `mock<MainViewModel>` even when testing pure mappers. Transitioning these to pure unit tests without ViewModel mocks would reduce test execution time and fragility.
*   **SIMP-OCT15.9-S1: Centralized Health Observation (Medium).** `LogManager` and `MainRepository` both manually subscribe to `TelemetryRepository.systemHealth`. Consolidating this into a shared `HealthMonitor` could reduce redundant flow collections.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT15.10-F1: Unit Test Suite Restoration.** Resolved Oct15.10. Updated `DashboardContinuityTest`, `MainViewModelMappingTest`, and `UxLogicFormattingTest` to align with the Pure Mapping Architecture (SIMP-OCT14.21-S1). Restored 100% unit test pass rate.
*   **ISSUE-OCT15.9-F1: Stress Test Verification.** Resolved Oct14.1. Integrated log gating into `PersistencePolicy` and refactored `LogManager` to eliminate fragmented IO rules. Fixed `StoragePressureAuditTest` to handle `TrailBuffer` primitive arrays. Corrected `SettingsRepository` identity conflict error message.
*   **ISSUE-OCT15.8-F1: UI Layer Integrity Audit.** Resolved Oct14.1. Refactored `ConnectionQualityRibbon` to eliminate redundant runtime calculations by consuming `HistoryBuffer.commNorm` directly. Verified O(1) rendering integrity across all forensic ribbons.
*   **ISSUE-OCT15.7-F1: Persistence Policy & Repository Consolidation.** Resolved Oct14.1. Expanded `PersistencePolicy` and consolidated all mapping into `TelemetryMapper`. Enforced strict IO gating for Mali anomalies across all repository paths.

---

## 📊 Hardening Progress Dashboard
- **Oct15.10: [SOT Count: 431 (Rules: 270), Open: H:0, M:0, L:0, Ideas: H:0, M:11, L:7, Testing: 16 (Sub-items: 125), QA: 333]**
- **Oct14.1: [SOT Count: 431 (Rules: 270), Open: H:0, M:0, L:0, Ideas: H:0, M:11, L:6, Testing: 16 (Sub-items: 125), QA: 333]**
- **Oct14.1: [SOT Count: 430 (Rules: 269), Open: H:0, M:0, L:0, Ideas: H:0, M:10, L:6, Testing: 16 (Sub-items: 125), QA: 332]**
