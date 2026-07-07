# Forensic Handover - v9.2.3 (HUD Local Health Standardization)

## 📌 Status: Stable / Build PASS / UX Consistency Implemented
This cycle remediates the "LED Contradiction" in the HUD by standardizing top-level status badges to reflect the local device's health.

### 🟢 Completed: Issue #044 (HUD: LEDs contradiction)
*   **UI/UX Standardisation**:
    *   Updated `GlobalStatusBar` in `SharedUiComponents.kt` to differentiate between `isLocalGpsActive` and `isTrackerGpsActive`.
    *   The top-level **GPS badge** now strictly represents the local device's GPS fix health.
    *   The **TRK/VWR badge** continues to represent the peer's telemetry presence (Peer Active).
    *   **Telemetry Veracity**: Maintained peer-dependent coloring for the **Speed** display and **Tracker State** label, ensuring they still turn gray (Slate500) if the remote Tracker's GPS is stale, even if the Viewer's local GPS is green.
*   **Refactoring**:
    *   Standardized `StatusBar` signature to accept both local and tracker GPS health flags.

### 🟢 Completed: Issue #326 (Intelligent Uncertainty UX Mapping)
*   **Engine Hardening**: Enriched Location Pending state with specific reasons (`GPS_GAP`, `JAMMER`).

### 🛠 Instructions for Resumption
1.  **Verification of #044**: Run the app in Viewer mode. Move the Tracker indoors (GPS loss). Verify that the top-level HUD GPS badge remains **Green** (if the Viewer is outdoors), while the **DAT** badge and the **Tracker Line** indicators reflect the signal loss.
2.  **Verification of Speed Gate**: Ensure that when the Tracker GPS is lost, the Viewer HUD speed drops to `0.0 km/h` and turns gray, regardless of the local Viewer's GPS status.
