# Upper Menu Line & Quick Navigation Views Specification (vNov4.34)

This document specifies the upper menu line (top control bar) across the Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications, detailing the expanded quick-action navigation buttons.

## 1. Upper Menu Line Layout & Quick Navigation Icons
The upper menu line is pinned to the very top of both Sensor and Monitor consoles, providing instant one-tap access to primary operational modes and diagnostics:
- **Setup (`Build` - Cyan)**: Opens the device Setup & Info dialog (`showSetupDialog`), displaying device IDs, relay URL, and version details.
- **Settings (`Settings` - White)**: Opens configuration overlays for custom device IDs, relay URL configuration, and maintenance options.
- **Tools (`Refresh` - Gold)**: Opens `ToolsOverlay` for high-frequency telemetry burst simulation, database pruning, test violation triggering, and watchdog resetting.
- **Map (`Place` - Green)**: Triggers map view centering, GPS fix verification, and auto-centering tracking mode.
- **Events (`Menu` - Red)**: Accesses recent system event logs, violation markers, and audit history.
- **Graphs (`Star` - Purple)**: Accesses telemetry analytics, IMU charts, and status metric visualizations.
- **Info (`Info` - Blue)**: Opens the system information and device capabilities guide (`PhoneSetupOverlay`).
- **Close / Disconnect (`Close` - Rose)**: Exits active session or locks the console.
