# Issue #072: Map Stabilization - Temporal Smoothing

## Status: OPEN (Active)
## Cycle: July.23.04
## Requirement: R105, R106

### Description
The tracker marker on the viewer map exhibits "jitter" or jumps to historical/raw locations when the device clock drifts or when switching between GPS and Network providers during low-signal periods.

### Verification Requirements
- **Clock Drift Immunity**: Verify that `clock_drift_ref` correctly aligns incoming telemetry to the viewer's local timeline.
- **Marker Smoothing**: Confirm that `MapOverlayManager` ignores raw fixes if a higher-accuracy smoothed anchor is active.
- **Visual Continuity**: Ensure no "Gray Jumps" occur on the map when the tracker transitions between duty cycle phases.

### Field Test Notes
- [ ] Test in underground parking (signal loss recovery).
- [ ] Test during device time-sync updates.
