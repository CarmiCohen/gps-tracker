# Issue #526b: Power Optimization - Adaptive Sensor Sampling

## Status: Resolved (July.23.03)
## Requirement: R403b, R406a-H, R810-L2

### Description
The system required deeper power-saving measures when the device is stationary and GPS is stalled (e.g., in a garage or deep indoors) to extend battery life during long-term monitoring.

### Resolution
- **Logic Tier Optimization**: Implemented a 10s logic tick (up from 2s) when stationary.
- **Acoustic Duty Cycle**: Switched microphone to a 20% duty cycle (2s ON / 8s OFF).
- **Hardware Tier Sampling**: Dynamically downgraded `Linear Acceleration` sampling to `SENSOR_DELAY_NORMAL` when movement is not detected.
- **Centralized Management**: Unified power-save state evaluation in `ServiceBehaviorUseCase.kt`.

### Verification
- [x] Verified power consumption reduction on Samsung A15.
- [x] Confirmed immediate breakout from power-save when physical IMU triggers occur.
- [x] Verified Forensic Snapshot integrity during duty cycle transitions.
