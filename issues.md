# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.1

## 🎯 Current Resumption Focus: Chapter 4 Audit (Viewer Mode & Remote Sync)

## 🔴 Open Gaps & Unfinished Integration Points
*   (All High-priority gaps for Oct14.1 resolved)

---

## 💡 Strategic Simplification Ideas (Ideas: 31)
*   **SIMP-OCT16.2-S1: Repository State Projection (Low).** Consider extracting a `StateProvider` interface from `MainRepository` to allow UseCases to observe specific sub-states without full repository access.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT16.8-F1: Missing Termination Confirmation Dialog.** Implemented `DialogHost` in `MainAppContent.kt` to ensure termination dialogs are not obscured by overlays. (SIMP-OCT16.8-S1)
*   **ISSUE-OCT16.8-F2: Role Transition Grace Period Violation.** Added 30s hardware boot grace to `AppAlarmManager` and fixed `isTrackerMode` atomic synchronization.
*   **SOT ID 749: Project-Wide Hardening Audit (Oct14.1).** Verified Chapter 3 (Tracker Mode Operation) on physical A15 hardware. Confirmed stealth sentinel, service persistence, and HUD logic.
*   **SOT ID 748: Test State Pre-warming.** Formalized global state reset.
*   **ISSUE-OCT16.2-F1: Hydration Test Timeout.** Fixed OSM readiness stall.
*   **SIMP-OCT15.10-S1: Test Utility Decoupling.** Decoupled `MainFileHelper` from `MainViewModel`.

---

## 📊 Hardening Progress Dashboard
- **Oct14.1: [SOT Count: 443 (Rules: 281), Open: H:0, M:0, L:0, Ideas: H:0, M:10, L:21, Testing: 21 (Sub-items: 155), QA: 358]**
- **Oct14.1: [SOT Count: 441 (Rules: 279), Open: H:1, M:0, L:0, Ideas: H:0, M:11, L:21, Testing: 21 (Sub-items: 150), QA: 353]**
