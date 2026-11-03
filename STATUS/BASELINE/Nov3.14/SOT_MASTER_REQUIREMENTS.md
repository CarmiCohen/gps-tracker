# SOT Master Requirements & Hardening Status (Nov3.14 Baseline)

## 🏗️ Architectural Master Rules (400 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.319 preserved)
*   **1.320 Forensic Buffer Integrity Guard (R-ID 91)**: The `ForensicBuffer` MUST perform explicit file existence checks during all I/O operations and synchronize the header metadata BEFORE advancing in-memory indices to ensure zero-loss recovery after external pruning or sudden power loss (R197).
*   **1.321 DataStore Singleton Enforcement (R-ID 92)**: All DataStore instances managing forensic identifiers or critical telemetry settings MUST be enforced as process-level singletons to prevent `IllegalStateException` during concurrent access or unit testing.
*   **1.322 Sentinel Kinematic Expansion (R-ID 93)**: `SentinelLogic` MUST support high-speed transport scenarios (up to 200 km/h) and rapid vehicle acceleration (5.0 m/s²) to prevent false-positive `JUMP` detections during valid high-velocity trajectories.
*   **1.323 Test Stress Mitigation (R-ID 94)**: Instrumented stress tests MUST utilize `yield()` in tight loops and provide adequate collection timeouts (min 2000ms) to prevent thread starvation and false-positive timeouts on budget hardware (A15).
*   **1.324 Adaptive Buffer Capacity Scaling (R-ID 95)**: The `ForensicBuffer` MUST dynamically scale buffer allocation (`calculateAdaptiveCapacity`) based on sensor sampling frequency and retention window requirements across variable SoC vendor bitrates (GAP-REVOL38.3-G1).
*   **1.325 Continuous Kinematic Calibration (R-ID 96)**: The `SensingEngine` MUST dynamically recalibrate the IMU noise floor during high-acceleration vehicle transitions (`calculateKinematicAdaptiveFloor`), scaling the noise floor baseline relative to acceleration magnitude (`accelFactor`) to ensure robust stationary detection (`isStationary`) under changing kinetic baselines (GAP-REVOL39.0-G1).

## 🛡️ Core Hardening Baseline
- **Nov3.14**: Physical Device Verification (A15 for Sensor, S21FW for Monitor), Reciprocal Simulation, Full-Project Verification, & Stabilization - PASSED.
- **Nov3.13**: Reciprocal Virtual Presence Simulation, Full-Project Verification, & Stabilization - PASSED.
- **Nov3.12**: Reciprocal Virtual Presence Simulation, Full-Project Verification, & Stabilization - PASSED.
- **Nov3.11**: Exhaustive Full-Project Verification, Stress Evaluation, & Stabilization - PASSED.
- **Nov3.1**: Continuous Kinematic Calibration & Dynamic IMU Noise Floor Recalibration (GAP-REVOL39.0-G1) - PASSED.
- **Nov3.0**: Adaptive JNI Buffer Scaling & Variable Bitrate Retention (GAP-REVOL38.3-G1) - PASSED.
- **Nov2.25**: Multi-Peer Forensic Integrity & Thermal Hysteresis (R133) - PASSED.
- **Nov2.24**: Exhaustive Full-Project Verification & Stress Evaluation - PASSED.
- **Nov2.23**: Full Project Test Battery & Forensic Stress Evaluation - PASSED.
- **Nov2.21**: Remediation of Nov2.20 Regressions (Sentinel, DataStore, Storage Resilience) - PASSED.
- **Nov2.1**: Full Test Suite Verification & Hilt Test Runner Hardening - PASSED.
- **Nov2.0**: Dependency Synchronization & Build Integrity - PASSED.

## ✅ Verification Chapters
*   **Chapter 5.35 (Nov3.14 Validation Audit)**: PASSED - Physical device verification using A15 (`R58X40GV2AR`) for sensor and S21FW (`R5CRC14PG4F`) for monitor, reciprocal virtual presence testing, and exhaustive test suite verification.
*   **Chapter 5.34 (Nov3.13 Validation Audit)**: PASSED - Reciprocal virtual presence testing (viewer presence validating tracker behavior, and tracker presence validating tracker behavior), plus exhaustive test suite verification.
*   **Chapter 5.33 (Nov3.12 Validation Audit)**: PASSED - Reciprocal virtual presence testing (viewer presence validating tracker behavior, and tracker presence validating tracker behavior), plus exhaustive test suite verification.
*   **Chapter 5.32 (Nov3.11 Validation Audit)**: PASSED - Exhaustive verification of all automated unit tests, hardening procedures, and stress evaluations across all modules.
*   **Chapter 5.31 (Nov3.1 Validation Audit)**: PASSED - Verified Continuous Kinematic Calibration (`calculateKinematicAdaptiveFloor`, `isStationary`) in `SensingEngine` under high-acceleration vehicle transitions.
*   **Chapter 5.30 (Nov3.0 Validation Audit)**: PASSED - Verified Adaptive Buffer Scaling (`calculateAdaptiveCapacity`) in `ForensicBuffer` and full lifecycle integration in `SensorService`.
*   **Chapter 5.25 (Nov2.25 Validation Audit)**: PASSED - Verified Thermal Hysteresis (R133) and Multi-peer backfill contention deduplication under high latency.
