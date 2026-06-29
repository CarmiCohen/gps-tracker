# Specification: GtoEngine (Graph Trajectory Optimization) (v8.9.52)

This document specifies the architecture and logic for the **GtoEngine**, an advanced optimization-based reconstruction system for tracking heavy tractor assets. 

## 1. Concept Summary
The **GtoEngine** (GTO: Graph Trajectory Optimization) uses a sliding-window factor graph to solve for the most likely path of the tracker. Unlike a Kalman filter which processes points one-by-one, GTO evaluates the relationship between a sequence of points and applies physical and environmental constraints.

### Why GTO for a Heavy Tractor?
Heavy tractors operate in environments with high mechanical vibration and signal reflection. GTO excels here because it can retroactively identify a "mechanical jump" or a "signal spike" by looking at the consistency of the path that follows it.

---

## 2. Detailed Architecture: The Factor Graph

The engine maintains a "Sliding Window" of nodes (GPS fixes) and "Factors" (constraints between nodes).

### 2.1. The Sliding Window
*   **Capacity**: 30-60 seconds of history (`HINDSIGHT_BUFFER_SIZE` = 10 points).
*   **Mechanism**: As new points arrive, the oldest points are "fixed" (marginalized) while the new points are optimized in real-time.

### 2.2. Factors (Constraints)
*   **GPS Factors**: The raw coordinate and its reported accuracy.
*   **Kinematic Factors**: Constraints based on tractor physics (`MAX_TRACTOR_ACCEL` < 2.0 m/s²).
*   **IMU Factors**: Correlation between physical vibration (Accelerometer) and movement distance.
*   **Magnetometer Factors**: Heading consistency check.

---

## 3. Advanced Classification Logic

### 3.1. Hindsight Correction & Promotion (Issue #461)
If a point arrives that implies a "Jump", the GtoEngine evaluates the sequence:
*   **Scenario A**: Next points return to the origin. **Decision**: The jump was noise. It is smoothed out.
*   **Scenario B**: Next points continue from the jump location. **Decision**: The jump was real high-speed movement. 
*   **Promotion**: The "Jump Hold" is canceled via **Trajectory Promotion**. Every node in the window is pushed to the forensic trail.
*   **Forensic Parity**: All promoted nodes MUST preserve their original `accuracy` and `maxAccuracy` context to maintain audit integrity (Issue #461 / Formerly #435).

### 3.2. Mechanical Vibration Signature (Issue #433)
*   **Work vs. Theft**: The engine analyzes the accelerometer frequency. 
    *   **High-Freq Vibration + Slow Movement**: The tractor is working. Thresholds are widened to allow for "mechanical jitter." (`GTO_WORK_SPEED_THRESHOLD` = 5.0 m/s).
    *   **Zero Vibration + Fast Movement**: The tractor is being towed. Thresholds are tightened. (`GTO_TOW_SPEED_THRESHOLD` = 10.0 m/s).

### 3.3. Radial Jitter (Zig-Zag) Filtering
By evaluating the graph's **Path Efficiency**, the engine calculates the ratio of `Displacement / PathLength`. If the efficiency is low, the engine "collapses" the jitter into a single stationary node.

---

## 4. Multilayer Implementation Strategy

The GtoEngine operates as a "State-Aware Optimizer":

1.  **Stage 1: The Sentry Gate**: Deterministic thresholds discard garbage data.
2.  **Stage 2: Graph Update**: New GPS and IMU data are added as nodes and factors.
3.  **Stage 3: Optimization**: The engine solves the graph to find the "Least-Energy" path.
4.  **Stage 4: Alarm Validation**: If the *optimized* path violates the geofence, the system evaluates the **Trajectory Confidence**.
    *   **Consistent Trajectory**: Immediate Alarm (via 30s `TRAJECTORY_PROMOTION_WINDOW_MS`).
    *   **Erratic Trajectory (Jump)**: 180s Security Hold (`JUMP_HOLD_DURATION_MS`).

---

## 5. Comparison: Sentinel vs. GtoEngine

| Metric | LocationSentinel | GtoEngine |
| :--- | :--- | :--- |
| **Logic Type** | Predictive (Recursive) | Optimization (Sliding Window) |
| **Lag** | Lower (Immediate output) | Slightly higher (Wait for window) |
| **Accuracy** | High | **Superior** (Retroactive correction) |
| **False Alarms** | Low | **Negligible** (Filters mechanical noise) |
| **CPU Cost** | Low | Moderate (Matrix solving) |

## 6. Summary Conclusion
The **GtoEngine** provides a "Truth-at-Source" model that understands the difference between a tractor bouncing in a field and a tractor being moved by an unauthorized party. In v8.9.52, this logic is strictly isolated and geographically anchored via **Dual-Metric Spatial Anchors**, ensuring forensic integrity.
