# Project Revolution: Code Removal & Simplification Strategy

## 1. Objective: "Shortening the Code"
The primary goal is to eliminate the "Dual-Mode" complexity where a single codebase attempts to be both a Sensor and a Monitor. By splitting them, we can delete large blocks of conditional logic and unused dependencies.

## 2. Removals from Sensor (Tracker)
To achieve a "Headless" state and minimize startup time:
- **Delete**: `MapComponents.kt`, `osmdroid` library dependencies, and all Map rendering logic.
- **Delete**: `GraphLogic.kt`, `AnalyticalUI.kt`, and complex Dashboard widgets.
- **Delete**: `MonitorScreen.kt` and all code related to viewing *other* trackers.
- **Delete**: All UI-based alarm sounds and notification overlays (Sensor is silent).
- **Result**: Significant reduction in DEX size and memory footprint.

## 3. Removals from Monitor (Viewer)
To achieve extreme battery efficiency:
- **Delete**: `GnssManager.kt`, `NmeaParser.kt`, and background location listeners.
- **Delete**: `KalmanFilter.kt` and `SpatialLogic.kt` (The Monitor consumes *already processed* data from the Sensor).
- **Delete**: `SensorHardwareDrivers.kt` (No need to interface with IMU, Lux, or Barometer).
- **Delete**: `ForegroundService` logic (The Monitor is a standard Activity-based app).
- **Result**: Elimination of background wake-locks and CPU-heavy spatial calculations.

## 4. Elimination of "Dual-Mode" Logic
In both apps, we will remove the `isTrackerMode` / `AppRole` branching found in the legacy project:
- **Legacy**: `if (isTrackerMode) { ... } else { ... }` blocks in `ConnectivitySuite`, `MainViewModel`, and `SettingsRepository`.
- **Revolution**: Separate, clean implementations for each app. No "Mode Switching" at runtime.

## 5. Dependency Stripping
- **Sensor**: Zero dependency on UI frameworks beyond what is needed for the simple Admin HUD.
- **Monitor**: Zero dependency on raw sensing or background execution libraries.
