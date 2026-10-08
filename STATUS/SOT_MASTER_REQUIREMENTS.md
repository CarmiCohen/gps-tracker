# SOT Master Requirements & Hardening Status (Oct8.9)

## 🏗️ Architectural Master Rules (175 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.142 Unified Health Evaluation (R-ID 680)**: Health status evaluation for both GNSS (Signal Loss, Gaps, Stalls) and behavioral anomalies (Jamming, Acoustic Violations) MUST be centralized in the `SentinelValidator`. Behavioral rejections identified during coordinate processing MUST be promoted into the unified `LocationPendingReason` to ensure consistent reporting and signaling priority across the telemetry pipeline. (Oct8.2 - Issue #SIMP-1007-17).
*   **1.143 Native GNSS Health Batching (R-ID 681)**: GNSS status metrics including satellite count, used satellites, and average SNR calculation MUST be offloaded to JNI via `GnssHealthBatch` to minimize JVM overhead and ensure deterministic hardware state evaluation in high-load scenarios. (Oct8.3 - Issue #SIMP-1011-1).
*   **1.144 Native Acoustic Buffer Processing (R-ID 682)**: High-frequency audio buffer processing, including RMS/Peak calculation and spike evaluation, MUST be offloaded to JNI via `AcousticBatch` to reduce JVM interrupts and mathematical overhead during acoustic monitoring. (Oct8.4 - Issue #SIMP-1011-2).
*   **1.145 Native Proximity Health Authority (R-ID 683)**: Proximity state evaluation, including environment-aware debouncing and health index calculation, MUST be offloaded to JNI via `ProximityBatch`. The native implementation MUST handle stationary state transitions and display flickering filters to minimize JVM wake-ups and math overhead. (Oct8.8 - Issue #SIMP-1012-2).
*   **1.146 Zero-Allocation Forensic Retrieval (R-ID 684)**: High-frequency forensic telemetry retrieval MUST utilize `inline` callback-based iteration (e.g., `forEachMatch`) instead of `Sequence` or `Iterator` patterns to achieve zero-allocation parity and prevent GC-induced jitter during background stability audits. (Oct8.8 - Issue #SIMP-1012-1).
*   **1.147 SNR-Based Jamming Discrimination (R-ID 685)**: Jammer suspicion MUST be refined using SNR-based forensic stability audits. The system MUST distinguish between active jamming (sustained low SNR across multiple satellites) and signal blockage (complete loss of samples or residual high SNR) by analyzing forensic SNR trails during recovery phases to ensure high-assurance diagnostic reporting. (Oct8.9 - Issue #SIMP-1012-3).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 663**: Forensic Stability Audit - Implemented SNR-based jamming discrimination in `ForensicAuditor` leveraging zero-allocation retrieval. (Oct8.9 - Issue #SIMP-1012-3).
*   **SOT ID 662**: Native Proximity Scaling - Migrated environment-aware proximity debouncing and index calculation to JNI to further centralize hardware health authority. (Oct8.8 - Issue #SIMP-1012-2).
*   **SOT ID 661**: Forensic Retrieval Optimization - Refactored `CircularStateBuffer` and `HardwareSuite` to use zero-allocation inline iteration for telemetry retrieval. (Oct8.8 - Issue #SIMP-1012-1).

---

## Verification Chapters
*   **Chapter 31.278 (Forensic Jamming Audit)**: PASSED - Verified that `ForensicAuditor` uses `forEachSnrSample` to analyze signal health. Confirmed that "Jammer Suspicion" is correctly promoted based on sustained low SNR vs. complete signal loss. (Oct8.9 - Issue #SIMP-1012-3).
*   **Chapter 31.277 (Native Proximity Audit)**: PASSED - Verified that `HardwareSuite` offloads proximity debouncing to JNI via `ProximityBatch`. Confirmed that stationary duration and thermal load are correctly considered natively. (Oct8.8 - Issue #SIMP-1012-2).
*   **Chapter 31.276 (Zero-Allocation Retrieval Audit)**: PASSED - Verified that `HardwareSuite` get*Samples methods utilize `inline` callbacks. Confirmed via profiling that no `Iterator` or `Sequence` objects are allocated during forensic sampling. (Oct8.8 - Issue #SIMP-1012-1).
