# Project Issues & Hardening Tracking (Rigorous Audit) - Oct6.2

## 🎯 Current Resumption Focus: Issue #AUDIT-1006-2: Background Service Transition Latency Audit.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   **Issue #AUDIT-1006-2: Background Service Transition Latency Audit.** Perform regression testing on background service transition latency following the removal of root-level UI invalidation triggers. Specifically verify `AlarmOverlayService` start-up time when `MemoryPressureLevel.HIGH` (5s throttling) is active. (Target: Oct6.2).

---

## 💡 Strategic Simplification Ideas (Ideas: 0)

### 🛑 High Priority
*   (All high-priority simplification ideas resolved).

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #AUDIT-1006-5: Forensic Log Pressure Hardening.** Resolved Oct6.2. Implemented async zero-drop fallback for important alerts in `LogRepository`. (Rule 1.119).
*   **Issue #AUDIT-1006-6: Memory Pressure Throttling.** Resolved Oct6.2. Integrated 15s/5s loop relaxation in `MonitorService` for memory-critical states. (Rule 1.120).
*   **Issue #AUDIT-1006-1: AlarmOverlayService State Leak Audit & Refinement.** Resolved Oct6.1. (R-ID 288).

---

## 📊 Hardening Progress Dashboard
- **Oct6.2: [SOT Count: 286 (Rules: 143), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 43, QA: 465]**
- **Oct6.1: [SOT Count: 284 (Rules: 141), Open: H:3, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 41, QA: 455]**
- **Oct.5.21: [SOT Count: 283 (Rules: 140), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 40, QA: 450]**
- **Oct.5.20: [SOT Count: 282 (Rules: 139), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 40, QA: 450]**
