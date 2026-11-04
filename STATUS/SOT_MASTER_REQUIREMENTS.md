# SOT Master Requirements & Hardening Status (Nov4.2)

## 🏗️ Architectural Master Rules (405 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.328 preserved)
*   **1.328 Telemetry Session Service Activation & Parameter Configuration (R-ID 99)**: The Monitor application MUST automatically start `MonitorSessionService` (with API 34+ `dataSync` foreground service compliance) upon pairing with a target tracker ID in `MonitorActivity` to ensure seamless telemetry synchronization and prevent idle "Waiting for Telemetry..." states (GAP-REVOL47.0-G1).
*   **1.329 Default Tracker and Monitor ID Specification (R-ID 100)**: The system MUST set Default Tracker ID to "T" and Default Monitor ID to "V" in `ForensicIdentity` (`DEFAULT_TRACKER_ID = "T"`, `DEFAULT_MONITOR_ID = "V"`) and `ForensicIdentityManager`. The Monitor pairing wizard MUST pre-fill the Tracker ID field with "T" to enable single-tap pairing (GAP-REVOL48.0-G1).
*   **1.330 Multi-Device Telemetry Sync & Expansion (R-ID 101)**: The system MUST support multi-device telemetry synchronization with range-based backfill deduplication in `MonitorRepository`, connection recovery in `SignalingClient`, and automated foreground service lifecycle management in `MonitorSessionService` under simulated network partitions (GAP-REVOL47.0-G1).

## 🛡️ Core Hardening Baseline
- **Nov4.2**: Multi-Device Telemetry Sync & Expansion (GAP-REVOL47.0-G1) - PASSED.
- **Nov4.0**: Default Tracker ("T") & Monitor ("V") ID Specification (GAP-REVOL48.0-G1) - PASSED.
- **Nov3.22**: Telemetry Session Service Activation & Parameter Configuration (GAP-REVOL47.0-G1) - PASSED.
- **Nov3.21**: Manual Device ID Customization & Version Display on First Screens (GAP-REVOL46.0-G1) - PASSED.
- **Nov3.20**: Flexible Device ID Entry & UI/Test Adaptation (GAP-REVOL45.0-G1) - PASSED.
- **Nov3.14**: Physical Device Verification (A15 for Sensor, S21FW for Monitor), Reciprocal Simulation, Full-Project Verification, & Stabilization - PASSED.

## ✅ Verification Chapters
*   **Chapter 5.40 (Nov4.2 Validation Audit)**: PASSED - Verification of multi-device telemetry synchronization, range-based backfill deduplication in `MonitorRepository`, signaling resilience, and automated foreground session service lifecycle in `MonitorSessionService` and `MonitorActivity`.
*   **Chapter 5.39 (Nov4.0 Validation Audit)**: PASSED - Verification of Default Tracker ID "T" and Default Monitor ID "V" in `ForensicIdentity`, `ForensicIdentityManager`, pairing wizard pre-fill, unit test suite, and connected Android test suites on A15 and S21FW.
*   **Chapter 5.38 (Nov3.22 Validation Audit)**: PASSED - Automatic telemetry session service (`MonitorSessionService`) activation upon pairing, API 34+ `dataSync` foreground service compliance, and parameter/viewer ID configuration.
*   **Chapter 5.37 (Nov3.21 Validation Audit)**: PASSED - Manual device ID customization (`setSelfId`) and version name display on the first screen of both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`).
*   **Chapter 5.36 (Nov3.20 Validation Audit)**: PASSED - Flexible device ID validation allowing any non-blank sequence of characters (including single-character IDs) across UI pairing wizard, identity validation logic, and unit/instrumented tests.
