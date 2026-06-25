# Phase: Kinematic Intelligence & Behavioral Refinement (v8.9.37)

This document outlines the strategic objectives for the Kinematic Intelligence phase, focusing on deepening the **GtoEngine's** ability to resolve sensor contradictions and maintaining forensic persistence under extreme OS restrictions.

---

## 1. Objective 1: Xiaomi MIUI 14 Heuristic Recovery (Issue #218)
**Context**: Current logic gates alerts based on "Unknown" status but relies on standard polling.
**Refinement**: Implemented "Xiaomi-Specific Pulse" and `XIAOMI_BOOT_GRACE_MS` (30s) to handle transient boot states (Issue #190).
*   **Mechanism**: If the engine detects a background suppression signature, it triggers an aggressive heuristic pulse via `MainAlarmLogic`.
*   **Action**: System force-refreshes autostart status and attempts service re-bind to wake from deep "Doze" mode.

## 2. Objective 2: Adaptive Jump Confidence (Issue #219)
**Context**: "Jump Points" are filtered based on distance/speed thresholds.
**Refinement**: Correlate Satellite SNR Stability (`ADAPTIVE_JUMP_SNR_THRESHOLD` 35.0f) with IMU Vibration.
*   **Mechanism**:
    *   **Scenario A**: GPS distance jump + High SNR + Zero Vibration = **Signal Reflection/Spoofing**. Uses `ADAPTIVE_JUMP_HOLD_MULTIPLIER` (2.0f) to extend `JUMP_HOLD_DURATION_MS`.
    *   **Scenario B**: GPS distance jump + Low SNR + High Vibration = **Legitimate Movement**. Promotes to ALARM immediately.

## 3. Objective 3: Hindsight Correction (Issue #220)
**Context**: Rejected points are preserved as Magenta Squares but removed from the optimized trail.
**Refinement**: Retroactive Trajectory Smoothing using `HINDSIGHT_BUFFER_SIZE` (5).
*   **Mechanism**: If the system detects a "Jump" followed by a high-confidence trajectory that aligns with rejected points, the engine "Rubber-Bands" them back into the optimized trail.
*   **Value**: Provides a smooth historical trace for forensics without real-time siren risks (Issue #415).

## 4. Objective 4: Acoustic "Location Pending" Optimization (Issue #221)
**Context**: Acoustic triggers fire immediately, but location can be ambiguous.
**Refinement**: Bayesian Confidence Scaling via `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` (15.0f).
*   **Mechanism**: The "Location Pending" state reflects the *growing uncertainty* (confidence radius) based on time elapsed since the last valid GPS fix.
*   **Visual**: Uses `Ghost Mode` (Issue #193) to indicate staleness while displaying the uncertainty expanding on the map.

---
*Status: Objectives integrated and hardened in v8.9.37 baseline.*
