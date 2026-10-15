# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.2

## 🎯 Current Resumption Focus: Final integrity audit and release block.

## 🟢 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT14.21-QA1: Manual Role Transition Audit (High).** Perform final physical verification of Tracker -> Viewer mode swaps on A15 hardware to confirm JNI and GPU stability under sustained load.

---

## 💡 Strategic Simplification Ideas (Ideas: 23)
*   **SIMP-OCT14.21-S1: MapViewState Mapper Extraction (Low).** Consider moving `mapMapViewState` and `computeTrailSegments` from `MainViewModel` to a dedicated `MapStateMapper` for complete "Pure Flat" architectural separation.
*   **SIMP-OCT14.10-S1**: HUD Mapper Extraction (Low). RESOLVED Oct14.2. Extracted to `HudMapper.kt`.
*   **SIMP-OCT14.10-S2**: Dashboard Mapper Extraction (Low). RESOLVED Oct14.2. Extracted to `DashboardMapper.kt`.
*   **SIMP-OCT14.11-S1**: Forensic Spill Flush Optimization (Medium). RESOLVED Oct14.2. Moved `MappedByteBuffer.force()` outside the synchronized block to reduce contention.
*   **SIMP-OCT14.13-S1: Atomic Mutation Simplification (High).** RESOLVED Oct14.1. Migrated custom mutex/non-atomic Read-Modify-Write in DataStore to internal transformation blocks within `updateData`.
*   **SIMP-OCT14.13-S2: Alarm Entity Resolver (Medium).** RESOLVED Oct14.1. Centralized 'Tracker' vs 'Viewer' naming logic into `EntityBrandingProvider`.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT14.0-F1: DataStore Mutation Integrity.** Resolved Oct14.1. Hardened `SettingsRepository` with functional transformation API (`incrementInt`) and ensured atomic `commitDraftSettings`. Verified via `DataStoreMutationStressTest`.
*   **ISSUE-OCT14.0-F2: Alarm mode-awareness branding (Issue #1413).** Resolved Oct14.1. Centralized branding logic in `EntityBrandingProvider` and synchronized `EngineConstants`.
*   **ISSUE-OCT14.14-P1: Physical Hardware Verification (A15/S21FE).** Resolved Oct14.1.
*   **ISSUE-OCT14.12-I1: S21FE Instrumental Test Verification (5m).** Resolved Oct14.1.
*   **ISSUE-OCT14.10-I1: A15 Instrumental Test Verification (30m).** Resolved Oct14.1.
*   **ISSUE-OCT14.5-H1: Full Unit Test Verification.** Resolved Oct14.1.

---

## 📊 Hardening Progress Dashboard
- **Oct14.21: [SOT Count: 421 (Rules: 260), Open: H:1, M:0, L:1, Ideas: H:0, M:10, L:6, Testing: 13 (Sub-items: 122), QA: 326]**
- **Oct14.1: [SOT Count: 417 (Rules: 258), Open: H:0, M:0, L:0, Ideas: H:0, M:12, L:8, Testing: 13 (Sub-items: 120), QA: 324]**
