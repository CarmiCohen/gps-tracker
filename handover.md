# Handover - v8.9.5

## Technical Summary: Issue #189 Fix
**Issue**: Viewer Background Location Gap.
**Affected Component**: `ViewerService.kt`

### Changes:
- **GPS Injection**: `GpsManager` is now injected and started within `ViewerService`.
- **Background Polling**: Set to `VIEWER_GPS_POLLING_MS` (10,000ms) to balance background persistence with battery life.
- **Relative Geofencing**:
    - The `LocationProcessor` now receives the Viewer's local coordinates.
    - `updateCalculatedDistances` is called with a `SpatialAnchor` constructed from the `remoteHandler`'s current tracker state.
    - `evaluateAlarms` now passes `maxOf(distToTracker, remoteHandler.trackerDistToHome ?: 0.0)` as `distToHomeAuthority`. This ensures the geofence alarm triggers if the tracker leaves its home zone OR if it drifts too far from the Viewer.
- **Lifecycle Management**: Added `gpsCollectionJob` and `gnssDetailJob` to the service's lifecycle management, ensuring they are cancelled in `onDestroy`.

### Dependencies:
- Requires `GpsManager` to be correctly provided via Hilt (already configured in `AppModule`).
- Relies on `LocationProcessor.updateCalculatedDistances` for spherical distance calculations.

### Verification Steps:
1. Start the app in Viewer mode.
2. Move the Viewer device away from the Tracker while the app is in the background.
3. Verify that "Distance to Tracker" updates on the dashboard upon return.
4. Verify that Geofence alerts trigger if the relative distance exceeds the threshold.
