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
- **Trail Pruning**: The map receives at most the newest 1,000 trail points, to keep rendering smooth on budget hardware (A15). Separately, a background job runs about every 15 minutes and keeps the newest 50,000 stored rows of each tracker (ISSUE-NOV5.2-O14).

---

## 3. Home-Points & Geofencing
*As built in Nov8.13 (ISSUE-NOV8.13-O51, SOT rule 1.394). The geofence is defined on the Monitor and enforced by the Sensor, so the Monitor does no per-fix work (user decision 2026-11-08, battery).*
- **Edit Mode (`HOME`)**: The `HOME` button in the live map's control column opens the Home Point editor: a map tap adds a Home Point; tapping a Home Point asks `Remove Home Point N?` before removing it. The editor panel shows the point count, the radius, and a radius slider (50 to 500 m, steps of 10 m, default 100 m), stored once the slider is released. `DONE` or `HOME` closes it.
- **Home Points Rendering**: Each Home Point is a green square with a green radius circle (`Polygon.pointsAsCircle`); taps inside a circle still reach the map.
- **Home Points Persistence**: `GeofenceConfig` (points, radius, revision) in the Monitor DataStore; each edit stamps a newer revision.
- **Delivery To The Sensor**: the Monitor sends the geofence through the relay's `settings_update` / `settings_relay` event on each edit and each time the tracker comes online (see `04_COMMUNICATION_PROTOCOL.md`). The Sensor keeps the newest revision and enforces it also while offline and after a restart.
- **Enforcement (Sensor)**: `SentinelLogic.evaluateHomeZone` on each fix: home within the radius of any point; outside only when the fix's GPS error circle is beyond every radius; no breach on the first fix after a start or a change; `GEOFENCE_BREACH` on leaving home.
- **Not built**: a `FENCE` toggle to hide the circles. The `ADD`/`DEL`/`FENCE` tools of the unused `MapComponents.kt` stack were never on screen (SIMP-NOV5.0-S1).

---

## 4. Violation Markers & Spatial Alerts
- **Jump Alert Markers (`JUMP`)**: Renders distinct purple/pink markers on the map at coordinates where kinematic velocity anomalies or GPS position jumps occurred.
- **Out-of-Range Markers (`OUT`)**: Renders a red dot at each stored trail row with status `GEOFENCE_BREACH` (where the tracker left every Home Point), with the time as its info text (Nov8.13). The `OUT` visibility toggle below belongs to the unused stack and is not on the live map.
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
- *Nov8.13 note: the `MapToolsOverlay` panel above is not called by the running Monitor (SIMP-NOV5.0-S1). The live map's control column holds `IN`, `OUT`, `SENSOR`, `LOCAL`, and `HOME` (the Home Point editor, section 3); the Sensor has no map, so R872 holds. osmdroid's own zoom buttons are hidden.*
