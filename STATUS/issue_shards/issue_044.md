# Issue #044: HUD: LEDs contradiction
**Status**: Resolved
**Priority**: High
**Requirement**: R991

## Description
Tracker: all green but VWR. Viewer: all green but GPS. Standardize top-level badges to local health.

## Resolution
Standardized top-level HUD status badges (INT, SRV, VWR/TRK, GPS) to reflect local device health. Decoupled remote telemetry coloring (Speed, State) to remain dependent on peer GPS signal. Implemented R991.
