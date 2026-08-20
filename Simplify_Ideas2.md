# Architectural Simplification Ideas (Aug.20.06)

Following the production hardening of the forensic pipeline, here are recommendations to further simplify the codebase and improve maintainability:

## 1. Telemetry Mapping Consolidation
- **Problem**: `MainRepository.kt`, `ConnectivitySuite.kt`, and `TelemetryUseCase.kt` contain redundant, manual mapping blocks between `HistoryEntity`, `PendingStatusEntity`, `TrackerStatus`, and `ConnectionPoint`.
- **Simplification**: Implement a unified `TelemetryMapper` or use Kotlin reflection/serialization to automate the transfer of common forensic fields. This reduces the risk of field-omission bugs (like the one fixed in R224).

## 2. UI Refresh Throttling
- **Problem**: `MainViewModel` uses multiple `sample()` and `combine()` blocks for dashboard and log states.
- **Simplification**: Centralize UI state sampling into a single `UiState` pulse to ensure consistency and reduce coroutine overhead.

## 3. Unified Event Bus Reduction
- **Observation**: The app currently uses `UiEvent`, `UiCommand`, and `CommandEvent` across multiple layers.
- **Simplification**: Consolidate into a single reactive `SystemEvent` stream. Many "UI Commands" are essentially service-layer actions that could be handled via a unified `ActionHandler` instead of passing through multiple sealed class translations.

## 4. Forensic Sampling State Machine
- **Observation**: `TrackerService.kt` contains manual logic for switching sampling intervals based on battery, thermal, and buffer pressure.
- **Simplification**: Move this logic into a dedicated `ForensicSamplingStrategy` class. This would decouple the background service from the specific interval math, making the core service loop easier to read and test.

## 5. Redundant Lifecycle Observers
- **Observation**: `MainActivity`, `MainAppContent`, and various Composables all have their own lifecycle listeners to refresh permissions or UI state.
- **Simplification**: Centralize lifecycle-dependent state updates into the `MainViewModel`. Using `lifecycle.repeatOnLifecycle` within the ViewModel can eliminate the need for multiple `DisposableEffect` observers in the UI layer.

## 6. ShadowCache Utility Standardisation
- **Observation**: With the introduction of `ShadowCache`, some legacy manual caching in `MainRepository` and `GpsApplication` remains.
- **Simplification**: Refactor all high-frequency lookups to use the centralized `ShadowCache`. This would eliminate custom synchronization logic scattered throughout the repository layer.
