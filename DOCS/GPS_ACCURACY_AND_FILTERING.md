# GPS Accuracy & Filtering Mechanism (v8.9.37)

This document describes the multi-stage filtering pipeline used to ensure high-fidelity location tracking and jump rejection.

## 1. The Filtering Pipeline
1.  **Monotonic Guard**: Rejects updates with backward-moving hardware timestamps (`CLOCK_REGRESSION_GATE_MS` 100ms).
2.  **Accuracy Gate (Issue #303)**: Ignores fixes with accuracy > `TRAJECTORY_REJECTION_ACCURACY_MULT` (3.0f) the recent high-accuracy average (`HIGH_ACCURACY_THRESHOLD_METERS` 35.0f).
3.  **ImmFilter (Interacting Multiple Model)**: 
    - **Stationary Model**: Aggressive smoothing for engine-idle/parking states. Uses `IMM_STATIONARY_PROBABILITY` (0.8).
    - **Kinematic Model**: High-responsiveness for active movement. Uses `IMM_KINEMATIC_PROBABILITY` (0.2).
4.  **Behavioral Sentinel (Issue #302)**: Validates GPS movement against accelerometer vibration data. Uses `VIBRATION_STATIONARY_THRESHOLD` (0.12f) to distinguish movement.

## 2. Forensic Enhancements (v8.9.37)
- **Log Spatial Anchor (Issue #208)**: All filtering events (Jumps, Outliers, Promotions) are now geographically anchored using `lat`/`lng` in the log record.
- **Data Parity (Issue #192)**: Historical points include raw speed, bearing, and battery current (`currentMa`).
- **Ghost Mode (Issue #193)**: Visual staleness indicators applied to markers and accuracy circles when telemetry > `TELEMETRY_UI_STALE_THRESHOLD_MS` (10s).

## 3. Jump Rejection (Sentinel Status)
- **JUMP**: Validated GPS spike rejected by sensor cross-validation. Uses `JUMP_GATE_VISUAL_JITTER_METERS` (10.0m) as a minimum rejection floor (Issue #304).
- **OUTLIER**: Extreme coordinate jump (> `OUTLIER_DISTANCE_THRESHOLD` 2000m) rejected by physics check.
- **PROMOTED**: Trajectory confirmed through consistent movement within `TRAJECTORY_PROMOTION_WINDOW_MS` (30s), overriding initial jitter filters (Issue #285).
