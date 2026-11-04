# System Tools & Diagnostics Menu (vNov4.32)

This document describes the System Tools & Diagnostics menu integration in the GPS Tracker & Sentinel system.

## 1. Menu Integration (`ToolsOverlay`)
The **Tools** option is placed directly under the top menu bar alongside the Setup (`Build`) and Settings (`Settings`) gear icons in both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications.

## 2. Available System Tools & Actions
When tapping the Tools option, the `ToolsOverlay` panel opens with the following advanced hardware and telemetry control actions:
1. **Trigger High-Frequency Burst (100Hz)**: Forces 100Hz IMU and GNSS sensor sampling to evaluate low-spec device resilience and buffer alignment.
2. **Prune Database & Reclaim Cache**: Executes maintenance database pruning to optimize local storage and remove expired log rows.
3. **Trigger Test Alarm / Violation**: Simulates test violation events (such as geofence breach or tamper detection) for alarm authority testing.
4. **Reset Service Watchdog**: Forces watchdog re-alignment and pulse heartbeat synchronization.
