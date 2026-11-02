# SOT Master Requirements & Hardening Status (Nov1.7)

## 🏗️ Architectural Master Rules (392 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.307 as per Revol31.8 baseline)
*   **1.308 Geofence Transition Forensic Integrity (R-ID 775)**: The `SentinelLogic` MUST detect `ENTER`/`EXIT` transitions with nanosecond precision, and the `ForensicBuffer` MUST persist these events alongside their `monotonicNanos` and `status` strings to ensure a complete, non-repudiable forensic trail of spatial violations. (Revol31.8 - ISSUE-REVOL30.1-F3).
*   **1.309 Signaling Pipeline Soak Integrity (R-ID 776)**: The `SignalingQueue` MUST implement critical event prioritization and support at least 5000 in-memory events to guarantee zero telemetry loss during extended network disconnections at 10Hz. All "ENTER/EXIT" and "JUMP" status events MUST be protected from queue eviction during overflow conditions. (Revol31.9 - ISSUE-REVOL31.9-F1).
*   **1.310 FGS Permission & API 34 Security (R-ID 10)**: All Foreground Services MUST implement strict runtime permission guards before calling `startForeground` to prevent `SecurityException` on API 34+. Specifically, `location` types MUST verify `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` and include appropriate type bits (`LOCATION`, `DATA_SYNC`, `REMOTE_MESSAGING`) to match manifest declarations and operational needs. (Nov1.0 - ISSUE-REVOL31.9-F2).
*   **1.311 Backfill Recovery Contention Audit (R-ID 80)**: The `MonitorRepository` MUST implement range-based request deduplication to prevent redundant backfill requests during concurrent telemetry ingestion. Telemetry ingestion MUST ensure zero duplicate records in the `TrailDao` even during multi-peer contention or overlapping backfill responses. (Nov1.1 - ISSUE-REVOL32.0-F1).
*   **1.312 Forensic Signaling Batching (R-ID 81)**: The `SignalingClient` MUST implement adaptive telemetry batching (time and size based) to optimize packet overhead during high-frequency bursts or recovery. Critical status updates (e.g., Geofence transitions) and Violation events MUST trigger an immediate batch flush to maintain real-time operational responsiveness. (Nov1.2 - ISSUE-REVOL33.0-F1).
*   **1.313 Adaptive Telemetry Sampling (R-ID 82)**: The `SensorService` MUST implement velocity-based adaptive sampling to optimize battery life. Telemetry frequency MUST scale dynamically: 10Hz when moving (> 5 m/s), 1Hz during slow movement, and 0.1Hz when stationary. The `TickOrchestrator` MUST support preemption to ensure immediate adaptation to velocity changes. (Nov1.3 - ISSUE-REVOL34.0-F1).
*   **1.314 Battery-Aware Thermal Throttling (R-ID 83)**: The `SensingEngine` MUST enforce multi-stage resource constraints. Sensing intervals MUST be capped at 1Hz during thermal warning (>40°C) or low battery (<20%), and 0.1Hz during critical thermal (>45°C) or critical battery (<15%). These constraints MUST override velocity-based performance modes to prevent device shutdown and ensure forensic continuity. (Nov1.4 - ISSUE-REVOL35.0-F1).
*   **1.315 Multi-Project Test Infrastructure (R-ID 84)**: All cross-module telemetry audits (e.g., Backfill Contention) MUST utilize standardized mocks and dependency overrides to ensure consistent behavior across `:app-monitor` and `:app-sensor`. Mock implementations MUST support all `SignalingClient` optimizations including batching and backfill range tracking. (Nov1.5 - ISSUE-REVOL35.0-F2).
*   **1.316 Forensic Heartbeat & Watchdog (R-ID 85)**: The `SensorService` MUST implement a dedicated watchdog to monitor the health of high-frequency sensing loops. The watchdog MUST utilize monotonic heartbeat timestamps from the `TickOrchestrator` and trigger an automated, safe restart of the loop if a stall exceeding 3x the intended interval (with a 10s floor) is detected. (Nov1.5 - ISSUE-REVOL36.0-F1).
*   **1.317 Forensic Silence & API 34 Permission Hardening (R-ID 86)**: The `SensorService` MUST defer initialization of all sensitive hardware and network components until after runtime permissions are explicitly verified. To pass high-frequency forensic audits on target hardware (A15), redundant Framework-level connectivity callbacks MUST be suppressed, relying instead on higher-level protocol heartbeats to ensure zero `StackLog` signatures in Logcat. (Nov1.7 - ISSUE-REVOL37.1-F1, ISSUE-REVOL37.1-F2).

## 🛡️ Core Hardening Baseline
- **Nov1.7**: Forensic Audit Remediation (API 34) - PASSED. Resolved fatal permission regressions and silenced forensic signature leaks.
- **Nov1.6**: Hilt Test Infrastructure Hardening - PASSED. Resolved duplicate Dagger bindings in multi-module test environments.
- **Nov1.5**: Forensic Heartbeat & Watchdog - PASSED. Implemented automated loop recovery and resolved cross-module test infrastructure.
- **Nov1.4**: Battery-Aware Thermal Throttling - PASSED. Implemented multi-stage interval capping with active broadcast receivers and thermal listeners.
- **Nov1.3**: Adaptive Telemetry Sampling - PASSED. Implemented velocity-based 10Hz/1Hz/0.1Hz sampling with TickOrchestrator preemption.
- **Nov1.2**: Forensic Signaling Batching - PASSED. Implemented adaptive 500ms/10-item batching in SocketioSignalingClient with critical event protection.
- **Nov1.1**: Backfill Recovery Contention Audit - PASSED. Implemented range-based deduplication in MonitorRepository.
- **Nov1.0**: Foreground Service Permission Remediation - PASSED. Verified API 34 security guards and FGS type alignment.

## ✅ Verification Chapters
*   **Chapter 5.19 (Nov1.7 Operational Forensic Audit)**: PASSED - Verified zero Davey events > 500ms and zero StackLog leaks on target hardware (A15).
*   **Chapter 5.18 (Nov1.6 Hilt Infrastructure)**: PASSED - Verified resolution of Dagger/Hilt duplicate bindings in BackfillContentionTest.
*   **Chapter 5.17 (Nov1.5 Watchdog Audit)**: PASSED - Verified loop stall detection and heartbeat freshness in WatchdogRecoveryTest.
*   **Chapter 5.16 (Nov1.5 Backfill Infrastructure)**: PASSED - Verified Hilt injection and batch-aware mock signaling in BackfillContentionTest.
*   **Chapter 5.15 (Nov1.4 Throttling Audit)**: PASSED - Verified multi-stage priority logic (Thermal > Battery > Velocity) in ThermalThrottlingTest.
*   **Chapter 5.14 (Nov1.3 Sampling Audit)**: PASSED - Verified velocity-based interval transitions and loop preemption in AdaptiveSamplingTest.
*   **Chapter 5.13 (Nov1.2 Batching Audit)**: PASSED - Verified batching efficiency, timer-based flushing, and critical event priority in SignalingBatchTest.
*   **Chapter 5.12 (Nov1.1 Backfill Audit)**: PASSED - Verified range-based deduplication and zero duplicate ingestion in BackfillContentionTest.
*   **Chapter 5.11 (Nov1.0 FGS Audit)**: PASSED - Verified SecurityException prevention and FGS type compliance on API 34 in Nov1.0.
