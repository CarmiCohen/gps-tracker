# GPS Accuracy & Filtering Mechanism (v8.8.35)

This document describes how the application handles GPS precision, accuracy-based buffering, and filtering to ensure reliable tracking and minimize false alarms.

## 1. Raw Accuracy Acquisition
The system retrieves horizontal accuracy (meters) from the Android Location API.
- **Transmission**: Accuracy is included in every telemetry packet.
- **Interpretation**: Lower values (e.g., 3m) indicate high precision; higher values (e.g., 50m) indicate signal drift.
- **Forensic Extension**: v8.8.35 includes raw **speed** and **bearing** in history records to supplement accuracy metrics.

## 2. Maximum Accuracy Buffer (`maxTrackerAccuracy`)
The app tracks a "High-Water Mark" for accuracy using a multi-bucket sliding window.
- **Logic**: The system maintains 4 buckets of 15 seconds each (`ACCURACY_WINDOW_BUCKET_MS` 60s total window).
- **Mechanism**: The sliding window (`ACCURACY_WINDOW_MAX_SIZE` = 4) ensures that high-uncertainty spikes phase out gradually.
- **Purpose**: Prevents false geofence triggers during temporary signal degradation while allowing faster recovery after interference.

## 3. Dynamic Geofence Calculation
The perimeter is a dynamic boundary adapting to signal quality:
- **Multiplier**: `Buffer = maxAccuracy * GEOFENCE_BUFFER_MULT` (6.0).
- **Expansion**: `GEOFENCE_ACCURACY_EXPANSION_MULT` (1.0).
- **Hysteresis**: `GEOFENCE_ACCURACY_HYSTERESIS_MULT` (1.10).
- **Total Perimeter**: `Threshold = UserRadius + Buffer`.
- **Predictive Buffer**: The system adds a speed-aware look-ahead margin (`GEOFENCE_PREDICTIVE_LOOKAHEAD_S` 2.0s). Violations are triggered if the tracker is projected to exit the fence within 2 seconds.

## 4. Tiered Filtering & Jump Rejection
The engine (now isolated in `:core:engine`) utilizes a multi-layered validation strategy:
- **Tier 1 (Outlier)**: Immediate rejection of points implying > 300 km/h (`OUTLIER_SPEED_CAP_MPS`) or > 2000m displacement (`OUTLIER_DISTANCE_THRESHOLD`).
- **Tier 2 (Security Jump)**: Sudden displacement (100m - 2000m) or speed (> 120 km/h `MAX_PHYSICAL_SPEED_MPS`) held for 180s (`JUMP_HOLD_DURATION_MS`) for sensor fusion confirmation.
- **Tier 3 (Jitter)**: Radial noise filtered via path efficiency (`PATH_EFFICIENCY_THRESHOLD` 0.1) and angular momentum checks.
- **Trajectory Promotion**: Consistent movement (speed > 2.0 m/s for > 30s `TRAJECTORY_PROMOTION_WINDOW_MS`) bypasses the "Jump" hold for immediate alerting.

## 5. UI Visualization
- **Accuracy Circles**: Rendered around the tracker marker. 
    - **Green**: Fresh data within accuracy limits.
    - **Red**: Stale data or high-drift uncertainty (7s `GPS_UI_FAIL_THRESHOLD_MS`).
- **Interactive SNR**: Tapping the "Sats" card in the HUD opens a detailed per-satellite signal strength overlay (`snrIdx`).

## 6. Movement Verification
- **Samples Required**: The `DISTANCE_ALARM_SAMPLES_REQUIRED` (default: 6) ensures a sustained breach before the siren triggers, unless a Trajectory Promotion occurs.
- **Anti-Flapping**: A 60s `MOVING_HOLD_DURATION_MS` prevents rapid toggling between stationary and moving states in high-interference areas.

## 7. Forensic Unification
Legacy version tags (`ver`, `vid`) have been removed from data models and database schemas in v8.8.35 to simplify the forensic model while maintaining high-fidelity filtering and accuracy tracking.
