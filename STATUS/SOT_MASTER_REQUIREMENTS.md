# SOT Master Requirements & Hardening Status (Oct11.7)

## 🏗️ Architectural Master Rules (199 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.168 Priority Escalation Window (R-ID 637)**: Foreground-to-background transitions MUST be protected by a 60s priority escalation window to stabilize resource allocation on budget hardware. (Oct11.5 - Issue #SIMP-1011-20).
*   **1.169 Radio-Warm Keep-Alive (R-ID 638)**: Staggered tier devices MUST maintain GNSS radio residency during background or stationary periods via periodic (60s) low-power bursts to prevent OS-induced hardware residency locks. (Oct11.6 - Issue #SIMP-1011-21).
*   **1.170 Dynamic Gap Thresholding (R-ID 639)**: GNSS gap detection thresholds MUST scale dynamically based on the current intended polling interval (2x factor) to prevent revival loop interference during power-save periods. (Oct11.7 - Issue #SIMP-1107).
*   **1.171 Harmonized UI Freshness (R-ID 640)**: HUD GPS status indicators MUST utilize mode-aware staleness thresholds (Interval + Grace) to ensure logical consistency with engine-level power relaxation strategies. (Oct11.7 - Issue #SIMP-1107).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 686**: GPS Acquisition Recovery - Implemented dynamic thresholds and revival guards to resolve the stationary "Red LED" regression. (Oct11.7 - Issue #SIMP-1107).
*   **SOT ID 685**: Radio-Warm Keep-Alive - Implemented 60s periodic bursts to prevent GNSS hardware lock. (Oct11.6 - Issue #SIMP-1011-21).
*   **SOT ID 684**: Priority Escalation Window - Implemented 60s window for stable background transitions. (Oct11.5 - Issue #SIMP-1011-20).
*   **SOT ID 683**: Signaling Echo Suppression - Refined validator to eliminate reflected pulse interference. (Oct11.5 - Issue #SIMP-1011-19).

...

---

## 🏁 Verification Chapters
*   **Chapter 31.769 (Acquisition Audit)**: PASSED - Verified LED Green persistence at T+5m stationary polling on SM-A155F. (Oct11.7 - Issue #SIMP-1107).
*   **Chapter 31.768 (Radio-Warm Audit)**: PASSED - Verified radio residency preservation at T+20m on SM-A155F. (Oct11.6 - Issue #SIMP-1011-21).
...
