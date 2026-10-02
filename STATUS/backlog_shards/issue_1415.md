# Issue #1415: CPU-Load Compensation for Sensors

## 🎯 Objective
Refine the `Passive Zeroing` and sensor evaluation logic to detect and compensate for hardware drift or "ghost" vibration spikes that occur during periods of high CPU saturation.

## 🚩 Problem Statement
During the **Oct.1.8** automated stress tests, high CPU load (100% saturation) caused the LIS2DLC12 accelerometer on the A15 to report erratic vibration deltas. This led to false `TAMPER_DETECTED` violations even when the device was physically stationary.

## 🛠️ Proposed Mediation
*   **Load-Aware Gating**: Integrate `SystemHealthState.cpuLoad` into the `SentinelValidator`.
*   **Hysteresis Expansion**: Temporarily expand the `VIBRATION_STATIONARY_THRESHOLD` or increase the `STATIONARY_FLOOR_MULT` when CPU load exceeds 85%.
*   **Calibration Suppression**: Pause `Passive Zeroing` recalibration during CPU saturation bursts to prevent baseline corruption.

## 📊 Requirements
*   **R-ID 590**: The system MUST ignore IMU jitter correlated with CPU load spikes above 0.85 to maintain Tracker stealth.
*   **R-ID 591**: `Passive Zeroing` must use a "Stable Load" gate before committing new sensor floors.
