# Telemetry & Status Card (HUD) Mechanism (v8.8.35)

This document describes the telemetry data collection, state management, and the high-fidelity UI components used to visualize the tracker's real-time status via the Status Card HUD.

## 1. Telemetry Data Engine
The telemetry system collects data from multiple hardware and network sources:
- **Location Data**: Latitude, longitude, speed, bearing, and accuracy. Forensic history includes raw **speed** and **bearing** for every telemetry packet.
- **Battery & Power**: Real-time battery percentage (`BATTERY_ALARM_THRESHOLD` 99% / `CRITICAL_BATTERY_THRESHOLD` 20%), temperature, and charging state. Includes **Steep Discharge** profiling.
- **Network Health**: RTT (Round Trip Time) and relay server connectivity status.
- **Sats Stats**: Satellites in view and used for both devices.
- **Forensic GNSS**: Raw Signal-to-Noise Ratio (SNR) for every satellite in view (`snrIdx`), including constellation metadata.
- **State Synchronization**: The `isJammer` telemetry field is strictly synchronized with the system's confirmed violation state (180s cumulative threshold) to ensure the remote UI consistently reflex confirmed violations without transient jitter.
- **Proximity Stability**: The `proximityCm` telemetry field uses debounced values (`debouncedProximityCm`) to prevent visual flickering on the HUD, particularly on devices with virtual proximity sensors (Samsung A15).
- **Role Identity**: Every telemetry packet includes the mandatory `role` field.

## 2. State Management & Flow Isolation
The `MainViewModel` (decoupled into UseCases in v8.8.28) acts as the central hub for telemetry state. To optimize performance, state is managed across multiple streams:

### A. Domain State
UseCases like `TelemetryUseCase` and `BehaviorUseCase` manage the raw telemetry and behavioral flags, exposing them as reactive flows.

### B. Isolated High-Frequency Flows
Jittery or high-frequency fields are exposed as independent `StateFlow` objects to minimize recomposition:
- `rtt`: Round-trip time to the relay server.
- `remoteSignal`: Signal quality of the remote tracker.
- `currentMa`: Real-time current consumption.
- `gpsIndexData`: Real-time GPS health metrics.
- `gnssDetail`: Detailed per-satellite SNR array.
- `systemPulse`: A 1Hz (`TICK_INTERVAL_MS`) heartbeat for UI timers and data aging visuals.

## 3. UI Hierarchy & Layout
To ensure optimal navigation ergonomics, the vertical stacking order in portrait mode is:
1.  **Header Bar**: Pinned navigation controls at the absolute top.
2.  **Status Card (HUD)**: Positioned immediately below the navigation row.
3.  **Primary Content**: Map (Background) or Dashboard (Foreground).

## 4. The Status Card HUD (`StatusBar`)
The "Status Card" is the primary interface for telemetry.
- **Role Identity**: Tracker-related data (TRK badge, speed, accuracy, temp) is rendered in **Lime500**. Viewer-related data is rendered in **ViewerOrange**.
- **Visual Heartbeat**: A circular progress indicator that shrinks as data ages (7s `GPS_UI_FAIL_THRESHOLD_MS` / 30s `WATCH_DOG_UI_GRACE_MS` for Link), turning red when a disconnect is imminent.
- **Instant Recovery (R923)**: Freshness logic utilizes the maximum of the position timestamp and telemetry arrival timestamp (`telemetryTs`). This ensures the UI "lights up" immediately upon sync, even before a fresh GPS fix is obtained.
- **Thermal Monitoring**: Temperature in °C with color-coding for high heat (> 46.0°C `MAX_SAFE_TEMPERATURE_CELSIUS`).
- **Interactive GNSS Label**: Tapping the "Sats" area launches the **GNSS Detail Overlay**, showing a real-time bar chart of signal strength.

## 5. Header Bar Navigation
The `HeaderBar` provides immediate access to analytical tools and forensic data:
- **Pinned Controls**: The **Log** and **Map** buttons are pinned adjacent to each other.
- **Forensic Verification**: The build version (v8.8.35) is displayed directly in the HUD for instant build verification.
