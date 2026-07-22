# Issue #062: Dynamic Anchor Breakout
**Status**: Resolved
**Priority**: High
**Requirement**: R990
**Resolution Version**: July17.08

## Description
Implement a displacement-weighted monitor to prevent "sticky anchors" where the device stays locked to a stationary coordinate despite significant physical movement.

## Tasks
- [x] Implement displacement accumulation logic in `LocationProcessor`.
- [x] Define breakout thresholds based on accuracy and stationary probability.
- [x] Propagate anchor unlock state to HUD.
- [x] Validation tracked in #053.

## Implementation Details
- Introduced `ANCHOR_ESCAPE_SCORE_THRESHOLD` (100.0) and `ANCHOR_DISPLACEMENT_WEIGHT` in `EngineConstants.kt`.
- Implemented `anchorEscapeScore` accumulation in `LocationProcessor.processGpsPoint`.
- The score increments based on displacement in the transition zone (starting at 70% of distance threshold) and estimated velocity.
- Physical sensor motion (vibration) now forces an immediate breakout score.
- Trend analysis of 3 consecutive points is used to detect sustained outward movement.
- Verified that `isAnchorLocked` is correctly propagated to `MainUiState` and displayed in `LegacyDashboardGrid` and `AppMapContainer`.
