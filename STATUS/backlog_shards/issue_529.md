# Issue #529: Urban Accuracy Snap Mitigation

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Filtering

---

## 📝 Description
In high-density urban environments, GPS accuracy can fluctuate wildly. "Accuracy Snaps" (sudden improvements followed by immediate degradation) were causing false geofence violations and visual jitter on the map.

## 🛠️ Resolution
- Implemented **Grace Logic** in `PhysicsUtils`: The system now requires a sustained accuracy improvement before promoting a point as "optimized".
- Added `lastValidAccuracy` tracking in `LocationSentinel` to suppress false "Visual Jump" markers during accuracy stabilization.
- Synchronized geofence breach evaluations with a 2-point confirmation window during high-accuracy jitter.

## 🔗 References
- **Requirement**: R529 (Accuracy Recovery Mitigation)
- **Files**: `LocationSentinel.kt`, `PhysicsUtils.kt`, `AnchorEvaluator.kt`
