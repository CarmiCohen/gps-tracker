# Phase: Kinematic Intelligence & Behavioral Refinement (v8.9.18)

This document outlines the strategic objectives for the v8.9.18 development phase, focusing on deepening the **GtoEngine's** ability to resolve sensor contradictions and maintaining forensic persistence under extreme OS restrictions.

---

## 1. Objective 1: Xiaomi MIUI 14 Heuristic Recovery (Issue #218)
**Context**: Current logic gates alerts based on "Unknown" status but relies on standard polling.
**Refinement Idea**: Implement a "Xiaomi-Specific Pulse."
*   **Mechanism**: If the engine detects a background suppression signature (e.g., missed ticks or sudden SNR drops), it will trigger an aggressive heuristic pulse.
*   **Action**: Instead of just logging "Missing," the system will force-refresh the autostart status and attempt a low-level service re-bind to wake the system from deep "Doze" mode.

## 2. Objective 2: Adaptive Jump Confidence (Issue #219)
**Context**: "Jump Points" are currently filtered based on distance/speed thresholds.
**Refinement Idea**: Correlate Satellite SNR Stability with IMU Vibration.
*   **Mechanism**:
    *   **Scenario A**: GPS distance jumps 150m + High SNR + Zero Vibration = **Signal Reflection/Spoofing**. Increase `JUMP_HOLD_DURATION_MS` from 180s to 300s.
    *   **Scenario B**: GPS distance jumps 150m + Low SNR + High Vibration = **Legitimate Movement (Towed/Loaded)**. Decrease Hold and promote to ALARM immediately.

## 3. Objective 3: Hindsight Correction (Issue #220)
**Context**: Rejected points are preserved as Magenta Squares but removed from the optimized trail.
**Refinement Idea**: Retroactive Trajectory Smoothing (Rubber-Band Logic).
*   **Mechanism**: If the system detects a "Jump" followed by a high-confidence trajectory that aligns with the rejected points in hindsight, the engine will "Rubber-Band" those points back into the optimized trail.
*   **Value**: Provides a perfectly smooth historical trace for forensics without risking false siren triggers in real-time.

## 4. Objective 4: Acoustic "Location Pending" Optimization (Issue #221)
**Context**: Acoustic triggers fire immediately, but the "Location Pending" state can be ambiguous.
**Refinement Idea**: Bayesian Confidence Scaling.
*   **Mechanism**: Refine the UI transition. If an Acoustic trigger occurs, the system will display the "Location Pending" icon with a confidence radius based on the *time elapsed* since the last valid GPS fix.
*   **Visual**: The "Location Pending" state will now reflect the *growing uncertainty* of the device's actual position until the next valid fix arrives.

---
*Baseline set at v8.9.17. Ready for Kinematic implementation.*
