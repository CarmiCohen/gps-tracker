# Map, Trails, & Geofencing Mechanism (v8.9.37)

This document describes the mapping engine, historical trail persistence, and geofence enforcement logic.

## 1. Map Engine (osmdroid)
The application uses **osmdroid** for offline-capable map rendering. 
- **Marker Pooling**: Uses a `SnapshotStateList` to manage markers efficiently within Compose (Issue #147).
- **Pruning**: Polylines and markers are pruned after 1000 points or 50 violation markers (`MARKER_POOL_PRUNE_THRESHOLD`) to maintain UI performance.

## 2. Trail Persistence
Historical movement is visualized as a "Blue Trail."
- **Data Source**: `TrailEntity` in SQLite.
- **Fidelity**: Supports both real-time and recovered points from offline backfill.
- **Jump Visualization**: Rejected points are shown as **Magenta Squares** (Jumps) or **Red Circles** (Out-of-Range).
- **Hindsight Correction**: Points promoted via the GtoEngine (Issue #285) are retroactively added to the trail to prevent "teleporting" visual artifacts.

## 3. Geofencing (GtoEngine)
The system enforces a safety radius around user-defined "Home Points."
- **Dynamic Buffer**: Uses a 6-sigma buffer (`GEOFENCE_BUFFER_MULT` = 6.0) based on `maxAccuracy` to prevent false alarms from signal jitter.
- **Predictive Breach**: Calculates the time-to-exit based on current velocity. Triggers alarms 2.0s (`GEOFENCE_PREDICTIVE_LOOKAHEAD_S`) before the physical breach occurs.
- **Log Spatial Anchor**: Geofence violations are geographically anchored. The red marker on the map reflects where the violation was calculated (Issue #208).

## 4. Forensic Integration
- **SIT Markers**: Mechanical sitting events (SIT) are reconstructed on the map from synchronized forensic logs (Issue #282).
- **Ghost Mode**: Markers and trails dim (Slate500) if the telemetry is older than 10s (`TELEMETRY_UI_STALE_THRESHOLD_MS`) (Issue #193).
- **Uncertainty Context**: The UI displays a Bayesian uncertainty radius when the location is pending (e.g., during a `GPS_STALL` or `ACOUSTIC_VIOLATION`), with the radius expanding at a conservative rate (`PENDING_UNCERTAINTY_GROWTH_RATE_MPS`) (Issue #221).
- **Role Identity**: Every trail point and violation is tagged with the source role (Ttk/Cohen).
