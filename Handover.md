# Forensic Handover - v9.2.6 (HUD Context Mapping Authority)

## 📌 Status: Stable / Build PASS / Release Ready
This cycle remediates the "False Jammer Indicator" in Tracker mode by standardizing the HUD's telemetry binding context.

### 🟢 Completed: Issue #049 (False Jammer Indicator)
*   **Root Cause Remediation**:
    *   Updated `GlobalStatusBar` in `SharedUiComponents.kt` to implement **Mode-Aware Binding**.
    *   In **Tracker mode**, the HUD now correctly binds the local device's telemetry line and top-level GPS health badges to `localLocation` instead of `trackerLocation`.
    *   This prevents stale or empty remote state from triggering erroneous "JAMMER" labels or "P" (Pending) badges when the local hardware is healthy.
*   **Requirement Codified**: **R049** (HUD Context Mapping Authority) added to `requirements_sot.md`.

### 🟢 Verified: Issue #053 (Anchor Lock Breakout)
*   **Logic Audit**: Confirmed that `LocationProcessor.kt` already contains the behavioral breakout trigger (threshold dropped to 0.0m upon physical motion detection).

### 🛠 Instructions for Resumption
1.  **Verification of #049**: Run the app in Tracker mode. Enter a building to induce GPS loss. Verify the top-level GPS badge turns **Red**, but the Tracker-line label remains healthy (or shows "GPS GAP") without incorrectly flagging "JAMMER".
2.  **Verification of #044**: In Viewer mode, verify the local GPS badge stays green if the Viewer has a fix, regardless of the Tracker's signal state.
3.  **Soak Test (#031)**: Continue monitoring for `STABILITY GAP` logs during long-duration runs.

---
*Generated for chat resumption. All authoritative documents (SoT, issues, archive) are synchronized.*
