# SOT Master Requirements & Hardening Status (Oct11.2)

## 🏗️ Architectural Master Rules (188 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.155 Forensic Sampling Decay (R-ID 624)**: Forensic sampling MUST decay to `FORENSIC_SAMPLING_INTERVAL_COOLING_MS` (250ms) during both thermal cooling and Mali driver anomalies to protect the JNI bridge from contention. (Oct11.1 - Issue #SIMP-1011-8).
*   **1.156 Authoritative Thermal Metadata (R-ID 625)**: All forensic traces MUST authoritatively capture the device cooling state (`coolingSnapshot`) to enable post-mortem verification of sampling decay during high-temp transients. (Oct11.1 - Issue #SIMP-1011-7).
*   **1.157 Hysteresis Visibility (R-ID 626)**: Suppression of telemetry jitter via hysteresis MUST be captured in forensic logs as "gate hits" to provide visibility into budget hardware stability without triggering reactive system flushes. (Oct11.1 - Issue #SIMP-1011-9).
*   **1.158 Acoustic Baseline Parity (R-ID 627)**: Authoritative acoustic spike detection (Native and Fallback) MUST implement temporal contraction (`ACOUSTIC_FLOOR_CONTRACTION_EMA`) to ensure the baseline correctly decays during duty-cycle "off" periods. (Oct11.2 - Issue #SIMP-1011-10).
*   **1.159 JNI Critical Safety (R-ID 628)**: Usage of `GetPrimitiveArrayCritical` MUST be preceded by array length verification and MUST NOT contain any nested JNI calls to ensure thread safety during driver-level stalls. (Oct11.2 - Issue #SIMP-1011-11).
*   **1.160 Adaptive HUD Sampling (R-ID 629)**: Compose HUD telemetry flows MUST dynamically transition to `HUD_STATE_SAMPLE_THROTTLED_MS` (1000ms) during `isMaliAnomaly` states to mitigate rendering pressure on budget hardware. (Oct11.2 - Issue #SIMP-1011-12).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 676**: Adaptive HUD Sampling - Implemented dynamic UI flow dampening (1Hz) during Mali anomalies. (Oct11.2 - Issue #SIMP-1011-12).
*   **SOT ID 675**: JNI Critical Hardening - Added bounds checks and validated zero-JNI loops for acoustic processing. (Oct11.2 - Issue #SIMP-1011-11).
*   **SOT ID 674**: Acoustic Baseline Parity - Integrated temporal contraction into authoritative JNI and Kotlin baseline evaluators. (Oct11.2 - Issue #SIMP-1011-10).
*   **SOT ID 673**: Authoritative Thermal Metadata - Integrated `coolingSnapshot` into optimized JNI traces and Room persistence. (Oct11.1 - Issue #SIMP-1011-7).
*   **SOT ID 672**: Adaptive I/O Back-off - Integrated `isMaliAnomaly` into persistence gating and proactive pruning to mitigate driver-level contention. (Oct11.1 - Issue #SIMP-1011-8).
*   **SOT ID 671**: Hysteresis Forensic Audit - Implemented logging of hysteresis gate hits to verify jitter suppression on budget hardware. (Oct11.1 - Issue #SIMP-1011-9).
...

---

## 🏁 Verification Chapters
*   **Chapter 31.755 (HUD Throttling Audit)**: PASSED - Verified telemetry flows transition to 1000ms sampling during Mali anomaly simulation. (Oct11.2 - Issue #SIMP-1011-12).
*   **Chapter 31.754 (JNI Safety Audit)**: PASSED - Verified zero JNI calls in `n22` critical sections and added bounds validation. (Oct11.2 - Issue #SIMP-1011-11).
*   **Chapter 31.753 (Acoustic Floor Recovery)**: PASSED - Verified temporal baseline contraction in both Native and Kotlin paths. (Oct11.2 - Issue #SIMP-1011-10).
*   **Chapter 31.745 (Field Stability Audit)**: PASSED - Verified hysteresis gate hits logging in `IntegrityMonitor` for jitter audit. (Oct11.1 - Issue #SIMP-1011-9).
...
