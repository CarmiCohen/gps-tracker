# SOT Master Requirements & Hardening Status (Oct8.4)

## 🏗️ Architectural Master Rules (172 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.142 Unified Health Evaluation (R-ID 680)**: Health status evaluation for both GNSS (Signal Loss, Gaps, Stalls) and behavioral anomalies (Jamming, Acoustic Violations) MUST be centralized in the `SentinelValidator`. Behavioral rejections identified during coordinate processing MUST be promoted into the unified `LocationPendingReason` to ensure consistent reporting and signaling priority across the telemetry pipeline. (Oct8.2 - Issue #SIMP-1007-17).
*   **1.143 Native GNSS Health Batching (R-ID 681)**: GNSS status metrics including satellite count, used satellites, and average SNR calculation MUST be offloaded to JNI via `GnssHealthBatch` to minimize JVM overhead and ensure deterministic hardware state evaluation in high-load scenarios. (Oct8.3 - Issue #SIMP-1011-1).
*   **1.144 Native Acoustic Buffer Processing (R-ID 682)**: High-frequency audio buffer processing, including RMS/Peak calculation and spike evaluation, MUST be offloaded to JNI via `AcousticBatch` to reduce JVM interrupts and mathematical overhead during acoustic monitoring. (Oct8.4 - Issue #SIMP-1011-2).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 660**: Native Acoustic Buffer Processing - Migrated `AudioRecord` RMS/Peak evaluation and spike detection to JNI to minimize JVM overhead in the 44.1kHz path. (Oct8.4 - Issue #SIMP-1011-2).
*   **SOT ID 659**: Native GNSS Health Batching - Migrated satellite status evaluation and SNR averaging to JNI to further decouple JVM from hardware state evaluation. (Oct8.3 - Issue #SIMP-1011-1).
*   **SOT ID 658**: Unified Health Evaluation - Consolidated redundant GNSS and behavioral health evaluation into `SentinelValidator`. Instrumented the processor and monitor service to promote behavioral rejections into the unified `LocationPendingReason`. (Oct8.2 - Issue #SIMP-1007-17).

---

## Verification Chapters
*   **Chapter 31.275 (Native Acoustic Audit)**: PASSED - Verified that `HardwareSuite` offloads audio buffer processing to JNI via `AcousticBatch`. Confirmed that DB levels and spike evaluations are correctly calculated natively with manual fallback. (Oct8.4 - Issue #SIMP-1011-2).
*   **Chapter 31.274 (Native GNSS Health Audit)**: PASSED - Verified that `HardwareSuite` offloads GNSS status changes to JNI via `GnssHealthBatch`. Confirmed that `satellitesInView`, `satellitesUsed`, and `averageSnr` are correctly calculated natively with manual fallback. (Oct8.3 - Issue #SIMP-1011-1).
