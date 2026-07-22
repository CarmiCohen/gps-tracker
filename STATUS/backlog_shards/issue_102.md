# Issue #102: Temporal Forensic Integrity (Monotonic rt Authority)

## Status: Resolved (July.21.00)
## Requirement: R102

### Description
The system must be immune to system clock drifts, manual user adjustments, and NITZ updates during forensic recording. Relying on Wall-Clock time (`ts`) for logic leads to "ribbon jumping" or duplicate buckets if the clock moves backward.

### Resolution
- **Monotonic Authority**: Switched `TelemetryAggregator` and `LocationProcessor` to use `rt` (SystemClock.elapsedRealtime) for all bucket calculations, gap detection, and interpolation logic.
- **Dual-Time Strategy**: `ts` (Wall-Clock) is preserved solely for UI display and forensic log timestamps, but logic flows strictly through the monotonic timeline.
- **Drift Mapping**: Implemented `clock_drift_ref` in `HistoryEntity` to allow reconstruction of the monotonic timeline across process boundaries (Issue #105).

### Verification
- [x] Verified that manual clock rollback does not affect ribbon continuity.
- [x] Verified `TelemetryAggregator.processPoint` correctly buckets using `rt`.
