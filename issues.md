# Project Issues & Hardening Tracking (Rigorous Audit) - Oct7.3

## 🎯 Current Resumption Focus: Radio Duty Cycle Analysis & Persistence Optimization.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority
*   *(No high priority open gaps)*

---

## 💡 Strategic Simplification Ideas (Ideas: 1)
*   **SIMP-1007-15: Unified Snapshot Container.** (Medium) - Consider moving snrSnapshot, vibeSnapshot, thermalSnapshot, and heapSnapshot into a single `ForensicSnapshot` data class within `IntegrityState` to simplify property delegation and copying logic.

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #QA-1007-1: Telemetry Forensic Expansion & Radio Soak Validation.** Resolved Oct7.3.
    *   **Protobuf Expansion**: Promoted 12 internal engine flags (muzzled, siren, environmental lockouts, snapshots) to Protobuf for remote diagnostics.
    *   **Dispatcher Hardening**: Implemented first-entry starvation cap in `SmartSignalingDispatcher` to prevent indefinite conflation delays.
    *   **Data Integrity**: Fixed `LocationUpdate.duplicate()` to preserve body-defined properties during pipeline emission.
*   **Issue #QA-1006-12: Android 15 (API 35) Deployment & Forensic Audit.** Resolved Oct7.2.
*   **Issue #SIMP-1006-14: Telemetry Field Pruning.** Resolved Oct7.1. 

---

## 📊 Hardening Progress Dashboard
- **Oct7.3: [SOT Count: 308 (Rules: 162), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 58, QA: 582]**
- **Oct7.2: [SOT Count: 306 (Rules: 161), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 57, QA: 578]**
- **Oct7.1: [SOT Count: 306 (Rules: 161), Open: H:1, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 57, QA: 577]**
