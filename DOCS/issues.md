# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov2.25)

## 🎯 Next Resumption Focus: Adaptive JNI Buffer Scaling (GAP-REVOL38.3-G1)

---

## 🔴 Open Gaps (Nov2.25)
*   **GAP-REVOL38.3-G1**: JNI Buffer allocation does not account for variable sensor bitrates on different SoC vendors.

---

## 🟢 Resolved Gaps (Nov2.25)
*   **Exhaustive Verification**: Completed 100% pass rate for all automated tests and forensic stress protocols for version Nov2.25.
*   **ISSUE-REVOL38.2-F1 (GAP-REVOL38.2-G1)**: Thermal Throttling Anomaly Correlation (R133). Implemented Thermal Hysteresis (2.0°C) in `SensingEngine` to prevent interval oscillation at boundaries. **PASSED.**
*   **ISSUE-REVOL38.2-F2 (GAP-REVOL38.2-G2)**: Multi-peer backfill contention under high network latency (>2000ms). Validated deduplication logic in `MonitorRepository`. **PASSED.**

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL38.2-S1**: Thermal Status Observer Unification. Move all thermal listening logic to a specialized `ThermalManager` in `core-shared`.
*   **SIMP-REVOL38.2-S2**: Backfill Request Windowing. Implement a windowed approach for very large gaps to reduce memory pressure during backfill.

---

## 📊 Hardening Progress Dashboard
- **Nov2.25: [SOT Count: 898 (Rules: 398), Open: H:0, M:1, L:0, Ideas: H:0, M:4, L:7, Testing: PASSED (Sub-items: 100), QA: 898]**
- **Nov2.24: [SOT Count: 898 (Rules: 398), Open: H:0, M:2, L:0, Ideas: H:0, M:2, L:5, Testing: PASSED (Sub-items: 100), QA: 898]**
- **Nov2.23: [SOT Count: 898 (Rules: 398), Open: H:0, M:2, L:0, Ideas: H:0, M:2, L:5, Testing: PASSED (Sub-items: 100), QA: 898]**
- **Nov2.21: [SOT Count: 898 (Rules: 398), Open: H:0, M:2, L:0, Ideas: H:0, M:2, L:5, Testing: PASSED (Full Suite), QA: 898]**
- **Nov2.20: [SOT Count: 894 (Rules: 394), Open: H:3, M:0, L:0, Ideas: H:0, M:1, L:5, Testing: FAILED, QA: 894]**
- **Nov2.1: [SOT Count: 894 (Rules: 394), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:5, Testing: PASSED (Full Suite), QA: 894]**
