# GPS Accuracy & Filtering Mechanism (v8.9.10)

This document describes the multi-stage filtering pipeline used to ensure high-fidelity location tracking and jump rejection.

## 1. The Filtering Pipeline
1.  **Monotonic Guard**: Rejects updates with backward-moving hardware timestamps (`CLOCK_REGRESSION_GATE_MS` 100ms).
2.  **Accuracy Gate**: Ignores fixes with accuracy > `TRAJECTORY_REJECTION_ACCURACY_MULT` (3x) the recent high-accuracy average (`HIGH_ACCURACY_THRESHOLD_METERS` 35m).
3.  **ImmFilter (Interacting Multiple Model)**: 
    - **Stationary Model**: Aggressive smoothing for engine-idle/parking states.
    - **Kinematic Model**: High-responsiveness for active movement.
4.  **Behavioral Sentinel**: Validates GPS movement against accelerometer vibration data.

## 2. Forensic Enhancements (v8.9.10)
- **Log Spatial Anchor**: All filtering events (Jumps, Outliers, Promotions) are now geographically anchored.
- **Data Parity**: Historical points include raw speed, bearing, and battery current (`currentMa`).
- **Ghost Mode**: Visual staleness indicators (Slate500) applied to markers and accuracy circles when data > 10s old.

## 3. Jump Rejection (Sentinel Status)
- **JUMP**: Validated GPS spike rejected by sensor cross-validation.
- **OUTLIER**: Extreme coordinate jump (> 2000m) rejected by physics check.
- **PROMOTED**: Trajectory confirmed through consistent movement, overriding initial jitter filters.
