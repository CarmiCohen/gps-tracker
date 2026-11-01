# SOT Master Requirements & Hardening Status (Nov1.2)

## 🏗️ Architectural Master Rules (387 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.307 as per Revol31.8 baseline)
*   **1.308 Geofence Transition Forensic Integrity (R-ID 775)**: The `SentinelLogic` detect `ENTER`/`EXIT` transitions with nanosecond precision, and the `ForensicBuffer` MUST persist these events alongside their `monotonicNanos` and `status` strings to ensure a complete, non-repudiable forensic trail of spatial violations. (Revol31.8 - ISSUE-REVOL30.1-F3).
*   **1.309 Signaling Pipeline Soak Integrity (R-ID 776)**: The `SignalingQueue` MUST implement critical event prioritization and support at least 5000 in-memory events to guarantee zero telemetry loss during extended network disconnections at 10Hz. All "ENTER/EXIT" and "JUMP" status events MUST be protected from queue eviction during overflow conditions. (Revol31.9 - ISSUE-REVOL31.9-F1).
*   **1.310 FGS Permission & API 34 Security (R-ID 10)**: All Foreground Services MUST implement strict runtime permission guards before calling `startForeground` to prevent `SecurityException` on API 34+. Specifically, `location` types MUST verify `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` and include appropriate type bits (`LOCATION`, `DATA_SYNC`, `REMOTE_MESSAGING`) to match manifest declarations and operational needs. (Nov1.0 - ISSUE-REVOL31.9-F2).
*   **1.311 Backfill Recovery Contention Audit (R-ID 80)**: The `MonitorRepository` MUST implement range-based request deduplication to prevent redundant backfill requests during concurrent telemetry ingestion. Telemetry ingestion MUST ensure zero duplicate records in the `TrailDao` even during multi-peer contention or overlapping backfill responses. (Nov1.1 - ISSUE-REVOL32.0-F1).
*   **1.312 Forensic Signaling Batching (R-ID 81)**: The `SignalingClient` MUST implement adaptive telemetry batching (time and size based) to optimize packet overhead during high-frequency bursts or recovery. Critical status updates (e.g., Geofence transitions) and Violation events MUST trigger an immediate batch flush to maintain real-time operational responsiveness. (Nov1.2 - ISSUE-REVOL33.0-F1).

## 🛡️ Core Hardening Baseline
- **Nov1.2**: Forensic Signaling Batching - PASSED. Implemented adaptive 500ms/10-item batching in SocketioSignalingClient with critical event protection.
- **Nov1.1**: Backfill Recovery Contention Audit - PASSED. Implemented range-based deduplication in MonitorRepository.
- **Nov1.0**: Foreground Service Permission Remediation - PASSED. Verified API 34 security guards and FGS type alignment.
- **Revol31.9**: Pipeline Stability & Soak Audit - PASSED. Verified 100% telemetry delivery and critical event protection.
- **Revol31.8**: Geofence Transition Forensic Audit - PASSED. Verified zero loss and nanosecond precision.
- **Revol31.2**: Signaling Resilience Hardening - PASSED. Verified zero loss during 10-cycle connection flapping.
- **Revol31.1**: Forensic Buffer Spill Audit - PASSED. Verified rollover integrity.

## ✅ Verification Chapters
*   **Chapter 5.13 (Nov1.2 Batching Audit)**: PASSED - Verified batching efficiency, timer-based flushing, and critical event priority in SignalingBatchTest.
*   **Chapter 5.12 (Nov1.1 Backfill Audit)**: PASSED - Verified range-based deduplication and zero duplicate ingestion in BackfillContentionTest.
*   **Chapter 5.11 (Nov1.0 FGS Audit)**: PASSED - Verified SecurityException prevention and FGS type compliance on API 34 in Nov1.0.
*   **Chapter 5.10 (Revol31.9 Soak Audit)**: PASSED - Verified zero telemetry loss and critical event protection under 2000-event stress tests in Revol31.9.
*   **Chapter 5.09 (Revol31.8 Geofence Audit)**: PASSED - Verified nanosecond transition precision and buffer persistence in Revol31.8.
*   **Chapter 5.08 (Revol31.2 Signaling Audit)**: PASSED - Verified aggressive reconnection and exhaustive flush logic in Revol31.2.
