# Architectural Simplification Ideas (Aug.21.01)

Following the production hardening of the forensic pipeline and the HUD centralization, here are updated recommendations to further simplify the codebase:

## 1. Reusable Sensitivity Slider (New Aug.21.01)
- **Observation**: `AlertManagementOverlay` now contains duplicate `Column`+`Slider` logic for Vibration and Tilt sensitivity.
- **Simplification**: Extract this into a reusable `SensitivitySlider` composable in `SettingsComponents.kt` or `SharedUiComponents.kt`. This would centralize the styling (BrandJd colors, padding, percentage text) and make future sensor calibrations (e.g., Acoustic) trivial to implement.

## 2. UI State Aggregation (Aug.20.10)
- **Problem**: The `combine` blocks in `MainViewModel.kt` for `dashboardState` and `hudState` are reaching the 7-parameter limit and becoming difficult to maintain.
- **Simplification**: Extract state aggregation logic into a dedicated `UiStateAggregator` service.

## 3. HUD Parameter Decoupling (Aug.20.10)
- **Observation**: The migration to `HudState` (R240) improved component signatures, but `GlobalStatusBar` still has many individual parameters.
- **Simplification**: Further flatten the `StatusBar` hierarchy so child components consume sub-sections of `HudState` directly.

## 4. Dependency Management (Aug.20.10)
- **Simplification**: Move common dependencies into a `Version Catalog` (libs.versions.toml) for consistency across modules.

## 5. Telemetry Mapping Consolidation
- **Simplification**: Implement a unified `TelemetryMapper` to eliminate redundant manual mapping blocks across the repository and service layers.

## 6. UI Refresh Throttling
- **Simplification**: Centralize UI state sampling into a single `UiState` pulse to ensure consistency and reduce coroutine overhead.

## 7. Unified Event Bus Reduction
- **Simplification**: Consolidate `UiEvent`, `UiCommand`, and `CommandEvent` into a single reactive `SystemEvent` stream.

## 8. Forensic Sampling State Machine
- **Simplification**: Move sampling interval logic into a dedicated `ForensicSamplingStrategy` class to decouple `TrackerService.kt` from specific interval math.

## 9. Redundant Lifecycle Observers
- **Simplification**: Centralize lifecycle-dependent state updates into the `MainViewModel` using `lifecycle.repeatOnLifecycle`.

## 10. ShadowCache Utility Standardisation
- **Simplification**: Refactor all high-frequency lookups to use the centralized `ShadowCache` to eliminate custom synchronization logic.
