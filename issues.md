# Project Issues

## Resolved Issues

### Issue #013: Forensic UI Expansion - Stationary Scaling Visibility
- **Description**: Expose internal stationary scaling metrics to the UI telemetry panel to verify adaptive logic during long-term testing.
- **Root Cause**: Lack of visibility into adaptive sensor parameters (`debounceMs`, `vibrationRollingSum`) made it difficult to verify "Stationary Scaling" without log analysis.
- **Implementation**: 
    * Exposed `proximityDebounceMs` and `vibrationRollingSum` in `AppSensorManager`.
    * Propagated fields through `LocationUpdate`, `TrackerStatus`, and `LocationState`.
    * Added `Prox Debounce` and `Rolling Vibe` fields to `LegacyDashboardGrid` in `OverlayComponents.kt`.
- **Status**: Resolved
- **Priority**: Medium
- **Target**: `LegacyDashboardGrid` in `OverlayComponents.kt`.
