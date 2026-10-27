git add .
git commit -m "Fix: Signaling Conflation UI Feedback and HUD Transparency. #ISSUE-OCT27.0-F1"
git tag -a Oct27.0 -m "Oct27.0 Release: Signaling Pipeline Visibility"
git push origin Oct27.0

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct27.0

## 🎯 Current Resumption Focus: Forensic Convergence Audit.

## 🔴 Open Gaps & Unfinished Integration Points
*   **GAP-OCT27.0-1: Forensic Convergence Audit.** Need to verify if `LogRepository` drain cycles can keep pace with sustained 100Hz telemetry bursts during 24-hour "Noisy" environment simulations.

---

## 💡 Strategic Simplification Ideas (Ideas: 95)
*   **SIMP-OCT27.0-S1: Unit-Based Badge Scaling (Low).** Transition HUD badges to use a shared `MeasurableBadge` component to reduce redundant `Box` and `Text` declarations in `SharedUiComponents.kt`.
*   **SIMP-OCT26.6-S1: Diagnostic Latch for SRV Badge (Low).** Add a 5-second "Aborted" latch color (Orange) to the SRV badge when a connection is rejected due to identity mismatch.
*   **SIMP-OCT26.5-S1: Signaling Metric Aggregation (Low).** Combine `framesReceived`, `framesEmitted`, and `framesConflated` into a single `SignalingHealth` object in `SmartSignalingDispatcher`.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT27.0-F1: Signaling Conflation UI Feedback.** Resolved Oct27.0. Implemented numerical counter overlay for the `DAT` badge and integrated long-press diagnostic feedback to show exact conflation metrics. (Resolved GAP-OCT26.5-1).
*   **ISSUE-OCT26.6-F1: Forensic Traceability Hardening.** Resolved Oct26.6. Standardized connection abortion logging across `CommunicationManager` and `ConnectivitySuite`.
*   **ISSUE-OCT26.5-F1: Telemetry Soak Simulation Hardening.** Resolved Oct26.5. Implemented double-buffering for `KinematicState` and `DiagnosticState` in `MainViewModel`.
*   **ISSUE-OCT26.4-F1: UI Separation Hardening.** Resolved Oct26.4. Enforced R-ID 612 by moving version tag to `BottomEnd`.

---

## 📊 Hardening Progress Dashboard
- **Oct27.0: [SOT Count: 847 (Rules: 348), Open: H:0, M:1, L:0, Ideas: 95, Testing: 1 (Sub-items: 33), QA: PASSED]**
- **Oct26.6: [SOT Count: 846 (Rules: 348), Open: H:0, M:1, L:0, Ideas: 94, Testing: 1 (Sub-items: 32), QA: PASSED]**
- **Oct26.5: [SOT Count: 844 (Rules: 346), Open: H:0, M:1, L:0, Ideas: 93, Testing: 1 (Sub-items: 31), QA: PASSED]**
- **Oct26.4: [SOT Count: 842 (Rules: 345), Open: H:0, M:0, L:0, Ideas: 92, Testing: 1 (Sub-items: 29), QA: PASSED]**
