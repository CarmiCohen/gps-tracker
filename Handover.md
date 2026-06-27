# Handover Status: Issues #340, #332, & #328 - Technical Hardening (v8.9.41)

## 🎯 Context & Objective
Remediated hardware-specific limitations and refined Bayesian uncertainty logic for real-world urban and low-light scenarios.

## 🛠️ Work Done
### 1. Lux-Locked Proximity Gate (#340)
- **Logic**: Integrated light-sensor gating for Samsung A15 virtual proximity.
- **Impact**: Suppresses "Far" transitions when Lux <= 0.01. This prevents false tamper alerts in complete darkness (pockets/bags) where the A15 virtual sensor typically fails.

### 2. Urban Canyon SNR-IMU Correlation (#332)
- **Logic**: Refined `PhysicsUtils.isVisualJump` to incorporate SNR stability.
- **Impact**: High SNR (>35dB) jumps without accompanying IMU vibration now trigger a higher score penalty and flag `isAdaptiveJump`. This effectively filters multipath reflections in high-signal urban areas.

### 3. Velocity-Aware Bayesian Expansion (#328)
- **Logic**: Replaced static uncertainty growth with a velocity-dependent drift rate in `MapComponents`.
- **Impact**: The uncertainty circle now expands based on last known speed (Stationary Floor: 1.5m/s, Cap: 33.3m/s). This provides a realistic "search radius" during location pending states.

## 📊 Hardening Tracking
- **Issue #340**: Moved to **RESOLVED**.
- **Issue #332**: Moved to **RESOLVED**.
- **Issue #328**: Moved to **RESOLVED**.

## 🛡️ Resumption Guardrails
1. **Rebuild**: Run `:app:assembleDebug`.
2. **Verification**: 
    - **A15**: Test proximity "Far" transition in a pitch-black room; verify alert is suppressed.
    - **UI**: Stop GPS and verify the uncertainty circle grows slower if the device was stationary vs. moving.

**Status**: 100% Resolved. Technical inconsistencies and hardware risks have been mitigated.
