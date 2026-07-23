# Issue #529: Geofence Reliability - Accuracy Recovery

## Status: Resolved (July.23.04)
## Requirement: R529

### Description
Sudden transitions from low-accuracy (GPS "gray" fixes) to high-accuracy (blue fixes) often resulted in large spatial jumps that triggered false "Visual Jump" alerts and geofence violations.

### Resolution
- **Accuracy Grace Logic**: Implemented in `PhysicsUtils.isVisualJump`.
- **Constraint**: If accuracy improves significantly between two fixes, the spatial displacement is suppressed if it falls within the uncertainty radius of the *previous* fix.
- **Result**: Drastic reduction in false-positive "snaps" in high-multipath urban areas.

### Verification
- [x] Verified via urban stress test (financial district).
- [x] Confirmed that real high-velocity movement still breaks the grace window.
