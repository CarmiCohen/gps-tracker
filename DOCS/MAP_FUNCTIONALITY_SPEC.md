# Project Revolution: Map Functionality & Feature Specification

This document provides the definitive specification of all map capabilities, rendering primitives, user interactions, and spatial features migrated and preserved from the pre-revolution architecture.

---

## 1. Overview & Map Engine
- **Engine Architecture**: an osmdroid `MapView` inside Jetpack Compose's `AndroidView`, built by `MonitorScreen` (`MonitorActivity.kt`), which rebuilds the overlays (scale bar, tap receiver, Home Point fences and markers, trails, position markers) on each update. The pre-split `OsmMap`, `MapController` and `MapOverlayManager` were removed in Nov17.1 (SIMP-NOV17.0-S1).

---

## 2. Trail Management
- **Dual Trail Rendering**: Independent polyline rendering for **Tracker (Sensor)** and **Viewer (Monitor)** paths.
- **Not built**: `CLEAR`, `SAVE` (JSON export) and `LOAD` (JSON import) of trails existed only in the unused pre-split tools panel, removed in Nov17.1 (SIMP-NOV17.0-S1).
- **Trail Pruning**: The map receives at most the newest 1,000 trail points, to keep rendering smooth on budget hardware (A15). Separately, a background job runs about every 15 minutes and keeps the newest 50,000 stored rows of each tracker (ISSUE-NOV5.2-O14).

---

## 3. Home-Points & Geofencing
*As built in Nov8.13 (ISSUE-NOV8.13-O51, SOT rule 1.394). The geofence is defined on the Monitor and enforced by the Sensor, so the Monitor does no per-fix work (user decision 2026-11-08, battery).*
- **Edit Mode (`HOME`)**: The `HOME` button in the live map's control column opens the Home Point editor: a map tap adds a Home Point; tapping a Home Point, or the red X on its row in the editor's point list (number and coordinates, left to right even in a right-to-left locale; ISSUE-NOV10.8-O85), asks `Remove Home Point N?` before removing it. The editor panel shows the point count, the radius, the point list (about three rows high, scrolling beyond), and a radius slider (50 to 500 m, steps of 10 m, default 100 m), stored once the slider is released. `DONE` or `HOME` closes it.
- **Home Points Rendering**: Each Home Point is a green square with a green radius circle (`Polygon.pointsAsCircle`); taps inside a circle still reach the map.
- **Home Points Persistence**: `GeofenceConfig` (points, radius, revision) in the Monitor DataStore; each edit stamps a newer revision.
- **Delivery To The Sensor**: the Monitor sends the geofence through the relay's `settings_update` / `settings_relay` event on each edit and each time the tracker comes online (see `04_COMMUNICATION_PROTOCOL.md`). The Sensor keeps the newest revision and enforces it also while offline and after a restart.
- **Enforcement (Sensor)**: `SentinelLogic.evaluateHomeZone` on each fix: home within the radius of any point; outside only when the fix's GPS error circle is beyond every radius; no breach on the first fix after a start or a change; `GEOFENCE_BREACH` on leaving home.
- **Not built**: a `FENCE` toggle to hide the circles. The `ADD`/`DEL`/`FENCE` tools of the pre-split stack were never on screen; that stack was removed in Nov17.1 (SIMP-NOV17.0-S1).

---

## 4. Violation Markers & Spatial Alerts
- **Out-of-Range Markers (`OUT`)**: Renders a red dot at each stored trail row with status `GEOFENCE_BREACH` (where the tracker left every Home Point), with the time as its info text (Nov8.13).
- **Not built**: jump markers, the `JUMP`/`OUT` visibility toggles and violation accuracy circles existed only in the pre-split stack, removed in Nov17.1 (SIMP-NOV17.0-S1).

---

## 5. Camera Navigation & Centering
- **Center on Sensor (`SENSOR`)**: Dedicated map button (Green icon) that immediately zooms and re-centers the map view on the remote Sensor's latest location.
- **Center on Local Device (`LOCAL`)**: Dedicated map button (Cyan icon) that re-centers the map view on the user's local Monitor location (`DEFAULT_LAT, DEFAULT_LNG`).
- **Connection-Aware Auto-Centering**: 
  - When there is **no connection** between devices, the map automatically centers around the monitor (local device).
  - When connected, the map automatically centers around the device whose GPS location is known (prioritizing the Sensor if both are known), unless overridden by the user via `SENSOR` or `LOCAL` buttons.
- **Follow Mode**: `MapFollowMode` (`MapCenterResolver.kt`): `SENSOR` and `LOCAL` lock the center to that device; `AUTO` applies the connection-aware rule above.
- **Zoom Controls (`IN` / `OUT`)**: Dedicated **Zoom In ("+")** and **Zoom Out ("-")** buttons for fast, single-tap zoom adjustments.

---

## 6. Stationary Anchor & Uncertainty Context
- **Not built**: the `ANCHOR LOCKED` badge, live accuracy circles and the dimmed "ghost" markers for stale telemetry existed only in the pre-split stack, removed in Nov17.1 (SIMP-NOV17.0-S1).

---

## 7. Replay & Map Utilities
- **Scale Bar Overlay**: Metric scale bar (`ScaleBarOverlay`) at the bottom right; the version name at the bottom left (Nov17.0, SOT 1.440).
- **Not built**: the replay cursor marker (pre-split stack, removed in Nov17.1).

---

## 8. Map Control Column
- **Live controls**: the map's control column holds `IN`, `OUT`, `SENSOR`, `LOCAL`, and `HOME` (the Home Point editor, section 3); osmdroid's own zoom buttons are hidden.
- **Absolute Stealth Rule (R872)**: geofence editing is never offered on the Sensor; the Sensor has no map, so R872 holds.
- *The pre-split gear toggle and `MapToolsOverlay` panel (`VIEWER`/`TRACKER`, `OUT`/`JUMP`, `LOAD`/`SAVE`, `ADD`/`DEL`, `FENCE`, `CLEAR`) were never called by the running Monitor and were removed in Nov17.1 (SIMP-NOV17.0-S1, SIMP-NOV5.0-S1).*
