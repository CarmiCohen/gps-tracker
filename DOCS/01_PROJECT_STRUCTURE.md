# Project Revolution: Folder & Module Architecture

## 1. Project Root
The new project will be located at `C:/CCwork/Android Projects/gps-revolution/` (or a similar name). It follows a strictly modularized Gradle structure to ensure separation of concerns and build performance.

## 2. Module Definitions

### A. `:app-sensor` (The Tracker)
- **Type**: Android Application (`com.android.application`)
- **Focus**: Background sensing engine and minimal Admin UI.
- **Key Files**:
    - `SensorService.kt`: The main foreground service orchestrating GPS, hardware, and telemetry.
    - `AdminActivity.kt`: Minimalist Compose-based HUD for configuration and diagnostics.
    - `SensorApp.kt`: Application class optimized for background stability.
- **Dependencies**: `:core-shared`, `:core-network`.

### B. `:app-monitor` (The Viewer)
- **Type**: Android Application (`com.android.application`)
- **Focus**: High-fidelity UI, Maps, and User Notifications.
- **Key Files**:
    - `MonitorActivity.kt`: Entry point for the Map and Dashboard UI.
    - `AlarmOverlayActivity.kt`: High-priority UI for violation alerts.
    - `MonitorViewModel.kt`: Manages UI state and on-demand polling lifecycle.
- **Dependencies**: `:core-shared`, `:core-network`, `:core-ui`.

### C. `:core-shared` (Business Logic)
- **Type**: Kotlin Library (`java-library`)
- **Focus**: Pure business logic, math, and data models.
- **Contents**:
    - `Models.kt`: Data classes for `LocationUpdate`, `Event`, `Alarm`.
    - `KalmanFilter.kt`: Precision filtering logic.
    - `SpatialLogic.kt`: Geofencing and movement detection algorithms.
    - `ForensicSpillBuffer.kt`: Low-level diagnostic logging engine.
- **Note**: This module has zero Android dependencies to facilitate lightning-fast unit testing.

### D. `:core-network` (Communication)
- **Type**: Android Library (`com.android.library`)
- **Focus**: Unified signaling and telemetry transport.
- **Contents**:
    - `SignalingProvider.kt`: Protocol implementation for Relay communication.
    - `ConnectivityCoordinator.kt`: Manages connection lifecycles (Foreground-only for Monitor).
    - `ProtobufSerializers.kt`: Efficient data wire-formats.

### E. `:core-ui` (Design System)
- **Type**: Android Library (`com.android.library`)
- **Focus**: Shared UI components for both apps.
- **Contents**:
    - `Theme.kt`, `Color.kt`, `Typography.kt`: Brand identity.
    - `HudComponents.kt`: Reusable diagnostic widgets for both Sensor HUD and Monitor Dashboard.

## 3. Directory Visualization
```text
gps-revolution/
├── app-sensor/              # Tracker App
├── app-monitor/             # Viewer App
├── core-shared/             # Math & Models
├── core-network/            # Signaling & HTTP
├── core-ui/                 # Design System
├── build-logic/             # [NEW] Gradle Convention Plugins
└── build.gradle.kts         # Root Build Script
```
