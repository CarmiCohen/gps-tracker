# Forensic Handover (Sep.22.30)

## 🎯 Current System State
*   **Version**: Sep.23.50 | **Build**: Centralized SSOT ViewModel (Verified)
*   **SOT Baseline**: SOT: 422 (Rules: 86, IDs: 422)
*   **Core Remediation**: Successfully resolved **Issue #1203**. 
    *   Unified all role-specific logic (Tracker/Viewer/Setup) into `MainViewModel`.
    *   Eliminated redundant coroutine allocation churn by centralizing high-frequency data streams.
    *   Ensured Map ViewState and configuration drafts survive role transitions.
    *   Resolved kinematic state misrouting where screens were receiving empty location updates.

---

## 🛡️ Core Architecture Blueprint

1.  **Unified SSOT (#1203)**:
    *   `MainViewModel` acts as the single point of entry for all telemetry and user events.
    *   `TrackerScreen` and `ViewerScreen` are now stateless consumers of activity-scoped data.
    *   `DiagnosticState` and `KinematicState` are reset atomically during role switches via `UiEvent.SetAppMode`.
2.  **Hardware Strategy (#1204)**:
    *   OEM-specific power management centralized in `DeviceHardeningStrategy`.

---

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 422 (Rules: 86, IDs: 422), Resolved: 1185, Open: 28, Testing: 3 (Sub-items: 12), Ideas: 13, QA: 283]**

---

## 🛡️ Forensic Hardening Summary (Current Session Updates)

### 1. Issue #1203: Hilt ViewModel Scope Optimization
*   **Status**: Fully Resolved & Verified (Sep.22.30).
*   **Remediation**: Eliminated state fragmentation and multi-subscription resource churn by refactoring the app back to a centralized ViewModel model, significantly improving memory efficiency and UX consistency.

---

## 🔴 Open Gaps & Resumption Guidance
*   **Service Consolidation**: The next priority is **Issue #1261: Refactor Tracker/Viewer Services into MonitorService**, merging the redundant background service implementations into a single role-reactive component.
*   **Siren Integration**: Resolve **Issue #1280** to ensure the siren trigger logic in `AppAlarmManager` is actually consumed by the background service loop.
