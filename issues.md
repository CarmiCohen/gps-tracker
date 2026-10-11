# Project Issues & Hardening Tracking (Rigorous Audit) - Oct11.2

## 🎯 Current Resumption Focus: Bayesian Drift & Ultra-Long Stability.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (2)
*   **Issue #SIMP-1011-13: Bayesian Drift Audit.** Verify uncertainty growth scaling during sustained GNSS gaps on low-SNR hardware. Ensure accuracy expands authoritatively to prevent geofence bypass during stalls.
*   **Issue #SIMP-1011-14: Ultra-Long Stability.** Audit `ULTRA_LONG_STATIONARY_DURATION_MS` (4h) transitions to ensure no state leakage and correct polling relaxation during extended stillness.

### 🟡 Medium Priority (1)
*   **Issue #SIMP-1011-15: Circular Buffer Forensic.** Audit zero-allocation iteration safety in `CircularStateBuffer.forEachMatch` to ensure thread-safety during high-frequency sensor floods.

---

## 💡 Strategic Simplification Ideas (Ideas: 2)
*   **SIMP-OCT11.2-S1: Acoustic Math Consolidation (Medium).** The temporal decay math is duplicated between JNI and Kotlin fallback. Moving this to a shared C++ header or a centralized utility in `:core:engine` could reduce drift risks.
*   **SIMP-OCT11.2-S2: Sampling Policy Monolith (Low).** `MainViewModel` now has complex sampling logic. Moving the sampling interval decision (Sample vs Throttled) to a `UiPolicyEngine` would keep the ViewModel thinner.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1011-12: HUD Throttling Sync.** Synced Compose HUD telemetry flows with `isMaliAnomaly` state. Implemented dynamic sampling (1000ms) during driver stalls to reduce rendering and I/O pressure on budget hardware. Resolved Oct11.2.
*   **Issue #SIMP-1011-11: JNI Resource Audit.** Hardened `n22` acoustic batching with explicit array bounds verification and zero-JNI loop enforcement within `GetPrimitiveArrayCritical` blocks to prevent system-level stalls. Resolved Oct11.2.
*   **Issue #SIMP-1011-10: Acoustic Floor Hardening.** Implemented temporal contraction (Leaky Integrator) in `jdhardware-jni.cpp` and `JdHardwareManager.kt` to ensure baseline recovery during duty-cycle "off" periods. Resolved Oct11.2.
*   **Issue #SIMP-1011-9: Field Stability Audit.** Verified that the 30MB hysteresis gate effectively silences "Memory Pressure" jitter on budget hardware. Implemented forensic logging of hysteresis "gate hits" in `IntegrityMonitor.kt` to provide visibility in field logs without triggering reactive flushes. Resolved Oct11.1.
*   **Issue #SIMP-1011-8: I/O Pressure Test.** Integrated `isMaliAnomaly` into persistence gating in `PersistencePolicy.kt` and proactive pruning in `LogRepository.kt`. Throttled forensic sampling to 250ms in `MonitorService.kt` during driver-level stalls to mitigate JNI bridge contention. Resolved Oct11.1.
*   **Issue #SIMP-1011-7: Thermal Forensic Audit.** Hardened forensic metadata to authoritatively audit sampling decay. Added `coolingSnapshot` to `LogEntry` and `LogEntity` (Migration 82). Integrated cooling state into `ForensicSpillBuffer` flags (0x10) and ensured it is captured in optimized native traces. Resolved Oct11.1.
*   **Issue #SIMP-1011-6: Mali Forensic Audit.** Correlated JNI bridge latency stalls (>100ms) with GPU driver anomaly detection in `IntegrityMonitor.kt`. Expanded `LatencyMonitor` to track rolling max JNI latency. Resolved Oct10.11.

---

## 📊 Hardening Progress Dashboard
- **Oct11.2: [SOT Count: 339 (Rules: 188), Open: H:2, M:1, L:0, Ideas: H:0, M:1, L:1, Testing: 74 (Sub-items: 370), QA: 748]**
- **Oct11.1: [SOT Count: 336 (Rules: 185), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 71 (Sub-items: 355), QA: 745]**
- **Oct10.11: [SOT Count: 333 (Rules: 182), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 71 (Sub-items: 355), QA: 742]**
- **Oct10.10: [SOT Count: 329 (Rules: 179), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 69 (Sub-items: 345), QA: 714]**
