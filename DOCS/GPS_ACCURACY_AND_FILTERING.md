# GPS Accuracy & Filtering Mechanism (v8.9.52)

This document describes the multi-stage filtering pipeline used to ensure high-fidelity location tracking and jump rejection.

## 1. The Filtering Pipeline
1.  **Monotonic Guard**: Rejects updates with backward-moving hardware timestamps (`CLOCK_REGRESSION_GATE_MS` 100ms). (Issue #311)
2.  **Accuracy Gate (Issue #303)**: Ignores fixes with accuracy > `TRAJECTORY_REJECTION_ACCURACY_MULT` (3.0f) the recent high-accuracy average (`HIGH_ACCURACY_THRESHOLD_METERS` 35.0f).
3.  **ImmFilter (Interacting Multiple Model)**: 
    - **Stationary Model**: Aggressive smoothing for engine-idle/parking states. Uses `IMM_STATIONARY_PROBABILITY` (0.8).
    - **Kinematic Model**: High-responsiveness for active movement. Uses `IMM_KINEMATIC_PROBABILITY` (0.2).
4.  **Behavioral Sentinel (Issue #302)**: Validates GPS movement against accelerometer vibration data. Uses `VIBRATION_STATIONARY_THRESHOLD` (0.12f) to distinguish movement.

## 2. Bayesian Authority (Issue #431)
When location fixes are pending (due to stalls or violations), the system utilizes **Bayesian Uncertainty Expansion**:
- **Expansion Rate**: Accuracy radius grows at **15.0m/s** (Moving) or **1.5m/s** (Stationary).
- **Safety Cap**: Expansion is strictly capped at **33.3m/s** (120 km/h) to maintain threshold sanity.
- **Enforcement**: This expanded uncertainty is the primary authority for the geofence breach logic and the visual map indicators.

## 3. Deduplication & Persistence (Issue #450)
- **Authoritative Gate**: `maxAccuracy` is the exclusive spatial gate for data persistence.
- **Sensitivity**: Movement is deduplicated using a **0.5x multiplier** of the current authoritative uncertainty (`DEDUPLICATION_SPATIAL_GATE_FACTOR`).

## 4. Jump Rejection (Sentinel Status)
- **JUMP**: Validated GPS spike rejected by sensor cross-validation. Uses `JUMP_GATE_VISUAL_JITTER_METERS` (10.0m) floor.
- **SNR Latch (Issue #452)**: Signal reflections with high SNR (≥35) trigger a **6-minute (360s)** security hold to prevent siren flickering.
- **OUTLIER**: Extreme coordinate jump (> 2000m) rejected by physics check.
- **PROMOTED**: Trajectory confirmed through consistent movement within `TRAJECTORY_PROMOTION_WINDOW_MS` (30s). Promoted points strictly preserve forensic metadata (Issue #435).
