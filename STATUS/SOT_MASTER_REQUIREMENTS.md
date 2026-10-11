# SOT Master Requirements & Hardening Status (Oct11.6)

## 🏗️ Architectural Master Rules (197 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.159 JNI Critical Safety (R-ID 628)**: Usage of `GetPrimitiveArrayCritical` MUST be preceded by array length verification and MUST NOT contain any nested JNI calls. (Oct11.2 - Issue #SIMP-1011-11).
*   **1.160 Adaptive HUD Sampling (R-ID 629)**: Compose HUD telemetry flows MUST dynamically transition to `HUD_STATE_SAMPLE_THROTTLED_MS` (1000ms) during `isMaliAnomaly` states. (Oct11.2 - Issue #SIMP-1011-12).
*   **1.161 Bayesian Drift Authority (R-ID 630)**: The engine MUST authoritatively expand the accuracy radius during GNSS gaps at 15.0 m/s. (Oct11.3 - Issue #SIMP-1011-13).
*   **1.162 Authoritative Stationary Duration (R-ID 631)**: All vibration processing paths MUST authoritatively accumulate `stationaryDurationMs`. (Oct11.3 - Issue #SIMP-1011-14).
*   **1.163 Inline Forensic Iteration (R-ID 632)**: Iteration over high-frequency sensor buffers MUST use `inline` matching functions. (Oct11.3 - Issue #SIMP-1011-15).
*   **1.164 Staged Forensic Back-pressure (R-ID 633)**: Forensic sampling MUST implement multi-stage decay based on granular spill-buffer fill levels. (Oct11.3 - Issue #SIMP-1011-16).
*   **1.165 Mali Contention Gate (R-ID 634)**: Mali driver anomaly detection MUST require sustained bridge contention (>150ms) for at least 30s. (Oct11.4 - Issue #SIMP-1011-18).
*   **1.166 Event Bus Staged Gating (R-ID 635)**: The `DomainEventBus` MUST implement staged, priority-aware gating during high-subscription pressure. (Oct11.5 - Issue #SIMP-1011-17).
*   **1.167 Signaling Echo Suppression (R-ID 636)**: Signaling validation MUST authoritatively suppress reflected pulses (echoes) during high-gain sensor cycles to prevent logic stalls. (Oct11.5 - Issue #SIMP-1011-19).
*   **1.168 Priority Escalation Window (R-ID 637)**: Foreground-to-background transitions MUST be protected by a 60s priority escalation window to stabilize resource allocation on budget hardware. (Oct11.5 - Issue #SIMP-1011-20).
*   **1.169 Radio-Warm Keep-Alive (R-ID 638)**: Staggered tier devices MUST maintain GNSS radio residency during background or stationary periods via periodic (60s) low-power bursts to prevent OS-induced hardware residency locks. (Oct11.6 - Issue #SIMP-1011-21).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 685**: Radio-Warm Keep-Alive - Implemented 60s periodic bursts to prevent GNSS hardware lock. (Oct11.6 - Issue #SIMP-1011-21).
*   **SOT ID 684**: Priority Escalation Window - Implemented 60s window for stable background transitions. (Oct11.5 - Issue #SIMP-1011-20).
*   **SOT ID 683**: Signaling Echo Suppression - Refined validator to eliminate reflected pulse interference. (Oct11.5 - Issue #SIMP-1011-19).
*   **SOT ID 682**: Event Bus Staged Gating - Implemented priority-aware dropping and increased capacity to 1024. (Oct11.5 - Issue #SIMP-1011-17).

...

---

## 🏁 Verification Chapters
*   **Chapter 31.768 (Radio-Warm Audit)**: PASSED - Verified radio residency preservation at T+20m on SM-A155F. (Oct11.6 - Issue #SIMP-1011-21).
*   **Chapter 31.767 (Background Transition Audit)**: PASSED - Verified 60s escalation window effectiveness on SM-A155F. (Oct11.5 - Issue #SIMP-1011-20).
*   **Chapter 31.766 (Signaling Echo Audit)**: PASSED - Verified 100% suppression accuracy of reflected pulses. (Oct11.5 - Issue #SIMP-1011-19).
...
