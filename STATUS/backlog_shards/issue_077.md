# Issue #077: Type Safety Authority (GPS Index)

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Math

---

## 📝 Description
The GPS index calculation in `TelemetryUtils` was using integer math for certain satellite weights, leading to "stair-stepping" in the quality indicators. This required a move to full double-precision implicit promotion.

## 🛠️ Resolution
- Refactored `calculateGpsIndex` to use `Double` for all internal accumulation.
- Simplified `satsIndex` math to leverage implicit promotion.
- Verified that quality indicators now show smooth transitions in high-multipath environments.

## 🔗 References
- **Requirement**: R999 (Type Safety Authority)
- **File**: `core/engine/src/main/java/com/gps19/core/engine/TelemetryUtils.kt`
