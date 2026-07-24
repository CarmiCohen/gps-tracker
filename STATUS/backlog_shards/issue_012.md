# Issue #012: Adaptive Proximity Debounce

## 🎯 Status: Resolved (Historical)
**Category**: Sensor Logic / Reliability

---

## 📝 Description
The proximity sensor was triggering false "NEAR" states during rapid device movement or vibration. This required an adaptive debounce mechanism that scales based on the device's behavioral state (Stationary vs. Moving).

## 🛠️ Resolution
- Implemented `PROXIMITY_DEBOUNCE_STATIONARY_MS` (5s) and `PROXIMITY_DEBOUNCE_MOVING_MS` (1s).
- Added scaling logic in `AppSensorManager` that increases debounce time the longer a device remains stationary.
- Integrated high-load scaling (2x multiplier) to prevent sensor thrashing during CPU-intensive tasks.

## 🔗 References
- **Requirement**: R729 (Behavioral Debouncing)
- **File**: `EngineConstants.kt`, `AppSensorManager.kt`
