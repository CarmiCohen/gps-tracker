# 🏛️ Resolution Archive - Sep.24.94

## 🏁 Issue #1311: AppAlarmManager Stateless Evaluation Model
*   **Resolved**: Sep.24.94
*   **Root Cause**: `AppAlarmManager` maintained redundant in-memory state (active alarms map, power pending flags, siren timestamps) parallel to the `AlarmEvaluationState` used by `MainAlarmLogic`. This duplication created a synchronization surface area that was prone to state leakage during role transitions (e.g., Tracker to Viewer) where stale memory maps could trigger incorrect alarm reactions before the next evaluation cycle.
*   **Remediation**:
    *   **EngineModels.kt**: Consolidated all evaluation metadata, including the `ActiveAlarm` registry and siren monotonic latches, into the `AlarmEvaluationState` DTO.
    *   **MainAlarmLogic.kt**: Refactored the detection engine to manage the lifecycle of `ActiveAlarm` objects directly within the provided state object, ensuring the evaluation is purely a function of its input state.
    *   **AppAlarmManager.kt**: Fully purged all local state variables and redundant maps. It now functions as a thin orchestrator that syncs telemetry into the `AlarmEvaluationState` and observes the output.
    *   **Architecture**: Achieved complete statelessness for the alarm evaluation pipeline, eliminating role-transition leakage vectors (R-ID 473).
*   **R-ID**: 473

# 🏛️ Resolution Archive - Sep.24.93

## 🏁 Issue #1265: Unified Event Orchestration via AppEventCoordinator
*   **Resolved**: Sep.24.93
*   **Root Cause**: Domain reactions (alerts, audio synthesis, forensic logging) were scattered across background services, `CommandRouter`, and `AppAlarmManager`, leading to high coupling and lifecycle dependencies. This made it difficult to ensure functional parity between roles and increased the risk of state leakage.
*   **Remediation**:
    *   **AppEventCoordinator.kt**: Created a central coordinator to orchestrate domain events. It observes reactive flows from `AppAlarmManager`, `IntegrityMonitor`, `HardwareSuite`, and `LocationProcessor`.
    *   **AppAlarmManager.kt**: Refactored to expose a reactive `isSirenRequired` StateFlow, removing imperative `AudioSynthesizer` calls from the evaluation loop.
    *   **MonitorService.kt**: Integrated `AppEventCoordinator` and removed redundant observation logic, significantly simplifying the background engine.
    *   **MainRepository.kt**: Added debounced baseline saving and synchronous location getters to support role-agnostic event reactions.
*   **R-ID**: 472

# 🏛️ Resolution Archive - Sep.24.92

## 🏁 Issue #1261: Refactor Tracker/Viewer Services into Role-Reactive MonitorService
*   **Resolved**: Sep.24.92
*   **Root Cause**: `TrackerService` and `ViewerService` shared significant redundant boilerplate for coroutine management, stream observation, lifecycle events, and forensic sampling. This duplication increased maintainability overhead and created risks of logic divergence between roles.
*   **Remediation**:
    *   **MonitorService.kt**: Created a unified, role-reactive background service that dynamically configures its logic based on the active `appModeFlow`.
    *   **Architecture**: Consolidated independent stream observation methods and job management into a shared engine. Implemented a dual-processor model for the Viewer role (local and remote) while maintaining single-processor efficiency for Trackers.
    *   **Manifest & Callers**: Updated `AndroidManifest.xml`, `MainActivity`, `WatchdogReceiver`, `BootReceiver`, and `MaintenanceWorker` to utilize the unified service.
    *   **Forensic Parity**: Merged the `forensicSamplingLoop` logic, ensuring identical audit precision and spike-trigger responsiveness for both monitor and tracking roles.
*   **R-ID**: 471
