# Forensic Handover - v9.2.7 (Local Capability Block)

## 📌 Status: Stable / Build PASS / Release Ready
This cycle implements R960, refining the HUD layout to prioritize local hardware capability status.

### 🟢 Completed: Requirement R960 (HUD Layout Refinement)
*   **Logical Grouping**: Updated `StatusBar` in `SharedUiComponents.kt` to move the **GPS** badge adjacent to **INT** and **SRV**.
*   **Local Capability Block**: The HUD upper row now forms a clear "Local Health Strip" (INT, SRV, GPS) followed by peer-dependent status badges (TRK/VWR, DAT).
*   **UX Hierarchy**: This layout allows the user to immediately distinguish between local hardware failures and remote link/peer failures.

### 🟢 Pre-existing State: v9.2.6
*   **Issue #049 (False Jammer Indicator)**: Remediated via Mode-Aware Binding.
*   **Forensic Stress Test**: Infrastructure implemented for manual violation simulation.

### 🛠 Instructions for Resumption
1.  **Verification of R960**: Run the app and verify the upper-left badge sequence is: `[INT] [SRV] [GPS] [TRK/VWR] [DAT]`.
2.  **Verification of #049**: Verify that local GPS loss in Tracker mode does not incorrectly flag "JAMMER" on the local telemetry line.
3.  **Soak Test (#031)**: Monitoring for `STABILITY GAP` logs.

---
*Generated for chat resumption. All authoritative documents (SoT, issues, archive) are synchronized.*
