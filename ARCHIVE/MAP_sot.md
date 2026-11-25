# MAP & Settings Wheel SOT Requirements

## 🗺️ Map Engine & UI (osmdroid + Compose)

### 1. Camera & Navigation
*   **Reactive Camera Actions (R1.61/Issue #1390)**: Imperative camera animations (zoom, centering) MUST be delivered via `SharedFlow<CameraAction>`.
*   **Map Follow Logic**: When `isMapLocked` is true, camera should follow TRACKER, VIEWER, or AUTO (both in view) based on `mapFollowMode`.

### 2. Marker & Trail Management
*   **Marker Pooling (Issue #147)**: Use `SnapshotStateList` for efficient management within Compose.
*   **Pruning Thresholds (Issue #440)**: Prune polylines/markers after 1000 points or 50 violation markers.
*   **Performance Optimization (Issue #016)**: Reuse `Polyline` objects and avoid redundant O(N) passes.
*   **Ghost Mode (Issue #338)**: Markers and trails MUST dim (Slate500) if telemetry is older than 35s (`TELEMETRY_UI_STALE_THRESHOLD_MS`).

### 3. Stationary & Geofencing
*   **Stationary Anchor (Issue #018)**: Clamp to `parkingAnchorPoint` if IMU confidence > 90%. Release if breakout > 20m or 0.8x accuracy. Prominent "ANCHOR LOCKED" badge required.
*   **Geofencing Gate**: Use 0.5x spatial gate for transitions.
*   **Predictive Breach**: Trigger alarms 2.0s before breach based on velocity.
*   **Log Spatial Anchor (Issue #208)**: Red markers MUST reflect where the violation was calculated.

### 4. Layout & Uncertainty
*   **Scale-Aware Ribbons (R1.62/Issue #1384)**: Use reserved bottom-padding and orientation-aware height metrics to prevent truncation.
*   **Uncertainty Context (Issue #431)**: Grow uncertainty radius at 15.0m/s (Moving) or 1.5m/s (Stationary), capped at 33.3m/s.

## ⚙️ Setting Wheel & Options

### 1. Map Settings Toggle (Wheel)
*   **Visibility**: Toggle button (purple gear icon) to show/hide Map Tools Overlay.
*   **Layout**: Top-right placement, padding adjusted for orientation (12dp landscape / 110dp portrait).

### 2. Settings Persistence & Validation
*   **Unified Back = Commit (R800)**: Exiting settings MUST trigger `DataStore` commit and service re-init if routing changed.
*   **Identity Authority (R974-977)**: IDs must be unique and alphanumeric. Auto-sanitize and alert user if malformed.
*   **Storage Authority (R1.68)**: Use `AppRole` enum for namespaced operations in `SettingsRepository`.
