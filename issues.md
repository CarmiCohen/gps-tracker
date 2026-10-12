# Project Issues & Hardening Tracking (Rigorous Audit) - Oct12.12

## 🎯 Current Resumption Focus: Activity Context Heuristics & JNI Pressure Consolidation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (0)
*   *All high priority issues resolved.*

### 🟡 Medium Priority (0)
*   *All medium priority issues resolved.*

---

## 💡 Strategic Simplification Ideas (Ideas: 11)
*   **SIMP-OCT12.12-S1: Pruning Deadlock Guard (Medium).** Unify the pruning triggers between `LogRepository` and `OfflineRepository` into a shared `PersistenceWatchdog` to prevent SQLite lock contention.
*   **SIMP-OCT12.12-S2: Checksum Failure Alerting (Low).** Implement a passive UI indicator for forensic corruption events to notify users of SD card failure.
*   **SIMP-OCT12.1-S1: JNI Batching Expansion (Medium).** Batch multiple accelerometer samples into a single JNI call (primitive array) to further reduce JNI crossing tax.
*   **SIMP-OCT12.1-S2: Sub-sampling Hierarchy (Low).** Centralize sensor sub-sampling logic into a dedicated `HardwareLoadBalancer`.
*   **SIMP-OCT11.9-S1: Jitter Buffer Depth Monitoring (Low).** Add telemetry for jitter buffer depth to `SignalingPipeline.Metrics`.
*   **SIMP-OCT11.8-S1: Performance Tier Factory (Medium).** Create a factory for `LocationProcessor` states that automatically populates `performanceTier`.
*   **SIMP-OCT11.7-S1: GPS Freshness Buffer Logic (Low).** Move the dynamic staleness calculation from `MainViewModel` to `GpsStatusManager`.
*   **SIMP-OCT11.6-S1: GNSS Keep-Alive Dynamic Scaling (Low).** Scale the keep-alive interval based on battery level and thermal headroom.
*   **SIMP-OCT11.3-S1: Bayesian Constant Unification (Medium).** Make uncertainty growth rates hardware-aware.
*   **SIMP-OCT11.2-S1: Acoustic Math Consolidation (Medium).** Move temporal decay math to shared C++ header.
*   **SIMP-OCT11.2-S2: Sampling Policy Monolith (Low).** Move sampling interval decision to a `UiPolicyEngine`.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1012-4: Batch Persistence.** Implemented background batch-flush loop in `OfflineRepository.kt` with a 10s interval/5-entry threshold to reduce SQLite write-lock duration. Added `insertAll` to `PendingStatusDao`. Resolved Oct12.12.
*   **Issue #SIMP-1012-3: Forensic Integrity.** Integrated CRC32 checksums into `ForensicSpillBuffer.kt` and `LogEntity` to detect storage-level corruption. Incremented DB version to 83. Resolved Oct12.12.
*   **Issue #SIMP-1012-1: Forensic Buffer Optimization & Memory Pressure Hardening.** Implemented optional JNI auditing and vibration sub-sampling in `HardwareSuite`. Resolved Oct12.1.
*   **Issue #SIMP-1011-23: Adaptive Signaling & Latency Compensation.** Implemented RTT-aware conflation scaling and arrival-time jitter correction. Resolved Oct11.9.
*   **Issue #SIMP-1011-22: Performance Tier Alignment (JNI & Bayesian).** Implemented tiered JNI bridge thresholds and hardware-aware Bayesian expansion rates. Resolved Oct11.8.
*   **Issue #SIMP-1107: GPS Acquisition Regression.** Implemented dynamic gap thresholding and revival loop guards. Resolved Oct11.7.

---

## 📊 Hardening Progress Dashboard
- **Oct12.12: [SOT Count: 357 (Rules: 207), Open: H:0, M:0, L:0, Ideas: H:0, M:6, L:5, Testing: 87 (Sub-items: 435), QA: 902]**
- **Oct12.1: [SOT Count: 355 (Rules: 205), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:4, Testing: 86 (Sub-items: 430), QA: 887]**
- **Oct11.9: [SOT Count: 353 (Rules: 203), Open: H:0, M:0, L:0, Ideas: H:0, M:4, L:3, Testing: 85 (Sub-items: 425), QA: 872]**
- **Oct11.8: [SOT Count: 351 (Rules: 201), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:3, Testing: 84 (Sub-items: 420), QA: 857]**
