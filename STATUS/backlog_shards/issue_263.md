# Issue #263: EMA Factor Inversion Fix

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Math

---

## 📝 Description
The Exponential Moving Average (EMA) factors for environmental sensors (Lux, Acoustic, Vibration) were incorrectly inverted, causing the baseline to respond too slowly to sudden spikes and too quickly to ambient noise.

## 🛠️ Resolution
- Corrected alpha factor application in `AppSensorManager`.
- Standardized `EMA_SLOW` and `EMA_FAST` constants in `EngineConstants.kt`.
- Verified that baselines now correctly stabilize against steady noise while remaining sensitive to forensic spikes.

## 🔗 References
- **File**: `core/engine/src/main/java/com/gps19/core/engine/EngineConstants.kt`
