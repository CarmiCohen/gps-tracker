# Issue #062: Stationary Anchor Monitor

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Filtering

---

## 📝 Description
Implementation of the core stationary anchoring logic to suppress coordinate "spaghetti" trails when the device is indoors or stationary. This task established the baseline for weighted coordinate averaging.

## 🛠️ Resolution
- Implemented `AnchorEvaluator` to handle stationary state detection.
- Established `PARKING_ANCHOR_MIN_DIST` and `ANCHOR_AVERAGING_WINDOW_SIZE` (R990).
- Integrated accuracy-weighted penalties to prevent anchor drift during multipath events.

## 🔗 References
- **Requirement**: R990 (Stationary Anchor Authority)
- **File**: `core/engine/src/main/java/com/gps19/core/engine/EngineConstants.kt`
