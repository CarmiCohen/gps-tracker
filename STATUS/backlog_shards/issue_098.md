# Issue #098: Samsung Stay-Alive Fallback (R405c)

## Status: Resolved (July.20.07)
## Requirement: R405c

### Description
On Samsung devices, hardware sensors (Step Detector) may fail to register after a period of inactivity or system-level optimization, even if the service is in the foreground. This results in the "Stationary Hard-Lock" never being challenged, causing tracking stalls.

### Resolution
- **Sensor Monitoring**: Implemented a watchdog in `AppSensorManager` that detects if sensor registration calls return `false` or if no events are received within 60s of expected motion.
- **Accelerometer Pulse**: If the primary sensor fails, the system immediately engages a high-frequency Accelerometer pulse to force the OS to keep the process active and attempt to re-establish the coordinate lock.
- **Self-Healing**: The fallback periodically attempts to re-register the Step Detector every 5 minutes.

### Verification
- [x] Verified on Samsung S21 that tracking resumes after simulated sensor failure.
- [x] Logcat confirms "Engaging Accelerometer Fallback Pulse" on sensor timeout.
