# Project Revolution: Bootstrap Guide

This guide outlines the steps to initialize the new multi-module project at `C:/CCwork/Android Projects/gps-revolution/`.

## Step 1: Directory Initialization
Create the root folder and the initial module skeleton:
```bash
mkdir "gps-revolution"
cd "gps-revolution"
mkdir "app-sensor" "app-monitor" "core-shared" "core-network" "core-ui" "build-logic" "DOCS"
```

## Step 2: Root Build Configuration
Initialize the `settings.gradle.kts` file at the root:
```kotlin
rootProject.name = "gps-revolution"

include(":app-sensor")
include(":app-monitor")
include(":core-shared")
include(":core-network")
include(":core-ui")
```

## Step 3: Module Bootstrap Strategy
To ensure the 200ms startup goal, use the following template for the `build.gradle.kts` files:

### `:app-sensor` (The Tracker)
- Apply `com.android.application`.
- Keep dependencies lean. Avoid Map or Graphing libraries.
- Optimize for `minSdk` and background execution.

### `:app-monitor` (The Viewer)
- Apply `com.android.application`.
- Include `osmdroid` or chosen Map provider.
- Implement Lifecycle-aware networking (Foreground only).

### `:core-shared`
- Apply `java-library` and `kotlin`.
- **Zero Android dependencies**. This ensures the Kalman filter and math logic can be tested in milliseconds without an emulator.

## Step 4: Documentation Migration
Copy the contents of `REVOLUTION_WORKSPACE` into the new `gps-revolution/DOCS/` directory to serve as the permanent technical specification for the new project.

## Step 5: Iterative Extraction
Do not copy the legacy `ConnectivitySuite.kt` directly. Instead:
1. Extract the `RealtimeStatus` Protobuf definition to `:core-network`.
2. Extract the `KalmanFilter` math to `:core-shared`.
3. Implement the `SensorService` first in `:app-sensor`.
4. Implement the `MonitorActivity` in `:app-monitor` to poll the Sensor.
