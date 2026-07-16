# Project Issues & Hardening Tracking (July.16.19)

This document tracks active issues, technical debt, and pending implementation tasks.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 291 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **ConnectivitySuite Scope**: Consolidates network, sync, and remote handling. Maintain internal modularity to prevent bloat.
*   **AppContainer Circularity**: Resolved via lambda/lazy. Must be preserved in future DI changes.
*   **AlarmHistory Persistence**: `AlarmHistory` is currently held in memory within `AppAlarmManager`. If the process is killed, transient states like `firstViolationTs` are reset. This is consistent with previous behavior but should be noted if persistent geofence debouncing is required across restarts.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (July.16.19)
*   **Issue #517 (R406k)**: Refactor AppAlarmManager (De-duplicate local state flags).
    *   Consolidated persistent evaluation flags (`powerAlarmPending`, `wasDistanceViolated`, `distanceViolationCounter`, `firstViolationTs`, `firstViolationWasJump`) into a single `AlarmHistory` model in `:core:engine`.
    *   Refactored `AppAlarmManager` to remove redundant local variables and use the centralized history object.
    *   Updated `MainAlarmLogic` and `AlarmEvaluationState` to operate on `AlarmHistory`.
    *   Simplified `resetEvaluation` logic.

*   **Issue #516 (R406j)**: De-duplicate "Status" Logic.
    *   Created `SystemHealthState` in `:core:engine` as the authoritative model for device metadata.
    *   Refactored `IntegrityMonitor` to produce `SystemHealthState`.
    *   Unified `TelemetryRepository` and `MainRepository` to expose `systemHealth` flow.
    *   Purged legacy `IntegrityState` and `IntegrityStateUi` models.
    *   Simplified `LocationState` to contain only pure position data.

*   **Issue #514 (R406i)**: Simplify GpsManager.
    *   Removed `kickGps` and `reviveGps` legacy commands.
    *   Removed `snrBuffer` and `getSnrSamples`.
    *   Simplified `GnssStatus.Callback` to provide only immediate metadata.
    *   Refactored `HistoryManager` and `TelemetryAggregator` to remove SNR sampling dependencies.
    *   Purged `EngineSnrSample` from models.

*   **Issue #513 (R406h)**: Flatten Service Architecture (ConnectivitySuite).
    *   Merged `AppNetworkManager`, `SyncManager`, and `RemoteHandler` into `ConnectivitySuite`.
    *   Removed redundant `RemoteUpdateWrapper`.
    *   Streamlined service dependency graph.
