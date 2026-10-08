# SOT Master Requirements & Hardening Status (Oct8.3)

## 🏗️ Architectural Master Rules (171 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.142 Unified Health Evaluation (R-ID 680)**: Health status evaluation for both GNSS (Signal Loss, Gaps, Stalls) and behavioral anomalies (Jamming, Acoustic Violations) MUST be centralized in the `SentinelValidator`. Behavioral rejections identified during coordinate processing MUST be promoted into the unified `LocationPendingReason` to ensure consistent reporting and signaling priority across the telemetry pipeline. (Oct8.2 - Issue #SIMP-1007-17).
*   **1.143 Native GNSS Health Batching (R-ID 681)**: GNSS status metrics including satellite count, used satellites, and average SNR calculation MUST be offloaded to JNI via `GnssHealthBatch` to minimize JVM overhead and ensure deterministic hardware state evaluation in high-load scenarios. (Oct8.3 - Issue #SIMP-1011-1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 659**: Native GNSS Health Batching - Migrated satellite status evaluation and SNR averaging to JNI to further decouple JVM from hardware state evaluation. (Oct8.3 - Issue #SIMP-1011-1).
*   **SOT ID 658**: Unified Health Evaluation - Consolidated redundant GNSS and behavioral health evaluation into `SentinelValidator`. Instrumented the processor and monitor service to promote behavioral rejections into the unified `LocationPendingReason`. (Oct8.2 - Issue #SIMP-1007-17).
*   **SOT ID 657**: SNR Decay Modeling - Implemented native SNR-Vibration correlation to distinguish between mechanical interference and electronic jamming. (Oct7.10 - Issue #SIMP-1010-3).

---

## 🏁 Verification Chapters
*   **Chapter 31.274 (Native GNSS Health Audit)**: PASSED - Verified that `HardwareSuite` offloads GNSS status changes to JNI via `GnssHealthBatch`. Confirmed that `satellitesInView`, `satellitesUsed`, and `averageSnr` are correctly calculated natively with manual fallback. (Oct8.3 - Issue #SIMP-1011-1).
*   **Chapter 31.273 (Unified Health Audit)**: PASSED - Verified that `HardwareSuite` delegates GNSS health evaluation to `SentinelValidator`. Verified that `LocationProcessor` and `MonitorService` promote behavioral rejections from the sentinel result into the `LocationPendingReason`, ensuring priority-based resolution in the telemetry aggregation layer. (Oct8.2 - Issue #SIMP-1007-17).
