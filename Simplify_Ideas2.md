# Simplification Ideas - vAug.20.03

Following the production hardening of the forensic pipeline, here are recommendations to further simplify the codebase and improve maintainability:

### 1. Unified Event Bus Reduction
- **Observation**: The app currently uses `UiEvent`, `UiCommand`, and `CommandEvent` across multiple layers.
- **Simplification**: Consolidate into a single reactive `SystemEvent` stream. Many "UI Commands" are essentially service-layer actions that could be handled via a unified `ActionHandler` instead of passing through multiple sealed class translations.

### 2. Forensic Sampling State Machine
- **Observation**: `TrackerService.kt` contains manual logic for switching sampling intervals based on battery, thermal, and buffer pressure.
- **Simplification**: Move this logic into a dedicated `ForensicSamplingStrategy` class. This would decouple the background service from the specific interval math, making the core service loop easier to read and test.

### 3. Redundant Lifecycle Observers
- **Observation**: `MainActivity`, `MainAppContent`, and various Composables all have their own lifecycle listeners to refresh permissions or UI state.
- **Simplification**: Centralize lifecycle-dependent state updates into the `MainViewModel`. Using `lifecycle.repeatOnLifecycle` within the ViewModel can eliminate the need for multiple `DisposableEffect` observers in the UI layer.

### 4. ShadowCache Utility Standardisation
- **Observation**: With the introduction of `ShadowCache`, some legacy manual caching in `MainRepository` and `GpsApplication` remains.
- **Simplification**: Refactor all high-frequency lookups to use the centralized `ShadowCache`. This would eliminate custom synchronization logic scattered throughout the repository layer.
