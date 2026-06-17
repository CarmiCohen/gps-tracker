# Connection & UI Connectivity Mechanisms (v8.8.35)

This document details the strategies used to maintain stable communication between the Tracker and Viewer, and how the UI reflects this state.

## 1. Connectivity Model
- **Protocol**: WebSocket (via Socket.io) for real-time telemetry.
- **Relay Role**: Acts as a signaling server and message forwarder.
- **Health Checks**:
    - **Ping/Pong**: RTT measurement to the relay.
    - **Visual Watchdog**: Dead-man timer for peer activity (`WATCH_TIMEOUT_MS` 30s).
    - **Zombie Detection**: Uses independent HTTP pulses to detect stalled TCP sockets.

## 2. UI Staleness & Visibility (v8.8.35)
The UI employs a tiered strategy for data freshness:
- **Position Health (GPS)**: Fields gray out after 7s (`GPS_UI_FAIL_THRESHOLD_MS`) of silence. **Instant Recovery (R923)**: Recovery occurs immediately upon receipt of any telemetry packet by utilizing the maximum of position and telemetry timestamps.
- **Link Health (Sensors)**: Fields gray out after 30s (`WATCH_DOG_UI_GRACE_MS`) of silence.
- **Cutoff**: All telemetry is hidden (--) after 10 minutes (`SENSOR_GRACE_PERIOD_MS`) of inactivity.

## 3. Forensic Continuity
The application utilizes monotonic timing via `TimeProvider` for all connectivity metrics (Drop durations, Uptime) to ensure forensic accuracy regardless of system clock adjustments.

## 4. Role & Identity Integrity (v8.8.35)
Every telemetry update and log is tagged with the mandatory `role` field. The Viewer correctly joins the Tracker's ID room to ensure bidirectional pulse reception. Legacy version tags (`ver`, `vid`) have been removed from the data models to simplify architecture.

## 5. Xiaomi Readiness (v8.8.35 Baseline)
Xiaomi devices require specific background, lock screen, and autostart permissions. The system verifies autostart state, reporting `ALERT_ID_XIAOMI_SYSTEM_MISSING` if restrictions are detected, with support for manual overrides via "Phone Setup" guidance.
