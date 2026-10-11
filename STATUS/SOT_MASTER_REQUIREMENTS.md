# SOT Master Requirements & Hardening Status (Oct11.5)

## 🏗️ Architectural Master Rules (194 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.159 JNI Critical Safety (R-ID 628)**: Usage of `GetPrimitiveArrayCritical` MUST be preceded by array length verification and MUST NOT contain any nested JNI calls to ensure thread safety during driver-level stalls. (Oct11.2 - Issue #SIMP-1011-11).
*   **1.160 Adaptive HUD Sampling (R-ID 629)**: Compose HUD telemetry flows MUST dynamically transition to `HUD_STATE_SAMPLE_THROTTLED_MS` (1000ms) during `isMaliAnomaly` states to mitigate rendering pressure on budget hardware. (Oct11.2 - Issue #SIMP-1011-12).
*   **1.161 Bayesian Drift Authority (R-ID 630)**: The engine MUST authoritatively expand the accuracy radius (`maxAccuracy`) during GNSS gaps at a rate determined by `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` (15.0 m/s) to prevent geofence bypass during stalls. (Oct11.3 - Issue #SIMP-1011-13).
*   **1.162 Authoritative Stationary Duration (R-ID 631)**: All vibration processing paths (Native and Kotlin Fallback) MUST authoritatively accumulate `stationaryDurationMs` to ensure parity in power-save relaxation transitions. (Oct11.3 - Issue #SIMP-1011-14).
*   **1.163 Inline Forensic Iteration (R-ID 632)**: Iteration over high-frequency sensor buffers MUST use `inline` matching functions (e.g., `forEachMatch`) to ensure zero-allocation parity and prevent GC pressure during forensic captures. (Oct11.3 - Issue #SIMP-1011-15).
*   **1.164 Staged Forensic Back-pressure (R-ID 633)**: Forensic sampling MUST implement multi-stage decay based on granular spill-buffer fill levels (50% -> 250ms, 80% -> 500ms, 95% -> 1000ms) to protect against OOM during sustained database I/O contention. (Oct11.3 - Issue #SIMP-1011-16).
*   **1.165 Mali Contention Gate (R-ID 634)**: Mali driver anomaly detection MUST use `LatencyMonitor.consumeMaxJniLatency` and MUST require sustained bridge contention (>150ms) for at least `MALI_ANOMALY_SUSTAINED_THRESHOLD_MS` (30s) to suppress false positives from transient I/O spikes. (Oct11.4 - Issue #SIMP-1011-18).
*   **1.166 Event Bus Staged Gating (R-ID 635)**: The `DomainEventBus` MUST implement staged, priority-aware gating (LOW at 1x threshold, NORMAL at 2x) during high-subscription pressure to ensure CRITICAL and HIGH priority telemetry are protected from eviction in the fixed-size buffer. (Oct11.5 - Issue #SIMP-1011-17).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 682**: Event Bus Staged Gating - Implemented priority-aware dropping and increased capacity to 1024. (Oct11.5 - Issue #SIMP-1011-17).
*   **SOT ID 681**: Mali Contention Gate - Refined anomaly detection to require 30s sustained contention gate. (Oct11.4 - Issue #SIMP-1011-18).
*   **SOT ID 680**: Staged Forensic Back-pressure - Implemented granular sampling decay based on spill-buffer fill level. (Oct11.3 - Issue #SIMP-1011-16).
*   **SOT ID 679**: Bayesian Drift Authority - Integrated authoritative uncertainty growth in MonitorService tick loop. (Oct11.3 - Issue #SIMP-1011-13).
*   **SOT ID 678**: Authoritative Stationary Duration - Fixed duration accumulation in HardwareSuite Kotlin fallback. (Oct11.3 - Issue #SIMP-1011-14).
*   **SOT ID 677**: Inline Forensic Iteration - Audited and verified zero-allocation safety in CircularStateBuffer. (Oct11.3 - Issue #SIMP-1011-15).
...

---

## 🏁 Verification Chapters
*   **Chapter 31.765 (Event Bus Backpressure Audit)**: PASSED - Verified staged priority dropping and buffer expansion to 1024. (Oct11.5 - Issue #SIMP-1011-17).
*   **Chapter 31.764 (Mali Correlation Audit)**: PASSED - Verified 30s sustained contention gate for Mali anomaly triggering. (Oct11.4 - Issue #SIMP-1011-18).
*   **Chapter 31.763 (Forensic Back-pressure Audit)**: PASSED - Verified multi-stage sampling decay transitions based on granular fill level. (Oct11.3 - Issue #SIMP-1011-16).
*   **Chapter 31.762 (Bayesian Drift Audit)**: PASSED - Verified authoritative maxAccuracy expansion during simulated GNSS gaps. (Oct11.3 - Issue #SIMP-1011-13).
*   **Chapter 31.761 (Stationary Parity Audit)**: PASSED - Verified accumulation of stationaryDurationMs in Kotlin fallback path. (Oct11.3 - Issue #SIMP-1011-14).
*   **Chapter 31.760 (Buffer Forensic Audit)**: PASSED - Verified zero-allocation iteration safety in CircularStateBuffer. (Oct11.3 - Issue #SIMP-1011-15).
...
