# Issue #326: Intelligent Uncertainty UX Mapping
**Status**: Resolved (v9.2.2)
**Requirement**: R326

## Description
When the device enters a "Location Pending" state (Bayesian uncertainty expansion), the system provides specific contextual reasons (e.g., GPS Stall vs. Environmental Gap) in the UI and logs.

## Resolution
- Added `GPS_GAP` to `LocationPendingReason` enum.
- Implemented priority-based merging in `TelemetryAggregator`.
- Propagated reasons into `ViolationReport` technical details.
- Updated `TrackerService` to detect environmental gaps.
