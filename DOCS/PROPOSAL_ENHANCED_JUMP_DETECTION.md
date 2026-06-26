# Proposal: Enhanced Jump Detection & Logic (v8.9.37)

This document summarizes the improvements to the "Jump Point" detection mechanism in the GPS Tracker project. It consolidates the mathematical, behavioral, and environmental filters into a formal specification for the **GtoEngine** (Adaptive Multi-Factor Jump Engine).

## 1. Executive Summary
The "Jump Point" logic uses tiered speed and distance thresholds to identify erratic GPS data. While effective at preventing basic false alarms, it is designed to catch "Micro-Jitter" and "Clever Theft" where intentional signal interference might be used.

The engine uses a **weighted probability model** that considers acceleration, directional consistency, distance floors, and sensor fusion. In v8.9.37, this is fully isolated in the `:core:engine` module and synchronized with the Source of Truth. (Issue #309 - Formerly #285)

---

## 2. Implementation State (v8.9.37)

| Feature | Status | Implementation |
| :--- | :--- | :--- |
| **Primary Trigger** | Active | Weighted Confidence Score (0-100). |
| **Distance Floor** | Active | **100m Minimum** (`JUMP_POINT_DISTANCE_THRESHOLD`) for "Security Jump" classification (Tier 2). |
| **Hold Logic** | Active | **180s Security Hold** (`JUMP_HOLD_DURATION_MS`): Suppresses alarms during transient jumps. |
| **Physics** | Active | **Acceleration Monitoring** (Limit: 2.0 m/s² `MAX_TRACTOR_ACCEL`). |
| **Behavior** | Active | **Angular Variance** & **Zig-Zag Detection** (Path Efficiency < 0.1 `PATH_EFFICIENCY_THRESHOLD`). |

---

## 3. Detailed Specification: The Three-Tier Threshold Model

To handle different data scenarios, the system categorizes GPS updates into three tiers:

### Tier 1: Outlier Filter (Infrastructure Level)
*   **Goal**: Prevent UI corruption and database bloat from impossible data.
*   **Criteria**: Distance > 2,000m (`OUTLIER_DISTANCE_THRESHOLD`) OR Speed > 300 km/h (`OUTLIER_SPEED_CAP_MPS`).
*   **Action**: Discard point completely; log "Hardware/Cold-Start Outlier."

### Tier 2: Security Jump (Hold Level)
*   **Goal**: Prevent false siren triggers from significant GPS signal reflection.
*   **Criteria**: Distance 100m – 2,000m OR Speed > 120 km/h (`MAX_PHYSICAL_SPEED_MPS`).
*   **Action**: Map as **Magenta Square**; Trigger 180s "Jump Hold" in Alarm Logic.

### Tier 3: Visual Jitter (Smoothing Level) (Issue #304)
*   **Goal**: Keep the map trail clean without delaying legitimate alarms.
*   **Criteria**: Radial noise that doesn't meet Tier 1/2 criteria. Uses `JUMP_GATE_VISUAL_JITTER_METERS` (10.0m) as rejection floor.
*   **Action**: Exclude from Polyline; **Allow** standard 6-sample Alarm Logic (No Hold).

---

## 4. Technical Improvements Specification

### 4.1. Velocity Inertia (Acceleration Check)
The system tracks the implied acceleration. A point is flagged if it exceeds physical limits of a heavy asset.
*   **Limit**: $2.0 m/s^2$ (Max Tractor Acceleration).
*   **Context**: Sensitivity to acceleration spikes is increased when the device is in the `PARKING` state.

### 4.2. Angular Momentum (Zig-Zag Filter)
Calculate the bearing change between consecutive points.
*   **Rule**: If the angle $\Delta > 120^\circ$ and speed is high, the "Jump Probability" increases.
*   **Path Efficiency**: If `Displacement / PathLength < 0.1`, the points are classified as **Radial Noise** (Jitter).

### 4.3. Consistency Promotion (Anti-Tampering)
If the system detects a "Jump" followed by consistent movement (speed > 2.0 m/s for > 30s `TRAJECTORY_PROMOTION_WINDOW_MS` with path efficiency > 0.1), the "Jump Hold" is immediately canceled.
*   **Rationale**: Consistent high-speed movement represents a **Trajectory**, not a jitter. Promote to **CRITICAL ALARM** immediately (Issue #309).

### 4.4. Predictive Exit
Speed-aware projection that triggers geofence alarms if the projected position (2.0s look-ahead `GEOFENCE_PREDICTIVE_LOOKAHEAD_S`) is outside the fence, provided it's not a Jump Point.

## 5. Forensic Unification
As of v8.9.37, the forensic model is simplified and hardened. Legacy version tags have been removed from data models. Traceability is maintained at the emission layer and enhanced by **Log Spatial Anchors** (Issue #208).
