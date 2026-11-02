# SOT Master Requirements & Hardening Status (Nov2.21)

## 🏗️ Architectural Master Rules (398 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.319 preserved)
*   **1.320 Forensic Buffer Integrity Guard (R-ID 91)**: The `ForensicBuffer` MUST perform explicit file existence checks during all I/O operations and synchronize the header metadata BEFORE advancing in-memory indices to ensure zero-loss recovery after external pruning or sudden power loss (R197).
*   **1.321 DataStore Singleton Enforcement (R-ID 92)**: All DataStore instances managing forensic identifiers or critical telemetry settings MUST be enforced as process-level singletons to prevent `IllegalStateException` during concurrent access or unit testing.
*   **1.322 Sentinel Kinematic Expansion (R-ID 93)**: `SentinelLogic` MUST support high-speed transport scenarios (up to 200 km/h) and rapid vehicle acceleration (5.0 m/s²) to prevent false-positive `JUMP` detections during valid high-velocity trajectories.
*   **1.323 Test Stress Mitigation (R-ID 94)**: Instrumented stress tests MUST utilize `yield()` in tight loops and provide adequate collection timeouts (min 2000ms) to prevent thread starvation and false-positive timeouts on budget hardware (A15).

## 🛡️ Core Hardening Baseline
- **Nov2.21**: Remediation of Nov2.20 Regressions (Sentinel, DataStore, Storage Resilience) - PASSED.
- **Nov2.1**: Full Test Suite Verification & Hilt Test Runner Hardening - PASSED.
- **Nov2.0**: Dependency Synchronization & Build Integrity - PASSED.
- **Nov1.7**: Forensic Audit & Hilt Stabilization - PASSED.

## ✅ Verification Chapters
*   **Chapter 5.22 (Nov2.21 Remediation Audit)**: PASSED - Verified trajectory promotion fix, DataStore singleton stability, and storage pressure resilience.
*   **Chapter 5.21 (Nov2.1 Baseline Audit)**: PASSED - Verified full test suite (34 tests) and Hilt instrumentation runner stability.
