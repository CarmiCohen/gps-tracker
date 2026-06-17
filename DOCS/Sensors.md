# Sensor Integration & Calibration (v8.8.35)

The system leverages a multi-sensor array to provide high-fidelity physical security and trajectory validation. In v8.8.35, all sensor processing is hardened with monotonic timing (Issue 125) and strictly isolated in the `:core:engine` module.

## 1. Primary Sensors
*   **GNSS (GPS/GLONASS/GALILEO)**: Primary location source. Monitored for stalls and signal gaps (`GPS_GAP_THRESHOLD_MS` 180s). Features **Escalated GPS Revival** (Issue 124) with forensic retry logging.
*   **Accelerometer**: Provides vibration data for `MOVING` vs `PARKING` state transitions (`VIBRATION_STATIONARY_THRESHOLD` 0.12g). Supports shock violation detection (> 0.8g `VIBRATION_SHOCK_THRESHOLD_G`).
*   **Barometer**: Tracks vertical displacement for "Lift" alerts (`BARO_LIFT_THRESHOLD_METERS` 0.8m) and contributes to chair occupancy detection.
*   **Magnetometer**: Heading and orientation monitoring. Used in `TILT_ALERT` (> 15° `TILT_THRESHOLD_DEGREES`).
*   **Microphone**: Acoustic monitoring for tamper detection. Uses `ACOUSTIC_MIN_THRESHOLD_DB` (50dB) floor.
*   **Proximity**: Detects hardware tampering or obstruction. Hardened with `debouncedProximityCm` to prevent real-time UI flickering on virtual sensors (Samsung A15).

## 2. Calibration Mechanisms
*   **Muzzle Window**: A 500ms (`MUZZLE_WINDOW_DURATION_MS`) logic gate that suppresses sensor triggers during high-I/O operations (sync flushes) to eliminate interference-based false positives.
*   **Passive Zeroing**: Automatically calibrates vibration and orientation floors after 300s (`PASSIVE_ZEROING_STATIONARY_MS`) of stationary state.
*   **Bootstrap Phase**: A 60s (`BOOTSTRAP_PHASE_MS`) establishment window for sensor baselines.
*   **Chair Occupancy (R832)**: Uses multi-factor fusion (Tilt 7°, Vibration 0.35g, Baro 0.08m) to detect sitting events.
*   **Monotonic Integrity (Issue 125)**: All sensor-based lockout and suspicion timers use `elapsedRealtime` to ensure absolute consistency across system clock jumps.

## 3. Forensic Alignment & Storage
All sensor events are timestamped and synchronized with the analytical ribbons.
- **Data Fidelity**: Historical points in the database preserve raw physics metrics including speed, bearing, and SIT status.
- **Identity Integrity**: Every sensor-derived forensic entry is tagged with the mandatory `role` field.
- **Persistence**: Sensor-derived indices are buffered and flushed to SQLite in batches (every 5s or 100 points).
