# Project Revolution: Map Functionality & Feature Specification

This document provides the definitive specification of all map capabilities, rendering primitives, user interactions, and spatial features migrated and preserved from the pre-revolution architecture.

---

## 1. Overview & Map Engine
- **Engine Architecture**: osmdroid integrated with Jetpack Compose via `AndroidView`, wrapped by `AppMapContainer` and `OsmMap`.
- **Imperative Controller**: `MapController` and `MapOverlayManager` handle high-performance marker pooling, polyline rendering, and overlay caching.
- **Compose Stability**: ArrayList pools migrated to `SnapshotStateList` (`mutableStateListOf`) for snapshot integrity and recomposition safety.

---

## 2. Trail Management
- **Dual Trail Rendering**: Independent polyline rendering for **Tracker (Sensor)** and **Viewer (Monitor)** paths.
- **Segment Optimization**: Renders polylines using primitive array segments (`MapTrailSegment`) to minimize memory allocation and prevent UI jank.
- **Clear Trails (`CLEAR`)**: Instantly purges active local trail history from memory and local database storage (`UiEvent.ClearTrails`).
- **Save Trails (Export - `SAVE`)**: Exports complete trail history to JSON format (`trails_backup_*.json`), saving `tracker_trail`, `viewer_trail`, `home_points`, timestamps, and spatial coordinates to `Documents/GPS-Tracker`.
- **Load Trails (Import - `LOAD`)**: Imports historical `.json` trail files, deserializes coordinates, and reconstructs trail overlays on the map canvas.
- **Trail Pruning**: Automatic background pruning maintains a max threshold of 2,000 trail points to ensure continuous rendering performance on budget hardware (A15).

---

## 3. Home-Points & Geofencing
- **Home Points Rendering**: Displays Home Point markers on the map canvas with custom icon overlays.
- **Add Home Point (`ADD`)**: Tapping anywhere on the map while in `ADD` mode inserts a new Home Point coordinate.
- **Remove Home Point (`DEL`)**: Tapping an existing Home Point marker in `REMOVE` mode deletes that specific Home Point.
- **Geofence Radius Circles**: Asynchronously calculates and renders metric boundary circles (`Polygon.pointsAsCircle`) around all active Home Points using background coroutine dispatching.
- **Toggle Fence Visibility (`FENCE`)**: Dedicated `FENCE` toggle button allows users to show or hide geofence radius circles without deleting the underlying points.
- **Home Points Persistence**: Serializes Home Points to `DataStore` preferences and syncs them across connected peer nodes via signaling events.

---

## 4. Violation Markers & Spatial Alerts
- **Jump Alert Markers (`JUMP`)**: Renders distinct purple/pink markers on the map at coordinates where kinematic velocity anomalies or GPS position jumps occurred.
- **Out-of-Range Markers (`OUT`)**: Renders bold red violation markers at exact spatial coordinates where geofence breakouts or boundary violations occurred.
- **Violation Visibility Toggles**:
  - `JUMP` Button: Toggles visibility of kinematic jump violation markers (`SetViolationsVisible`).
  - `OUT` Button: Toggles visibility of geofence breakout violation markers (`SetGeofenceViolationsVisible`).
- **Violation Accuracy Circles**: Renders spatial uncertainty buffer circles around violation markers reflecting the GPS accuracy at the exact moment of the breach.

---

## 5. Camera Navigation & Centering
- **Center on Sensor (`SENSOR`)**: Dedicated map button (Green icon) that immediately zooms and re-centers the map view on the remote Sensor's latest location.
- **Center on Local Device (`LOCAL`)**: Dedicated map button (Cyan icon) that re-centers the map view on the user's local Monitor location (`DEFAULT_LAT, DEFAULT_LNG`).
- **Connection-Aware Auto-Centering**: 
  - When there is **no connection** between devices, the map automatically centers around the monitor (local device).
  - When connected, the map automatically centers around the device whose GPS location is known (prioritizing the Sensor if both are known), unless overridden by the user via `SENSOR` or `LOCAL` buttons.
- **Map Lock / Follow Mode**: Automatically follows the Sensor, Local device, or Auto-fits both when enabled. Manually dragging/panning the map automatically unlocks follow mode (`SetMapLocked(false)`).
- **Zoom Controls (`IN` / `OUT`)**: Dedicated **Zoom In ("+")** and **Zoom Out ("-")** buttons for fast, single-tap zoom adjustments.

---

## 6. Stationary Anchor & Uncertainty Context
- **Stationary Anchor Locked Badge**: Displays a prominent `"ANCHOR LOCKED"` badge overlay when IMU confidence exceeds 90% and the device enters a stationary state.
- **Dynamic Uncertainty Radius**: Renders dynamic accuracy circles (`trackerCircle` & `viewerCircle`) around live positions, expanding during motion or high GPS dilution of precision (DOP).
- **Ghost Mode (Stale Telemetry)**: Automatically dims markers and trails to Slate500 if incoming telemetry is older than 35 seconds (`TELEMETRY_UI_STALE_THRESHOLD_MS`).

---

## 7. Replay & Map Utilities
- **Replay Cursor Marker**: Replay folder overlay featuring a dedicated cursor marker and radius circle for scrubbing through historical telemetry logs.
- **Scale Bar Overlay**: Metric scale bar (`ScaleBarOverlay`) aligned at the bottom corner for distance evaluation.

---

## 8. Map Settings Wheel & Control Overlay
- **Map Settings Toggle (`MapSettingsToggle`)**: Floating purple gear button on the top-right corner that toggles the Map Tools Overlay panel.
- **Map Tools Overlay Panel (`MapToolsOverlay`)**:
  - `IN` / `OUT`: Zoom controls.
  - `VIEWER` / `TRACKER`: Quick centering.
  - `OUT` / `JUMP`: Violation marker visibility toggles.
  - `LOAD` / `SAVE`: Trail import and export actions.
  - `ADD` / `DEL`: Geofence editing tools (Viewer mode only).
  - `FENCE`: Geofence circle visibility toggle.
  - `CLEAR`: One-tap trail purge and tracker re-centering.
- **Absolute Stealth Rule (R872)**: Interactive geofence modification controls (`ADD` / `DEL`) are strictly hidden when running in Tracker mode to prevent unintended interactions or operational exposure.
