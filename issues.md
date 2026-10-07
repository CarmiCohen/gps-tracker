# Project Issues & Hardening Tracking (Rigorous Audit) - Oct7.6

## 🎯 Current Resumption Focus: Native Anomaly Detection & Memory Pressure Correlation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   **Issue #SIMP-1007-16: Native Anomaly Detection.** Implement correlation logic in `jdhardware-jni.cpp` to trigger "Suspicious Environmental Noise" when SNR and vibration levels indicate interference.
*   **Issue #SIMP-1007-17: Native Memory Pressure Throttling.** Integrate native heap usage spikes from `VibrationBatch` into the sensor polling interval decision logic in `SentinelValidator`.

---

## 💡 Strategic Simplification Ideas (Ideas: 1)
*   **ID: SIMP-1007-17 [Low]**: Consolidate redundant location pending logic between HardwareSuite and SentinelValidator now that JNI batching covers state evaluation.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1007-15: Unified Snapshot Container.** Resolved Oct7.5.
*   **Issue #QA-1007-1: Telemetry Forensic Expansion & Radio Soak Validation.** Resolved Oct7.3.
*   **Issue #QA-1006-12: Android 15 (API 35) Deployment & Forensic Audit.** Resolved Oct7.2.
*   **Issue #SIMP-1006-14: Telemetry Field Pruning.** Resolved Oct7.1. 

---

## 📊 Hardening Progress Dashboard
- **Oct7.6: [SOT Count: 313 (Rules: 164), Open: H:2, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 58, QA: 587]**
- **Oct7.5: [SOT Count: 312 (Rules: 163), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 58, QA: 586]**
- **Oct7.4: [SOT Count: 310 (Rules: 162), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 58, QA: 584]**
- **Oct7.3: [SOT Count: 308 (Rules: 162), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 58, QA: 582]**
