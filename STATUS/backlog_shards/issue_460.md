# Issue #460: Bayesian Uncertainty Expansion

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Kinematics

---

## 📝 Description
Implementation of the kinematic uncertainty expansion model. During GPS gaps, the system must "grow" the estimated error radius (uncertainty bubble) using monotonic time to ensure conservative geofence evaluations.

## 🛠️ Resolution
- Implemented `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` (15.0 m/s).
- Added expansion logic in `MainAlarmLogic.kt` using `rt` (realtime) deltas.
- Established a speed cap (`33.3 m/s`) to prevent runaway expansion during extremely long gaps.

## 🔗 References
- **File**: `core/engine/src/main/java/com/gps19/core/engine/MainAlarmLogic.kt`, `EngineConstants.kt`
