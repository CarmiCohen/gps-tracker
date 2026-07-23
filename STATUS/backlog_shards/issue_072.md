# Issue #072: Map Stabilization - Temporal Smoothing

## Status: RESOLVED
## Cycle: July.23.06
## Requirement: R105, R106

### Description
The tracker marker on the viewer map exhibits "jitter" or jumps to historical/raw locations when the device clock drifts or when switching between GPS and Network providers during low-signal periods.

### Resolution Summary
- **Temporal Smoothing (EMA)**: Implemented an Exponential Moving Average filter for both tracker and viewer markers in `OsmMap`.
- **Dynamic Alpha**: Uses `POSITION_EMA_ALPHA_STATIONARY` (0.1) when stationary and `POSITION_EMA_ALPHA_DEFAULT` (0.3) when moving to balance stability and responsiveness.
- **Snap Threshold**: Added a 30m displacement threshold to ensure the marker snaps instantly to new valid locations during accuracy recovery or significant movement, preventing "rubber-banding."
- **Centering Sync**: Centering logic now uses smoothed coordinates to prevent visual vibration between the map and the marker.

### Verification Requirements
- [x] **Clock Drift Immunity**: Verified that `calculateFreshness` uses `systemPulse` which is drift-corrected against the monotonic timeline.
- [x] **Marker Smoothing**: Confirmed visual stability at zoom level 18.0+.
- [x] **Visual Continuity**: No "Gray Jumps" observed during provider transitions.

### Field Test Notes
- [x] Test in underground parking (signal loss recovery): Marker snaps correctly upon exit.
- [x] Test during device time-sync updates: Marker remains locked to the physical position.
