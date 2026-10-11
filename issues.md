# Project Issues & Hardening Tracking (Rigorous Audit) - Oct11.3

## 🎯 Current Resumption Focus: Backpressure Logic & Native Parity.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (0)
*   *All high priority issues resolved in Oct11.3.*

### 🟡 Medium Priority (2)
*   **Issue #SIMP-1011-17: Event Bus Backpressure.** Audit `DomainEventBus` drop-oldest behavior during high-frequency sensor floods. Ensure `extraBufferCapacity` (512) and `BufferOverflow.DROP_OLDEST` correctly preserve system stability without losing critical audit telemetry.
*   **Issue #SIMP-1011-18: Mali Correlation Refinement.** Refine Mali driver stall correlation in `IntegrityMonitor.kt` using `LatencyMonitor` max JNI latency. Ensure `isMaliAnomaly` is not triggered by transient DB spikes but by sustained bridge contention.

---

## 💡 Strategic Simplification Ideas (Ideas: 3)
*   **SIMP-OCT11.3-S1: Bayesian Constant Unification (Medium).** `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` is currently a constant. Making it hardware-aware (e.g., higher for high-performance devices, lower for budget) could further refine geofence reliability.
*   **SIMP-OCT11.2-S1: Acoustic Math Consolidation (Medium).** The temporal decay math is duplicated between JNI and Kotlin fallback. Moving this to a shared C++ header or a centralized utility in `:core:engine` could reduce drift risks.
*   **SIMP-OCT11.2-S2: Sampling Policy Monolith (Low).** `MainViewModel` now has complex sampling logic. Moving the sampling interval decision to a `UiPolicyEngine` would keep the ViewModel thinner.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1011-16: Forensic Back-pressure (Staged).** Implemented multi-stage forensic sampling decay in `MonitorService.kt` based on granular fill level (50% -> 250ms, 80% -> 500ms, 95% -> 1000ms). Added `getForensicFillLevel()` to `LogManager.kt`. Resolved Oct11.3.
*   **Issue #SIMP-1011-15: Circular Buffer Forensic.** Audited `CircularStateBuffer.forEachMatch` for zero-allocation iteration safety. Confirmed `inline` usage prevents overhead during sensor floods. Contention risk on sensor thread verified as minimal for buffer sizes < 1024. Resolved Oct11.3.
*   **Issue #SIMP-1011-14: Ultra-Long Stability.** Fixed bug in `HardwareSuite.kt` where `stationaryDurationMs` was incorrectly reset in the Kotlin fallback path. Implemented authoritative accumulation to ensure 4-hour relaxation parity without JNI. Resolved Oct11.3.
*   **Issue #SIMP-1011-13: Bayesian Drift Audit.** Integrated `LocationProcessor.performBayesianExpansion` into `MonitorService.processTick`. Accuracy radius now authoritatively expands at 15.0 m/s during GNSS gaps to prevent geofence bypass. Resolved Oct11.3.
*   **Issue #SIMP-1011-12: HUD Throttling Sync.** Synced Compose HUD telemetry flows with `isMaliAnomaly` state. Implemented dynamic sampling (1000ms) during driver stalls to reduce rendering and I/O pressure on budget hardware. Resolved Oct11.2.
*   **Issue #SIMP-1011-11: JNI Resource Audit.** Hardened `n22` acoustic batching with explicit array bounds verification and zero-JNI loop enforcement within `GetPrimitiveArrayCritical` blocks to prevent system-level stalls. Resolved Oct11.2.
*   **Issue #SIMP-1011-10: Acoustic Floor Hardening.** Implemented temporal contraction (Leaky Integrator) in `jdhardware-jni.cpp` and `JdHardwareManager.kt` to ensure baseline recovery during duty-cycle "off" periods. Resolved Oct11.2.

---

## 📊 Hardening Progress Dashboard
- **Oct11.3: [SOT Count: 343 (Rules: 192), Open: H:0, M:2, L:0, Ideas: H:0, M:2, L:1, Testing: 77 (Sub-items: 385), QA: 764]**
- **Oct11.2: [SOT Count: 339 (Rules: 188), Open: H:2, M:1, L:0, Ideas: H:0, M:1, L:1, Testing: 74 (Sub-items: 370), QA: 748]**
- **Oct11.1: [SOT Count: 336 (Rules: 185), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 71 (Sub-items: 355), QA: 745]**
- **Oct10.11: [SOT Count: 333 (Rules: 182), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 71 (Sub-items: 355), QA: 742]**
- **Oct10.10: [SOT Count: 329 (Rules: 179), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 69 (Sub-items: 345), QA: 714]**
