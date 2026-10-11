# Project Issues & Hardening Tracking (Rigorous Audit) - Oct11.6

## 🎯 Current Resumption Focus: Backpressure Logic & Native Parity.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (1)
*   **Issue #AUDIT-1105: GPS Hardware Lock (Samsung/Staggered).** At T+8m background execution, Samsung App Standby forces GNSS into deep sleep, ignoring recovery pulses. Requires radio-warm keep-alive strategy. (Identified Oct11.5).

### 🟡 Medium Priority (0)
*   *All medium priority issues resolved.*

---

## 💡 Strategic Simplification Ideas (Ideas: 4)
*   **SIMP-OCT11.6-S1: GNSS Keep-Alive Dynamic Scaling (Low).** Scale the keep-alive interval (60s) based on battery level and system thermal headroom to optimize power consumption on budget devices.
*   **SIMP-OCT11.3-S1: Bayesian Constant Unification (Medium).** `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` is currently a constant. Making it hardware-aware could further refine geofence reliability.
*   **SIMP-OCT11.2-S1: Acoustic Math Consolidation (Medium).** Move temporal decay math to shared C++ header to reduce drift risks.
*   **SIMP-OCT11.2-S2: Sampling Policy Monolith (Low).** Move sampling interval decision to a `UiPolicyEngine`.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1011-21: Radio-Warm Keep-Alive.** Implemented periodic (60s) low-power GNSS bursts in `HardwareSuite.kt` for Staggered devices to maintain radio residency and prevent hardware sleep during background stalls. Resolved Oct11.6.
*   **Issue #SIMP-1011-20: Background Transition Hardening.** Implemented 60s priority escalation window in `DeviceHardeningStrategy.kt`. Resolved Oct11.5.
*   **Issue #SIMP-1011-19: Signaling Role Alignment.** Refined `SignalingValidator.kt` to suppress reflected pulses (echoes). Resolved Oct11.5.
*   **Issue #SIMP-1011-17: Event Bus Backpressure.** Increased buffer capacity to 1024 and implemented staged priority-aware gating. Resolved Oct11.5.

---

## 📊 Hardening Progress Dashboard
- **Oct11.6: [SOT Count: 348 (Rules: 197), Open: H:1, M:0, L:0, Ideas: H:0, M:2, L:2, Testing: 82 (Sub-items: 410), QA: 828]**
- **Oct11.5: [SOT Count: 347 (Rules: 196), Open: H:1, M:0, L:0, Ideas: H:0, M:2, L:1, Testing: 81 (Sub-items: 405), QA: 812]**
- **Oct11.4: [SOT Count: 344 (Rules: 193), Open: H:0, M:1, L:0, Ideas: H:0, M:2, L:1, Testing: 78 (Sub-items: 390), QA: 780]**
