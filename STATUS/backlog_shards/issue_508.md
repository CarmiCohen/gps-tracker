# Issue #508: Optimization Removal (SNR Scaling)

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Cleanup

---

## 📝 Description
During high-frequency telemetry trials, it was determined that adaptive jump thresholds and SNR-based uncertainty scaling were introducing non-deterministic jitter on budget hardware. These optimizations were removed to favor a more predictable baseline.

## 🛠️ Resolution
- Decommissioned `Adaptive Jump` logic in `PhysicsUtils.kt`.
- Removed SNR-based scaling factors from the kinematic bubble expansion.
- Reverted to static weighted gates for jump detection to ensure cross-device consistency.

## 🔗 References
- **File**: `core/engine/src/main/java/com/gps19/core/engine/PhysicsUtils.kt`
