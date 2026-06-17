# Viewer Info Page Layout Documentation (v8.8.35)

This document describes the modern "Telemetry-First" HUD and Dashboard layout implemented under the high-assurance architecture and refined in v8.8.35.

## 1. Unified Navigation
- **No In-App Back Button**: To maximize telemetry space, navigation is handled exclusively via the system Back button or gesture.
- **Pinned P2P Controls**: The HeaderBar pins the **Event Log** and **Map** buttons adjacent to each other at the end of the bar. This provides a "Forensic Shortcut" allowing the user to toggle instantly between reading a security event and seeing its location.
- **Analytical Access**: Rapid access to analytical Ribbons (Bar Chart) and the diagnostic Dashboard (Info).
- **Forensic Identification**: The build version (v8.8.35) is displayed between Ribbons and Log buttons for build verification.

## 2. Global Status Bar (HUD)
The HUD provides high-density real-time data for both the Tracker (TRK) and Viewer (VIEW).

### A. Connectivity Badges
- `INT`: Local Internet health (`INTERNET_LOSS_THRESHOLD_MS` 60s).
- `SRV`: Relay Server connection status.
- `TRK`: Peer-to-peer connection with the tracker (`TRACKER_SIGNAL_LOSS_THRESHOLD_MS` 180s).
- `DAT`: Real-time data flow integrity.
- `GPS`: Active GPS fix status.
- `ALM`: (Pulsing Red) Indicates active unresolved alarms.

### B. Device Metrics
- **Speed**: Current tracker speed in km/h.
- **Accuracy**: Current horizontal accuracy vs. session peak.
- **GPS Age**: Seconds since the last valid coordinate update (7s `GPS_UI_FAIL_THRESHOLD_MS` gray-out).
- **Satellites**: Number of satellites used vs. those in view.
- **Battery/Temp**: Live power level (`CRITICAL_BATTERY_THRESHOLD` 20%) and thermal state (`MAX_SAFE_TEMPERATURE_CELSIUS` 46.0°C).

## 3. Dashboard Content (Diagnostic Grid)
Accessible by tapping the HUD or the "Info" icon in the HeaderBar.

### Diagnostic & Session Statistics
- **Max Drop**: The longest period of disconnection in the current session.
- **Watchdog**: Live countdown to the next expected heartbeat (`SYSTEM_WATCHDOG_INTERVAL_MS` 90s).
- **Bruto/Uptime**: Total application life-cycle timers using monotonic time.
- **Since Conn/Disco**: Timers tracking the state of the current peer connection.

### Analytical Scores
- **GPS-Index**: Overall reliability score (0.0 - 1.0).
- **Age Index**: Penalty-based score for stale data.
- **Acc Index**: Precision-based score for coordinate quality.

---
*Note: This layout reflects the v8.8.35 architecture, focusing on forensic continuity and the simplified forensic model.*
