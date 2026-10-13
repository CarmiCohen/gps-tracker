# Resolution Archive

## [Oct.13.2] - Mapping Test Hardening
**Issue ID**: #SIMP-OCT11.20-S1
**Status**: RESOLVED
**Description**: Hardened unit tests for MainViewModel smoothing projections to ensure correct EMA alpha branch coverage by using high-fidelity kinematic test data.
**Root Cause**: Unit tests were defaulting to stationary speed (0.0 m/s), which triggered the stationary smoothing alpha (0.1) instead of the default movement alpha (0.3), causing coordinate drift assertions to fail.
**Remediation**:
- **Test Hardening**: Updated `MainViewModelMappingTest.kt` to ensure mock `KinematicState` uses non-stationary speed (> 0.5 m/s) for smoothing integration tests.
- **Verification**: Verified that all 29 tests in `:app` pass, specifically validating the 0.3 alpha branch for moving markers.

## [Oct10.3] - Unified HUD Overlay
**Issue ID**: #SIMP-OCT12.16-S1
**Status**: RESOLVED
**Description**: Merged HeaderBar, GlobalStatusBar, and map overlays into a unified `OverlayManager` to prevent Z-order collisions and simplify state management.
**Root Cause**: Multiple overlays were managed independently across `ViewerScreen`, `TrackerScreen`, and `AppMapContainer`, leading to Z-order collisions (e.g., scale bar overlapping version tags) and fragmented state routing.
**Remediation**:
- **Consolidation**: Created `UnifiedHUDOverlay` in `OverlayManager.kt` to host all global UI elements (Header, Status Bar, Map Tools, Badges).
- **Interface Hardening**: Centralized overlay logic in `ViewerScreen` and `TrackerScreen`, removing redundant calls from `AppMapContainer`.
- **Event Routing**: Added `UiEvent.ClearTrails` to `MainUiState.kt` and `UiEventCoordinator.kt` to support unified toolset actions.
- **Complexity Reduction**: Simplified screen layouts by delegating overlay Z-stacking to a single high-level component.

## [Oct12.21] - Mapping Verification & GPS Re-Zeroing
**Issue ID**: #SIMP-OCT11.20-S1, #SIMP-OCT12.17-S1
**Status**: RESOLVED
**Description**: Implemented comprehensive unit tests for UI mapping projections and added a background GPS re-zeroing mechanism to clear thermal drift.
**Root Cause**: Mapping logic in `MainViewModel` lacked regression tests to ensure parity with engine tiers. Internal GPS chips were exhibiting accuracy decay (>30m) after prolonged high-frequency operation due to thermal drift.
**Remediation**:
- **Mapping Verification**: Created `MainViewModelMappingTest.kt` covering Dashboard, HUD, and Map projections across all 4 polling tiers. Refactored `MainViewModel` to pass temporal dependencies into projection methods.
- **GPS Re-Zeroing**: Implemented `checkGpsRezeroing()` in `HardwareSuite.kt` to trigger a silent provider restart every 6 hours if accuracy thresholds are not met.
- **Dependencies**: Integrated `mockito-kotlin` for robust unit testing of internal ViewModel logic.

... (Rest of the file preserved)
