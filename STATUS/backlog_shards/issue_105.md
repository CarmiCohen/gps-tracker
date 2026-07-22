# Issue #105: Monotonic Timeline Reconstruction

## Status: Resolved (July.20.01)
## Requirement: R105

### Description
To ensure "1Hz Ribbon Fidelity" across process boundaries (e.g., app update or service death), the system must reconstruct the monotonic timeline on startup. Using `SystemClock.elapsedRealtime()` alone resets on every reboot.

### Resolution
- **Drift Mapping**: Implemented `clock_drift_ref` in `HistoryEntity`. On startup, the system calculates the delta between the wall-clock (`ts`) and monotonic clock (`rt`) to anchor the new timeline to the historical one.
- **Continuity Logic**: The `TelemetryAggregator` uses this reference to ensure that "Gaps" are correctly calculated even if the system rebooted between pulses.

### Verification
- [x] Verified that ribbons remain continuous after a device reboot.
- [x] Confirmed "Black Gaps" are correctly rendered for the duration the device was powered off.
