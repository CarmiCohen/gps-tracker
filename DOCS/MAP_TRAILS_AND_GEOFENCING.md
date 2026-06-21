# Map, Trails, & Geofencing Mechanism (v8.9.10)

This document describes the mapping engine, historical trail persistence, and geofence enforcement logic.

## 1. Map Engine (osmdroid)
The application uses **osmdroid** for offline-capable map rendering. 
- **Marker Pooling**: Uses a `SnapshotStateList` to manage markers efficiently within Compose.
- **Pruning**: Polylines and markers are pruned after 1000 points or 50 violation markers (`MARKER_POOL_PRUNE_THRESHOLD`) to maintain UI performance.

## 2. Trail Persistence
Historical movement is visualized as a "Blue Trail."
- **Data Source**: `TrailEntity` in SQLite.
- **Fidelity**: Supports both real-time and recovered points from offline backfill.
- **Jump Visualization**: Rejected points are shown as **Magenta Squares** (Jumps) or **Red Circles** (Out-of-Range).

## 3. Geofencing (GtoEngine)
The system enforces a safety radius around user-defined "Home Points."
- **Dynamic Buffer**: Uses a 6-sigma buffer based on `maxAccuracy` to prevent false alarms from signal jitter.
- **Predictive Breach**: Calculates the time-to-exit based on current velocity. Triggers alarms 2.0s before the physical breach occurs.
- **Log Spatial Anchor (v8.9.10)**: Geofence violations are geographically anchored. The red marker on the map now accurately reflects where the violation was *calculated*, even if the machine is already moving further away.

## 4. Forensic Integration
- **SIT Markers**: Mechanical sitting events are reconstructed on the map from synchronized logs.
- **Ghost Mode**: Markers and trails dim (Slate500) if the telemetry is older than 10s.
- **Role Identity**: Every trail point and violation is tagged with the source role.
