# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.2.5

## 🎯 Current Resumption Focus: State Persistence & Peer Convergence
Hardening of cross-device acknowledgment state and idempotent trigger evaluation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   (All current high-priority gaps resolved).

---

## 💡 Strategic Simplification Ideas (Ideas: 2)

### 🔵 Low Priority
*   **Issue #1175: Real-time Only Path**
    *   *Significance*: **Strategic**. Consider removing backlog sync and forensic backfilling.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1416-1 / SOT ID 594: Native Sensor Pulse Audit.** Resolved Oct.2.5. Offloaded 250Hz frequency auditing to JNI to eliminate heap churn (R-ID 256).
*   **Issue #1402-B / R-ID 582: WindowManager Lifecycle Hardening.** Resolved Oct.2.3. Implemented full Lifecycle transitions and explicit disposal for AlarmOverlayService (R-ID 582).
*   **Issue #1417: Jitter-Resistant Connectivity Transitions.** Resolved Oct.2.2. Integrated 3s temporal hysteresis for RELAY_OFFLINE and Peer Error suppression (R-ID 593).
*   **Issue #1416: Memory Pressure Mitigation.** Resolved Oct.2.2. Integrated heap-aware throttling for forensic sampling and aggressive GC triggers (R-ID 592).
*   **Issue #1415: CPU-Load Compensation for Sensors.** Resolved Oct.2.2. Integrated load-aware gating (0.85 threshold) for IMU jitter compensation (R-ID 590/591).
*   **Issue #1414: Dashboard UNKNOWN state (Local Tracker).** Resolved Oct.1.8. Fixed local state routing to MainViewModel (R-ID 589).
*   **Issue #1413: Mode-Based Alert Violation (Stealth Regression).** Resolved Oct.1.8. Restricted AlarmOverlay to Viewer Mode only (R-ID 588).
*   **Issue #1410: Engine Thread-Safety (Fatal CME).** Resolved Oct.1.8. Migrated to ConcurrentHashMap and synchronized mutations (R-ID 585).
*   **Issue #1410: Viewer Persistence (Recurring Alarms).** Resolved Oct.1.8. Integrated global lastAlarmAckTs sync (R-ID 579).
*   **Issue #1402-B: System-Wide Alarm Overlay Failure.** Resolved Oct.1.7. Hardened disposal in Oct.1.8 (R-ID 582).

---

## 📊 Hardening Progress Dashboard
- **Oct.2.5: [SOT Count: 251 (Rules: 108), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 12, QA: 357]**
- **Oct.2.3: [SOT Count: 251 (Rules: 107), Open: H:0, M:0, L:0, Ideas: H:0, M:2, L:1, Testing: 12, QA: 356]**
- **Oct.2.2: [SOT Count: 251 (Rules: 106), Open: H:1, M:0, L:0, Ideas: H:0, M:2, L:1, Testing: 12, QA: 355]**
- **Oct.2.1: [SOT Count: 251 (Rules: 104), Open: H:3, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 12, QA: 353]**
- **Oct.1.8: [SOT Count: 251 (Rules: 102), Open: H:4, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 12, QA: 352]**
