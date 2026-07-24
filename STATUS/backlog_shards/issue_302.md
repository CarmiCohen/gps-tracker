# Issue #302: Behavioral State Thresholds

## 🎯 Status: Resolved (Historical)
**Category**: Engine / State Machine

---

## 📝 Description
Implementation of velocity and confidence thresholds for the behavioral state machine (Moving, Parking, Stationary). The system previously suffered from state "fluttering" during low-speed maneuvers.

## 🛠️ Resolution
- Established `SUSTAINED_SPEED_THRESHOLD` and `SUSTAINED_SPEED_STATIONARY_THRESHOLD`.
- Implemented a time-based confidence buffer (`STATE_CONFIDENCE_BUFFER_MS`) to prevent rapid state transitions.
- Integrated high-speed promotion logic to immediately exit power-save modes upon significant motion.

## 🔗 References
- **File**: `core/engine/src/main/java/com/gps19/core/engine/EngineConstants.kt`
