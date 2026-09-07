# Project Issues & Hardening Tracking (Sep.07.70)

## 🎯 Current Resumption Focus: Forensic Auditor Consolidation
Extract shared audit logic into a unified ForensicAuditor (Simplicity Idea #3).

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)
*   **Idea #3: Forensic Auditor Consolidation**. Shared audit logic between Tracker and Viewer services should be unified to reduce redundancy.

## 🟢 Recently Resolved Issues (Sep.07.70)
*   **Issue #975 RESOLVED: Service Mutual Exclusivity**. Enforced termination of the opposite role service during mode transitions in `MainActivity` to prevent "ghost" telemetry and HUD false-positives during single-device testing.

## 🟢 Recently Resolved Issues (Sep.07.61)
*   **HUD LED Specification Compliance (R960/R972)**: Remediated false-positive green indicators for `VWR` and `DAT` in Tracker Mode. Gated `DAT` strictly to Viewer Mode and restricted `VWR` activity resets to genuine peer pulses, preventing generic signaling heartbeats from masking peer absence.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 288 (Rules: 50, IDs: 238), Resolved: 975, Open: 0, Testing: 95% (Sub-items: 47), Ideas: 5, QA: 263]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.07.70)*
