# Issue #062: Dynamic Anchor Breakout
**Status**: Open
**Priority**: High
**Requirement**: R990

## Description
Implement a displacement-weighted monitor to prevent "sticky anchors" where the device stays locked to a stationary coordinate despite significant physical movement.

## Tasks
- Implement displacement accumulation logic in `LocationProcessor`.
- Define breakout thresholds based on accuracy and stationary probability.
- Propagate anchor unlock state to HUD.
- Validation tracked in #053.
