# Connection & UI Connectivity Mechanisms (v8.9.10)

This document details the strategies used to maintain stable communication between the Tracker and Viewer, and how the UI reflects this state.

## 1. Connectivity Model
- **Protocol**: WebSocket (via Socket.io) for real-time telemetry.
- **Relay Role**: Acts as a signaling server and message forwarder.
- **Health Checks**:
    - **Ping/Pong**: RTT measurement to the relay.
    - **Visual Watchdog**: Dead-man timer for peer activity (`WATCH_TIMEOUT_MS` 30s).
    - **Zombie Detection**: Uses independent HTTP pulses to detect stalled TCP sockets.

## 2. UI Staleness & Visibility (v8.9.10)
The UI employs a tiered strategy for data freshness:
- **Ghost Mode (Issue 193)**: Dashboard fields, accuracy circles, and map markers enter a dimmed "Ghost" state (Slate500) when telemetry is older than **10s** (`TELEMETRY_UI_STALE_THRESHOLD_MS`).
- **Position Health (GPS)**: Fields gray out after **10s** (`GPS_UI_FAIL_THRESHOLD_MS`). **Instant Recovery (R923)**: Recovery occurs immediately upon receipt of any telemetry packet.
- **Link Health (Sensors)**: Fields gray out after 30s (`WATCH_DOG_UI_GRACE_MS`) of silence.
- **Cutoff**: All telemetry is hidden (--) after 10 minutes (`SENSOR_GRACE_PERIOD_MS`) inactivity.

## 3. Forensic Continuity
The application utilizes monotonic timing via `TimeProvider` for all connectivity metrics (Drop durations, Uptime) to ensure forensic accuracy regardless of system clock adjustments.

## 4. Role & Identity Integrity (v8.9.10)
Every telemetry update and log is tagged with the mandatory `role` field. The Viewer joins the Tracker's ID room to ensure bidirectional pulse reception. v8.9.10 includes acknowledged SIT synchronization (Issue 194), absolute power parity (Issue 192), and **Log Spatial Anchoring** (Issue 208).

## 5. Xiaomi Readiness (v8.9.10 Baseline)
Xiaomi devices require specific background, lock screen, and autostart permissions. The system verifies autostart state, reporting `ALERT_ID_XIAOMI_SYSTEM_MISSING` if restrictions are detected. Includes `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient boot alarms.
