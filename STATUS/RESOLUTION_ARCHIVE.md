# 🏛️ Resolution Archive - Sep.27.14

## 🏁 Issue #1293: Lifecycle-Aware Tick Orchestrator
*   **Resolved**: Sep.27.14
*   **Root Cause**: Background services managed multiple internal loops (`tickJob`, `heartbeatJob`, `forensicSamplingJob`) with decentralized synchronization gates (`initializationDeferred`). This setup scattered state-checking responsibilities across components, risking unpredictable lifecycle transitions and complicating testing.
*   **Remediation**:
    *   **TickOrchestrator.kt**: Created a single standalone, thread-safe orchestrator handling initialization completeness gates, named loop builder lifecycles, and centralized atomic cancellations.
    *   **BaseMonitorService.kt** & **MonitorService.kt**: Removed separate manual job structures and deferred waits, routing all tick, heartbeat, and forensic loops through `TickOrchestrator` to secure deterministic structured concurrency.
*   **SOT ID**: 514 (Lifecycle-Aware Tick Orchestration)

## 🏁 Issue #1351: StateSubscription Coroutine Scoping
*   **Resolved**: Sep.27.13
*   **Root Cause**: `MainViewModel.startBaseObservations` utilized multiple separate `.onEach { ... }.launchIn(viewModelScope)` data stream allocations. This led to fragmented subscription scopes, separate job allocations, and redundant `.flowOn(Dispatchers.Main.immediate)` specifications, violating structured concurrency orchestration principles.
*   **Remediation**:
    *   **MainViewModel.kt**: Refactored `startBaseObservations` to orchestrate all reactive data streams inside a single parent coroutine block. Used child `launch` coroutine builders for individual flow collections, eliminating redundant launch overhead and ensuring atomic structured execution.
*   **SOT ID**: 513 (Unified StateSubscription Coroutine Scoping)

## 🏁 Issue #1350: Unified State Mapping Authority
*   **Resolved**: Sep.27.12
*   **Root Cause**: `MainViewModel` was performing complex reactive state projections for Dashboard, HUD, and Map views, leading to a bloated ViewModel and violating the "Perfectly Thin ViewModel" architectural goal. The mapping logic was coupled with the ViewModel's lifecycle scope instead of being managed by a dedicated projection authority.
*   **Remediation**:
    *   **UiStateCoordinator.kt**: Introduced a dedicated authority to manage all reactive state projections (Dashboard, HUD, MapViewState) and trail segment computations.
    *   **MainViewModel.kt**: Refactored to delegate all state projection logic to `UiStateCoordinator`, reducing its complexity and achieving a perfectly thin orchestration pattern.
    *   **Architecture**: Unified the reactive projection pipeline, ensuring consistency across all UI components and improving testability by isolating mapping logic from ViewModel lifecycle management.
*   **SOT ID**: 512 (Unified State Mapping Authority)

## 🏁 Issue #1202: UI Event Routing Unification
*   **Resolved**: Sep.27.11
*   **Root Cause**: `MainViewModel.onEvent` contained a massive, procedural `when (event)` block that directly orchestrated UseCases, persistence, and state updates. This pattern created a tight coupling between user interface events and domain orchestration, leading to a "God Object" architecture in the ViewModel and increasing maintenance surface area.
*   **Remediation**:
    *   **UiEventCoordinator.kt**: Introduced a central, standalone `UiEventCoordinator` component to manage all routing of `UiEvent`s to their corresponding domain UseCases and Repositories.
    *   **MainViewModel.kt**: Completely refactored `onEvent` to delegate directly to `UiEventCoordinator`, drastically reducing its internal logic surface area and architectural complexity.
    *   **Settings Management**: Unified and moved the debounced draft auto-save and draft preparation logic into the coordinator layer to maintain pure state handling in the ViewModel tier.
*   **SOT ID**: 511 (UI Event Routing Unification)

## 🏁 Issue #1201: Reactive Siren Lockout
*   **Resolved**: Sep.27.10
*   **Root Cause**: The siren lockout and cooldown state (`silencedUntilRt`) was directly managed within the procedural audio generation component (`AudioSynthesizer`). This created a tight coupling between domain policy (when a siren is permitted to play) and hardware/audio synthesis, violating clean separation of concerns and preventing other domain/UI components from reactively observing the lockout status.
*   **Remediation**:
    *   **SirenLockoutUseCase.kt**: Introduced a dedicated, thread-safe `SirenLockoutUseCase` to act as the single source of truth for siren lockout state using a reactive `StateFlow<Long>`.
    *   **AudioSynthesizer.kt**: Refactored to remove all independent lockout state management, querying `SirenLockoutUseCase` directly to gate audio generation.
    *   **AppAlarmManager.kt**: Refactored to observe the reactive `silencedUntilRt` flow from `SirenLockoutUseCase` and automatically re-evaluate system-wide siren playback requirements upon lockout changes.
    *   **CommandRouter.kt**: Updated command handling paths to update the centralized lockout authority.
    *   **MainUiState.kt**: Extended `DiagnosticState` with a `silencedUntilRt` property to enable full visibility and reactivity in the UI tier.
*   **SOT ID**: 510 (Reactive Siren Lockout)

## 🏁 Issue #1290: UI State Mapper Consolidation
*   **Resolved**: Sep.27.9
*   **Root Cause**: The existence of a separate, stateless `UiStateMapper` interface and implementation layer introduced unnecessary DI surface area and increased the cognitive load for maintaining UI state transformations, despite the logic being exclusively consumed by `MainViewModel`.
*   **Remediation**:
    *   **MainViewModel.kt**: Physically integrated all `Dashboard` and `HUD` state mapping logic as private helper functions. Removed the `UiStateMapper` dependency.
    *   **AppModule.kt**: Removed the `UiStateMapper` binding.
    *   **Cleanup**: Decommissioned `UiStateMapper.kt` and verified the removal of the redundant mapping layer.
*   **SOT ID**: 509 (UI Mapping Consolidation)
