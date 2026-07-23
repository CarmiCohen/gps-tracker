# Issue #533: Stationary Anchor Refinement

## Status: Resolved (July.23.04)
## Requirement: R990c

### Description
GPS "spaghetti" trails and micro-drifts in high-density urban canyons cause false movement triggers even when the device is physically stationary.

### Resolution
- **Sliding Window Buffer**: Implemented `anchorAveragingBuffer` (8-point window) in `LocationProcessor.kt`.
- **Convergence Logic**: The stationary anchor now converges to the weighted mean of fixes rather than snapping to the latest raw coordinate.
- **Breakout Scoring**: Integrated displacement trends and velocity weights into the breakout threshold to maintain responsiveness to real movement.

### Verification
- [x] Verified via Level 4 Urban Canyon field test (Static).
- [x] Displacement drift reduced by ~70% in high-multipath environments.
