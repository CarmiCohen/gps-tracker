# Map, Trails, & Geofencing Mechanism (v8.8.35)

This document describes the mapping engine, historical trail persistence, and the geofencing logic used for theft detection.

## 1. Map Engine & Visuals
- **Provider**: OpenStreetMap (osmdroid).
- **Trail Persistence**: Historical paths are stored in the database. Every point is tagged with role.
- **Visual Pen Lift**: Encountering a Tier 1 or Tier 2 Jump Point immediately breaks the trail line on the map.
- **Marker Pruning**: Visual markers are pruned when the pool exceeds 50 (`MARKER_POOL_PRUNE_THRESHOLD`) to maintain performance.

## 2. Geofence Logic
- **Origin**: The system uses "Home Points" as geofence centers.
- **Dynamic Gate**: `radius + (accuracy * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)`.
- **Hysteresis**: A 5.0m buffer (`GEOFENCE_HYSTERESIS_METERS`) prevents siren flickering at the boundary.
- **Accuracy Recovery**: Uses a 4-bucket sliding window. Spikes expire gradually, enabling faster recovery after interference.

## 3. Jump Validation & Rejection
- **Security Hold**: Tier 1 and 2 jumps trigger a 180s delay before geofence violations are permitted.
- **Trajectory Promotion**: Consistent movement (> 2.0 m/s for > 30s) bypasses the Jump Hold for immediate alerting.
- **Moving Hold**: A 60s anti-flapping hold is applied when transitioning from stationary to moving states.

## 4. Predictive Geofence
- **Look-ahead**: Projects the position 2.0s into the future (`GEOFENCE_PREDICTIVE_LOOKAHEAD_S`).
- **Trigger**: Alarms if the projected point is outside the fence, provided the current trajectory is stable.
- **Min Speed**: Predictive markers are only generated if speed > 1.0 m/s.

## 5. Implementation Status
In v8.8.35, all geofence and jump rejection logic is strictly isolated within the `:core:engine` module. All forensic markers and timing calculations (Issue 125) utilize monotonic time (`elapsedRealtime`) to ensure security audit trail integrity regardless of system clock changes. Legacy `ver` and `vid` tags have been removed in favor of a simplified forensic model.
