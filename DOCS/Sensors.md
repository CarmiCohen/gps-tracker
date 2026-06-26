# Sensor Integration & Calibration (v8.9.37)

The system leverages a multi-sensor array to provide high-fidelity physical security and trajectory validation. In v8.9.37, all sensor processing is hardened with monotonic timing and strictly isolated in the `:core:engine` module.

## 1. Primary Sensors
*   **GNSS (GPS/GLONASS/GALILEO)**: Primary location source. Monitored for stalls and signal gaps (`GPS_GAP_THRESHOLD_MS` 60s). Features **Escalated GPS Revival** (Issue #124 / #198) with forensic retry logging every 120s.
*   **Accelerometer**: Provides vibration data for `MOVING` vs `PARKING` state transitions. Supports shock violation detection (> 0.8g).
*   **Barometer**: Tracks vertical displacement for "Lift" alerts and contributes to chair occupancy detection.
*   **Magnetometer**: Heading and orientation monitoring. Used in `TILT_ALERT`.
*   **Microphone**: Acoustic monitoring for tamper detection. Uses `ACOUSTIC_MIN_THRESHOLD_DB` (50dB) absolute floor.
*   **Proximity**: Detects hardware tampering or obstruction. Hardened with specialized hysteresis (5s for Samsung A15 - Issue #191).

## 2. Calibration Mechanisms
*   **Muzzle Window**: A 2000ms logic gate suppresses sensor triggers during high-I/O sync operations. (Issue #191)
*   **Passive Zeroing**: Automatically calibrates baselines after 300s of stationary state.
*   **Chair Occupancy**: Uses multi-factor fusion (Tilt 7°, Vibration 0.35g, Baro 0.08m) to detect sitting events. (Issue #282)
*   **Monotonic Integrity**: All sensor-based lockout and suspicion timers use `TimeProvider.elapsedRealtime()`.

## 3. Forensic Alignment & Storage
All sensor events are timestamped and synchronized with the analytical ribbons.
- **Log Spatial Anchor**: Every sensor violation or detection event is now automatically anchored with `lat`/`lng` coordinates.
- **Ghost Mode UX**: Dashboard fields enter a dimmed "Ghost" state if telemetry is older than 10s (`TELEMETRY_UI_STALE_THRESHOLD_MS`).
- **Persistence**: Sensor-derived indices are buffered and flushed to SQLite in batches.
