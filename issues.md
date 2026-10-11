# Project Issues & Hardening Tracking (Rigorous Audit) - Oct11.8

## 🎯 Current Resumption Focus: Adaptive Signaling & Latency Compensation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (0)
*   *All high priority issues resolved.*

### 🟡 Medium Priority (0)
*   *All medium priority issues resolved.*

---

## 💡 Strategic Simplification Ideas (Ideas: 6)
*   **SIMP-OCT11.8-S1: Performance Tier Factory (Medium).** Create a factory for `LocationProcessor` states that automatically populates `performanceTier` from a single hardware authority to reduce propagation boilerplate in `MonitorService`.
*   **SIMP-OCT11.7-S1: GPS Freshness Buffer Logic (Low).** Move the dynamic staleness calculation from `MainViewModel` to `GpsStatusManager` or a shared engine policy to centralize failure detection authority.
*   **SIMP-OCT11.6-S1: GNSS Keep-Alive Dynamic Scaling (Low).** Scale the keep-alive interval (60s) based on battery level and system thermal headroom to optimize power consumption on budget devices.
*   **SIMP-OCT11.3-S1: Bayesian Constant Unification (Medium).** `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` is currently a constant. Making it hardware-aware could further refine geofence reliability. (Partially addressed in Oct11.8 via state-driven tiering).
*   **SIMP-OCT11.2-S1: Acoustic Math Consolidation (Medium).** Move temporal decay math to shared C++ header to reduce drift risks.
*   **SIMP-OCT11.2-S2: Sampling Policy Monolith (Low).** Move sampling interval decision to a `UiPolicyEngine`.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1011-22: Performance Tier Alignment (JNI & Bayesian).** Implemented tiered JNI bridge thresholds (Standard: 150ms, Staggered: 250ms) and hardware-aware Bayesian expansion rates (Standard: 15m/s, Staggered: 25m/s) to reduce false Mali anomalies and stabilize geofencing on budget hardware. Resolved Oct11.8.
*   **Issue #SIMP-1107: GPS Acquisition Regression.** Implemented dynamic gap thresholding and revival loop guards to prevent failure detection from fighting power-save polling intervals (60s/300s). Harmonized UI staleness logic. Resolved Oct11.7.
*   **Issue #SIMP-1011-21: Radio-Warm Keep-Alive.** Implemented periodic (60s) low-power GNSS bursts in `HardwareSuite.kt` for Staggered devices to maintain radio residency and prevent hardware sleep during background stalls. Resolved Oct11.6.
*   **Issue #SIMP-1011-20: Background Transition Hardening.** Implemented 60s priority escalation window in `DeviceHardeningStrategy.kt`. Resolved Oct11.5.
*   **Issue #SIMP-1011-19: Signaling Role Alignment.** Refined `SignalingValidator.kt` to suppress reflected pulses (echoes). Resolved Oct11.5.
*   **Issue #SIMP-1011-17: Event Bus Backpressure.** Increased buffer capacity to 1024 and implemented staged priority-aware gating. Resolved Oct11.5.

---

## 📊 Hardening Progress Dashboard
- **Oct11.8: [SOT Count: 351 (Rules: 201), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:3, Testing: 84 (Sub-items: 420), QA: 857]**
- **Oct11.7: [SOT Count: 350 (Rules: 199), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:3, Testing: 83 (Sub-items: 415), QA: 842]**
- **Oct11.6: [SOT Count: 348 (Rules: 197), Open: H:1, M:0, L:0, Ideas: H:0, M:2, L:2, Testing: 82 (Sub-items: 410), QA: 828]**
- **Oct11.5: [SOT Count: 347 (Rules: 196), Open: H:1, M:0, L:0, Ideas: H:0, M:2, L:1, Testing: 81 (Sub-items: 405), QA: 812]**
