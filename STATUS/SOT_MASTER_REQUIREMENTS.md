# SOT Master Requirements & Hardening Status (Oct11.8)

## 🏗️ Architectural Master Rules (201 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.168 Priority Escalation Window (R-ID 637)**: Foreground-to-background transitions MUST be protected by a 60s priority escalation window to stabilize resource allocation on budget hardware. (Oct11.5 - Issue #SIMP-1011-20).
*   **1.169 Radio-Warm Keep-Alive (R-ID 638)**: Staggered tier devices MUST maintain GNSS radio residency during background or stationary periods via periodic (60s) low-power bursts to prevent OS-induced hardware residency locks. (Oct11.6 - Issue #SIMP-1011-21).
*   **1.170 Dynamic Gap Thresholding (R-ID 639)**: GNSS gap detection thresholds MUST scale dynamically based on the current intended polling interval (2x factor) to prevent revival loop interference during power-save periods. (Oct11.7 - Issue #SIMP-1107).
*   **1.171 Harmonized UI Freshness (R-ID 640)**: HUD GPS status indicators MUST utilize mode-aware staleness thresholds (Interval + Grace) to ensure logical consistency with engine-level power relaxation strategies. (Oct11.7 - Issue #SIMP-1107).
*   **1.172 Tiered JNI Bridge Thresholds (R-ID 641)**: Mali anomaly detection MUST utilize hardware-aware JNI bridge latency thresholds (Standard: 150ms, Staggered: 250ms) to accommodate higher base jitter on budget SoCs. (Oct11.8 - Issue #SIMP-1011-22).
*   **1.173 Hardware-Aware Bayesian Expansion (R-ID 642)**: Authoritative uncertainty growth (Bayesian expansion) MUST scale its expansion rate threshold (Standard: 15m/s, Staggered: 25m/s) based on the device's hardware performance tier to maintain geofence reliability. (Oct11.8 - Issue #SIMP-1011-22).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 688**: Performance Tier Alignment - Implemented hardware-aware JNI latency and Bayesian expansion thresholds to stabilize budget hardware tracking. (Oct11.8 - Issue #SIMP-1011-22).
*   **SOT ID 687**: GPS Acquisition Recovery - Implemented dynamic thresholds and revival guards to resolve the stationary "Red LED" regression. (Oct11.7 - Issue #SIMP-1107).
*   **SOT ID 686**: Radio-Warm Keep-Alive - Implemented 60s periodic bursts to prevent GNSS hardware lock. (Oct11.6 - Issue #SIMP-1011-21).
*   **SOT ID 685**: Priority Escalation Window - Implemented 60s window for stable background transitions. (Oct11.5 - Issue #SIMP-1011-20).
*   **SOT ID 684**: Signaling Echo Suppression - Refined validator to eliminate reflected pulse interference. (Oct11.5 - Issue #SIMP-1011-19).

...

---

## 🏁 Verification Chapters
*   **Chapter 31.770 (Performance Tier Audit)**: PASSED - Verified JNI threshold relaxation (250ms) suppressed false Mali alerts on SM-A155F under IO load. Verified Bayesian expansion rate scaling at T+60s gap. (Oct11.8 - Issue #SIMP-1011-22).
*   **Chapter 31.769 (Acquisition Audit)**: PASSED - Verified LED Green persistence at T+5m stationary polling on SM-A155F. (Oct11.7 - Issue #SIMP-1107).
*   **Chapter 31.768 (Radio-Warm Audit)**: PASSED - Verified radio residency preservation at T+20m on SM-A155F. (Oct11.6 - Issue #SIMP-1011-21).
...
