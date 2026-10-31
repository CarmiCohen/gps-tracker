# SOT Master Requirements & Hardening Status (Revol31.2)

## 🏗️ Architectural Master Rules (382 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.303 as per Revol30.6 baseline)
*   **1.304 Forensic UI Jitter Audit (R-ID 771)**: The `MonitorRepository` MUST utilize `OnConflictStrategy.REPLACE` and standardized sequence ID ordering to ensure smooth trail rendering during high-frequency backfill ingestion, preventing visual "jumping" or duplicate polyline segments. (Revol31.0 - ISSUE-REVOL30.0-F7).
*   **1.305 Forensic Buffer Spill Audit (R-ID 773)**: The `ForensicBuffer` MUST implement proactive disk space verification and robust I/O recovery to prevent telemetry loss during storage-full conditions. Rollover integrity MUST be maintained under sustained 50Hz burst pressure. (Revol31.1 - ISSUE-REVOL30.0-F8).
*   **1.306 Signaling Resilience Hardening (R-ID 772)**: The `SocketioSignalingClient` MUST employ an aggressive reconnection strategy (`Int.MAX_VALUE` attempts) and an exhaustive queue flushing mechanism that guarantees all queued telemetry is transmitted immediately upon reconnection, ensuring zero data loss during LTE/WiFi flapping. (Revol31.2 - ISSUE-REVOL30.1-F2).
*   **1.307 Socket Testability (R-ID 774)**: The signaling infrastructure MUST utilize a `SocketFactory` abstraction and injectable `IoDispatcher` to allow deterministic unit testing of connection state logic and telemetry flushing without reliance on live network interfaces. (Revol31.2 - ISSUE-REVOL30.1-F2).

## 🛡️ Core Hardening Baseline
- **Revol31.2**: Signaling Resilience Hardening - PASSED. Verified zero loss during 10-cycle connection flapping.
- **Revol31.1**: Forensic Buffer Spill Audit - PASSED. Verified rollover integrity.
- **Revol31.0**: Forensic UI Jitter Audit - PASSED. Verified smooth trail rendering.
- **Revol30.6**: Backfill Duplicate Prevention - PASSED. Verified unique constraint on sequenceId.
- **SOT ID 877**: Forensic Buffer Recovery - PASSED. (Revol30.3).
- **SOT ID 876**: Multi-Peer Signaling Resilience - PASSED. (Revol30.3).
- **SOT ID 875**: E2E Telemetry Pairing - PASSED. (Revol30.1).
- **SOT ID 874**: Atomic Header Sync - PASSED. (Revol29.4).
- **SOT ID 873**: Salted Identity Persistence - PASSED. (Revol29.4).
- **SOT ID 872**: Network Handover Watchdog - PASSED. (Revol29.2).
- **SOT ID 871**: Thermal/Power Scaling - PASSED. (Revol29.2).
- **SOT ID 870**: Orientation-Independent Sensing - PASSED. (Revol29.2).
- **SOT ID 869**: Startup Hydration Deferral - PASSED. (Oct27.10).

## ✅ Verification Chapters
*   **Chapter 5.08 (Revol31.2 Signaling Audit)**: PASSED - Verified aggressive reconnection and exhaustive flush logic in Revol31.2.
*   **Chapter 5.07 (Revol30.6 Integrity Audit)**: PASSED - Verified backfill merging and duplicate prevention.
*   **Chapter 5.06 (Revol30.3 Recovery Audit)**: PASSED - Verified Forensic Buffer recovery logic.
*   **Chapter 5.05 (Revol30.3 Signaling Audit)**: PASSED - Verified multi-peer signaling resilience.
*   **Chapter 5.04 (Revol30.1 Identity & E2E Audit)**: PASSED - Verified unified identity management and API 34 FGS compliance.
*   **Chapter 4.39 (Startup Hydration Audit)**: PASSED - Verified Davey < 500ms and zero StackLog leaks.
