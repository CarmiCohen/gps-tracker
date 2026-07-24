# Issue #221: Bayesian Uncertainty Scaling

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Kinematics

---

## 📝 Description
Implementation of monotonic time-based scaling for location uncertainty. The system uses `systemPulseRt` to grow the uncertainty bubble during GPS gaps, ensuring that geofence evaluations remain conservative when fresh data is unavailable.

## 🛠️ Resolution
- Integrated `systemPulseRt` into the kinematic prediction model.
- Established expansion rates for stationary vs. moving states (R460).
- Standardized geofence breach logic to account for expanded error margins.

## 🔗 References
- **Requirement**: R460 (Bayesian Uncertainty Authority)
- **Files**: `TrackerScreen.kt`, `ViewerScreen.kt`, `PhysicsUtils.kt`
