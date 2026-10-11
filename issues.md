# Project Issues & Hardening Tracking (Rigorous Audit) - Oct11.5

## 🎯 Current Resumption Focus: Backpressure Logic & Native Parity.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (0)
*   *All high priority issues resolved in Oct11.3.*

### 🟡 Medium Priority (0)
*   *Issue #SIMP-1011-17 resolved in Oct11.5.*

---

## 💡 Strategic Simplification Ideas (Ideas: 3)
*   **SIMP-OCT11.3-S1: Bayesian Constant Unification (Medium).** `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` is currently a constant. Making it hardware-aware (e.g., higher for high-performance devices, lower for budget) could further refine geofence reliability.
*   **SIMP-OCT11.2-S1: Acoustic Math Consolidation (Medium).** The temporal decay math is duplicated between JNI and Kotlin fallback. Moving this to a shared C++ header or a centralized utility in `:core:engine` could reduce drift risks.
*   **SIMP-OCT11.2-S2: Sampling Policy Monolith (Low).** `MainViewModel` now has complex sampling logic. Moving the sampling interval decision to a `UiPolicyEngine` would keep the ViewModel thinner.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1011-17: Event Bus Backpressure.** Audited `DomainEventBus` drop-oldest behavior. Increased buffer capacity to 1024 and implemented staged priority-aware gating (LOW at 1x threshold, NORMAL at 2x) to protect CRITICAL/HIGH telemetry during sensor floods. Resolved Oct11.5.
*   **Issue #SIMP-1011-18: Mali Correlation Refinement.** Refine Mali driver stall correlation in `IntegrityMonitor.kt` using `LatencyMonitor` max JNI latency. Refined anomaly trigger to require sustained bridge contention (>150ms) for 30s to suppress transient jitter. Resolved Oct11.4.
*   **Issue #SIMP-1011-16: Forensic Back-pressure (Staged).** Implemented multi-stage forensic sampling decay in `MonitorService.kt` based on granular fill level (50% -> 250ms, 80% -> 500ms, 95% -> 1000ms). Added `getForensicFillLevel()` to `LogManager.kt`. Resolved Oct11.3.
*   **Issue #SIMP-1011-15: Circular Buffer Forensic.** Audited `CircularStateBuffer.forEachMatch` for zero-allocation iteration safety. Confirmed `inline` usage prevents overhead during sensor floods. Contention risk on sensor thread verified as minimal for buffer sizes < 1024. Resolved Oct11.3.
*   **Issue #SIMP-1011-14: Ultra-Long Stability.** Fixed bug in `HardwareSuite.kt` where `stationaryDurationMs` was incorrectly reset in the Kotlin fallback path. Implemented authoritative accumulation to ensure 4-hour relaxation parity without JNI. Resolved Oct11.3.
*   **Issue #SIMP-1011-13: Bayesian Drift Audit.** Integrated `LocationProcessor.performBayesianExpansion` into `MonitorService.processTick`. Accuracy radius now authoritatively expands at 15.0 m/s during GNSS gaps to prevent geofence bypass. Resolved Oct11.3.
*   **Issue #SIMP-1011-12: HUD Throttling Sync.** Synced Compose HUD telemetry flows with `isMaliAnomaly` state. Implemented dynamic sampling (1000ms) during driver stalls to reduce rendering and I/O pressure on budget hardware. Resolved Oct11.2.
*   **Issue #SIMP-1011-11: JNI Resource Audit.** Hardened `n22` acoustic batching with explicit array bounds verification and zero-JNI loop enforcement within `GetPrimitiveArrayCritical` blocks to prevent system-level stalls. Resolved Oct11.2.
*   **Issue #SIMP-1011-10: Acoustic Floor Hardening.** Implemented temporal contraction (Leaky Integrator) in `jdhardware-jni.cpp` and `JdHardwareManager.kt` to ensure baseline recovery during duty-cycle "off" periods. Resolved Oct11.2.

---

## 📊 Hardening Progress Dashboard
- **Oct11.5: [SOT Count: 345 (Rules: 194), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:1, Testing: 79 (Sub-items: 395), QA: 796]**
- **Oct11.4: [SOT Count: 344 (Rules: 193), Open: H:0, M:1, L:0, Ideas: H:0, M:2, L:1, Testing: 78 (Sub-items: 390), QA: 780]**
- **Oct11.3: [SOT Count: 343 (Rules: 192), Open: H:0, M:2, L:0, Ideas: H:0, M:2, L:1, Testing: 77 (Sub-items: 385), QA: 764]**
- **Oct11.2: [SOT Count: 339 (Rules: 188), Open: H:2, M:1, L:0, Ideas: H:0, M:1, L:1, Testing: 74 (Sub-items: 370), QA: 748]**
- **Oct11.1: [SOT Count: 336 (Rules: 185), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 71 (Sub-items: 355), QA: 745]**
