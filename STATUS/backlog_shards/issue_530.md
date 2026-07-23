# Issue #530: Validation - Urban Multipath Stress Testing

## Status: RESOLVED
## Cycle: July.23.07

### Description
Verification of the "Accuracy Recovery" grace logic (#529) and "Stationary Anchor" refinement (#533) through stress testing in high-density urban environments (Level 4 canyons).

### Implementation (July.23.07 Refinement)
- **Accuracy-Weighted Breakout**: Modified `LocationProcessor.kt` to scale the displacement contribution to the escape score based on fix accuracy. High-uncertainty fixes (accuracy > 40m) now have significantly reduced "vote" in breaking the anchor.
- **IMU Damping**: Introduced `ANCHOR_IMU_DAMPING_FACTOR` (0.5). When the IMU confirms the device is stationary (`isPhysicallyStationary`), GPS-based breakout score accumulation is halved.
- **Accuracy Snap Integration**: Explicitly suppress breakout when `PhysicsUtils` identifies an "Accuracy Snap" (correction to blue-dot precision).

### Verification Requirements
- [x] **Location Stability**: Tracker marker remains locked to the mean position during 30-minute static tests in narrow urban streets.
- [x] **Breakout Sensitivity**: Real movement is detected within 5 meters of the anchor point once IMU confirms motion.
- [x] **Accuracy Snap Suppression**: "Visual Jump" alerts and anchor breakouts are suppressed when transitioning from low-accuracy (gray) to high-accuracy (blue) fixes.

### Field Test Notes
- [x] Test Site A: Financial District (Deep canyons). Successfully suppressed 15m multipath bursts.
- [x] Test Site B: Underpass / Covered walkway transitions. Accuracy snaps handled without jump alerts.
