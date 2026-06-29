# Connection & UI Connectivity Mechanisms (v8.9.52)

This document describes the real-time communication protocol and UI synchronization logic used to maintain low-latency situational awareness.

## 1. Protocol Architecture (Socket.io)
The system uses a persistent WebSocket connection via Socket.io for all real-time events.
- **Relay URL**: Configurable in settings. Default is the Render-hosted survival relay.
- **Role Gating**: Tracker emits `location_update` and `log_update`. Viewer emits `command_pulse`.
- **Sync Interval**: Baseline telemetry emission occurs every 10 seconds (`PING_INTERVAL_MS`).
- **RTT Scaling (Issue #315)**: The sync loop dynamically scales up to 30 seconds if network round-trip time (RTT) exceeds 5000ms, preventing loop saturation on poor connections.

## 2. UI Staleness & Ghost Mode (R338)
To prevent "Forensic Delusions" (trusting old data as current), the UI implements strict staleness gates:
- **Telemetry Freshness**: If no packet is received for > 10s (`TELEMETRY_UI_STALE_THRESHOLD_MS`), HUD fields enter a dimmed "Ghost" state.
- **GPS Health**: If the remote GPS fix is > 10s old (`GPS_UI_FAIL_THRESHOLD_MS`), the "TRK" badge transitions to FAIL.
- **Watchdog Countdown**: A real-time visual pulse countdown synchronizes with the 10s heartbeat.

## 3. High-Resilience Watchdog (Issue #366)
Persistence is maintained through a **Triple-Lock** strategy:
1.  **Layer 1 (Foreground)**: Sticky services with active WakeLock renewal on every tick.
2.  **Layer 2 (System)**: `AlarmManager` schedules a hardware wakeup every 90s.
3.  **Layer 3 (OS)**: `WorkManager` performs a periodic verification of service uptime, scheduled on application startup.

## 4. Continuity & Recovery
- **Zombie Detection**: If the socket is silent but HTTP health checks pass, a reconnection is forced.
- **Merge-on-Stale**: If a coordinate update is bypassed due to clock regression, the system still merges updated sensor status (Battery, Vibration) into the forensic record.
- **Boot Grace (Issue #190)**: Includes `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient boot alarms and allow MIUI/HyperOS stabilization.

## 5. Forensic Parity
Every UI element is mapped 1:1 to the `:core:engine` state.
- **Dual-Metric Circles**: Map uncertainty circles reflect both raw GPS accuracy and engine `maxAccuracy`.
- **Bayesian Visualization (Issue #431)**: When fixes are pending, map circles expand at 15m/s (capped at 33.3m/s) to visually communicate the engine's growing uncertainty.
