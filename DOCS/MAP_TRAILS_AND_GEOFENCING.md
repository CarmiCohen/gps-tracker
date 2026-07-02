# Map, Trails, & Geofencing Mechanism (v8.9.78)

This document describes the mapping engine, historical trail persistence, and geofence enforcement logic.

## 1. Map Engine (osmdroid)
The application uses **osmdroid** for offline-capable map rendering. 
- **Marker Pooling**: Uses a `SnapshotStateList` to manage markers efficiently within Compose (Issue #147).
- **Pruning**: Polylines and markers are pruned after 1000 points or 50 violation markers (`MARKER_POOL_PRUNE_THRESHOLD`) to maintain UI performance (Issue #440).
- **Performance Optimization (Issue #016)**: Trail rendering logic minimizes main-thread overhead by reusing `Polyline` objects and avoiding redundant O(N) count passes during pool pruning.

## 2. Trail Persistence
Historical movement is visualized as a "Blue Trail."
- **Data Source**: `TrailEntity` in SQLite.
- **Fidelity**: Supports both real-time and recovered points from offline backfill.
- **Jump Visualization**: Rejected points are shown as **Magenta Squares** (Jumps) or **Red Circles** (Out-of-Range).
- **Hindsight Correction**: Points promoted via the GtoEngine (Issue #367) are retroactively added to the trail to prevent "teleporting" visual artifacts. Promoted points strictly preserve original forensic metadata (Issue #435).

## 3. Stationary Anchor Hard-Lock (Issue #018)
To eliminate GPS drift ("spaghetti trails") in urban canyons or indoor environments, the system implements a strict behavioral lock.
- **Engagement**: When IMU confidence in a stationary state exceeds 90%, the engine clamps coordinates to a fixed `parkingAnchorPoint`.
- **Breakout Sensitivity**: The lock is released only if a spatial breakout exceeds a dynamic threshold (max of 20m or 0.8x accuracy).
- **Visual Feedback**: The map displays a prominent "ANCHOR LOCKED" badge when the lock is active. Coordinates and speed are strictly clamped to zero/anchor in this state.

## 4. Geofencing (GtoEngine)
The system enforces a safety radius around user-defined "Home Points."
- **Authoritative Gate**: `maxAccuracy` is the exclusive authority for geofence transitions. Thresholds use a **0.5x spatial gate** for deduplication and persistence sensitivity (Issue #450).
- **Dynamic Buffer**: Uses a 6-sigma buffer (`GEOFENCE_BUFFER_MULT` = 6.0) based on `maxAccuracy` to prevent false alarms from signal jitter.
- **Predictive Breach**: Calculates the time-to-exit based on current velocity. Triggers alarms 2.0s (`GEOFENCE_PREDICTIVE_LOOKAROW_S`) before the physical breach occurs.
- **Log Spatial Anchor**: Geofence violations are geographically anchored. The red marker on the map reflects where the violation was calculated (Issue #208).

## 5. Forensic Integration
- **SIT Markers**: Mechanical sitting events (SIT) are reconstructed on the map from synchronized forensic logs (Issue #459 / Formerly #336-E).
- **Ghost Mode (R338)**: Markers and trails dim (Slate500) if the telemetry is older than **15s** (`TELEMETRY_UI_STALE_THRESHOLD_MS`) (Issue #338 / Issue #428).
- **Uncertainty Context**: The UI displays a Bayesian uncertainty radius when the location is pending (e.g., during a `GPS_STALL` or `ACOUSTIC_VIOLATION`).
    - **Expansion Rate**: Conservative growth at 15.0m/s (Moving) or 1.5m/s (Stationary).
    - **Safety Cap**: Expansion is strictly capped at **33.3m/s** to maintain threshold sanity (Issue #431).
- **Role Identity**: Every trail point and violation is tagged with the source role (T/V). (Standardized per R182).
