# Telemetry & Status Card (HUD) Mechanism (v8.9.42)

This document describes the telemetry data collection, state management, and the high-fidelity UI components used to visualize the tracker's real-time status via the Status Card HUD.

## 1. Telemetry Data Engine
The telemetry system collects data from multiple hardware and network sources:
- **Location Data**: Latitude, longitude, speed, bearing, and accuracy. Forensic history includes raw **speed** and **bearing** for every telemetry packet.
- **Battery & Power**: Real-time battery percentage (`BATTERY_ALARM_THRESHOLD` 20% / `CRITICAL_BATTERY_THRESHOLD` 20%), temperature, and charging state. Includes **Steep Discharge** profiling (Issue #353) and absolute parity for **battery current** (`currentMa`) (Issue #337).
- **Network Health**: RTT (Round Trip Time) and relay server connectivity status.
- **Sats Stats**: Satellites in view and used for both devices.
- **Forensic GNSS**: Raw Signal-to-Noise Ratio (SNR) for every satellite in view (`snrIdx`), including constellation metadata.
- **State Synchronization**: The `isJammer` telemetry field is strictly synchronized with the system's confirmed violation state (180s cumulative threshold).
- **Proximity Stability**: The `proximityCm` telemetry field uses debounced values (`debouncedProximityCm`).
- **Role Identity**: Every telemetry packet includes the mandatory `role` field.
- **Log Spatial Anchor**: All events captured by the telemetry engine are geographically anchored, allowing the Map to display exactly where a specific telemetry violation occurred (Issue #208).

## 2. State Management & Flow Isolation
The `MainViewModel` (decoupled into UseCases) acts as the central hub for telemetry state. To optimize performance, state is managed across multiple streams:

### A. Domain State
UseCases like `TelemetryUseCase` and `BehaviorUseCase` manage the raw telemetry and behavioral flags, exposing them as reactive flows.

### B. Isolated High-Frequency Flows
Jittery or high-frequency fields are exposed as independent `StateFlow` objects to minimize recomposition:
- `rtt`: Round-trip time to the relay server.
- `remoteSignal`: Signal quality of the remote tracker.
- `currentMa`: Real-time current consumption.
- `gpsIndexData`: Real-time GPS health metrics.
- `gnssDetail`: Detailed per-satellite SNR array.
- `systemPulse`: A 1Hz heartbeat for UI timers and data aging visuals.

## 3. UI Hierarchy & Layout
To ensure optimal navigation ergonomics, the vertical stacking order in portrait mode is:
1.  **Header Bar**: Pinned navigation controls at the absolute top.
2.  **Status Card (HUD)**: Positioned immediately below the navigation row.
3.  **Primary Content**: Map (Background) or Dashboard (Foreground).

## 4. The Status Card HUD (`StatusBar`)
The "Status Card" is the primary interface for telemetry.
- **Role Identity**: Tracker-related data is rendered in **BrandJd** (#367C2B). Viewer-related data is rendered in **ViewerOrange**. (R865/R866)
- **Visual Heartbeat**: A circular progress indicator that shrinks as data ages (10s for Position / 30s for Link).
- **Ghost Mode UX**: HUD metrics, accuracy circles, and dashboard fields enter a dimmed "Ghost" state (Slate500) when telemetry is older than 10s (Issue #338).
- **Instant Recovery (R923)**: Freshness logic utilizes the maximum of the position timestamp and telemetry arrival timestamp (`telemetryTs`).
- **Thermal Monitoring**: Temperature in °C with color-coding for high heat (> 46.0°C).
- **Interactive GNSS Label**: Tapping the "Sats" area launches the **GNSS Detail Overlay**.

## 5. Header Bar Navigation
The `HeaderBar` provides immediate access to analytical tools and forensic data:
- **Pinned Controls**: The **Log** and **Map** buttons are pinned adjacent to each other.
- **Forensic Verification**: The build version (v8.9.42) is displayed directly in the HUD for instant build verification.
