# Project Issues & Hardening Tracking (Rigorous Audit) - Oct8.4

## 🎯 Current Resumption Focus: Strategic Simplification & Redundancy Consolidation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (None)

---

## 💡 Strategic Simplification Ideas (Ideas: 0)
*   *(None)*

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1011-3: Forensic Buffer Consolidation.** Consolidated `EngineSnrSample`, `EngineAcousticSample`, and `EngineSensorSnapshot` into a single `ForensicSample` container. Reduced memory fragmentation and allocation pressure by using a unified circular buffer (`forensicBuffer`) for all high-frequency telemetry data. Resolved Oct8.4.
*   **Issue #SIMP-1011-2: Acoustic JNI Offloading.** Migrated `AudioRecord` iterative math (RMS and Peak) to JNI via `AcousticBatch`. This reduces JVM mathematical overhead and interrupt frequency during 44.1kHz audio monitoring. Resolved Oct8.4.
*   **Issue #SIMP-1011-1: Native GNSS Batching.** Migrated satellite status evaluation and SNR averaging to JNI via `GnssHealthBatch` to minimize JVM overhead and ensure deterministic hardware state evaluation. Resolved Oct8.3.
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.** Instrumented `LocationProcessor` and `MonitorService` to ensure behavioral rejections (Acoustic, Jamming, Tamper) are promoted into the `LocationPendingReason` and propagated through the telemetry pipeline. Resolved Oct8.2.
*   **Issue #SIMP-1007-17: Strategic Simplification.** Consolidated redundant location pending logic between `HardwareSuite` and `SentinelValidator`. Centralized GNSS and behavioral health evaluation in `SentinelValidator`. Resolved Oct7.11.
*   **Issue #SIMP-1010-3: SNR Decay Modeling.** Native correlation of SNR vs Vibration to distinguish jamming from mechanical interference. Resolved Oct7.10.
*   **Issue #SIMP-1010-2: Muzzle Hysteresis Native Offloading.** Migrate remaining `stationaryStartRt` and muzzle logic to JNI to further reduce JVM overhead in the 100Hz path. Resolved Oct7.9.
*   **Issue #SIMP-1010-1: Adaptive Acoustic Gating.** Implement native logic to adjust the `ACOUSTIC_EMA` alpha based on `vibrationRollingSum`. Resolved Oct7.8.

---

## 📊 Hardening Progress Dashboard
- **Oct8.4: [SOT Count: 325 (Rules: 172), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 68 (Sub-items: 340), QA: 631]**
- **Oct8.3: [SOT Count: 323 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 67 (Sub-items: 335), QA: 626]**
- **Oct8.2: [SOT Count: 322 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 66 (Sub-items: 330), QA: 621]**
- **Oct8.1: [SOT Count: 321 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 65 (Sub-items: 325), QA: 616]**
- **Oct7.11: [SOT Count: 319 (Rules: 170), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 64 (Sub-items: 320), QA: 611]**
- **Oct7.10: [SOT Count: 318 (Rules: 169), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 63 (Sub-items: 315), QA: 606]**
- **Oct7.9: [SOT Count: 317 (Rules: 168), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 62 (Sub-items: 310), QA: 601]**
- **Oct7.8: [SOT Count: 316 (Rules: 167), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 61 (Sub-items: 305), QA: 596]**
