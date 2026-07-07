# Forensic Handover - v9.2.8 (Notification Throttling)

## 📌 Status: Stable / Build PASS / Release Ready
This cycle implements R993, standardizing notification update frequency to balance UI responsiveness with background efficiency.

### 🟢 Completed: Requirement R993 (Notification Throttling)
*   **Intelligent Throttling**: Replaced legacy 60-second modulo updates with a time-based gate in `BaseMonitorService`.
*   **Dual-Rate Refresh**:
    *   **Foreground (Active)**: 1-second refresh rate when the user is actively viewing the app (guarded by `isUiVisible()`).
    *   **Background (Idle)**: 10-second refresh rate (`NOTIFICATION_THROTTLE_MS`) to reduce CPU wakeups and system log spam.
*   **Architecture Consistency**: Implementation utilizes the `TimeProvider` for monotonic consistency and maintains zero-dependency purity in `:core:engine` by hosting the constant in `EngineConstants.kt`.

### 🟢 Pre-existing State: v9.2.7
*   **R960 (HUD Layout)**: Local capability block grouping (INT, SRV, GPS) verified.

### 🛠 Instructions for Resumption
1.  **Verification of R993**: 
    *   Open the app and verify the notification "Sats/Batt" pulse updates every second.
    *   Move to the background and verify via logcat or observation that the notification updates slow down to every 10 seconds.
2.  **Soak Test (#031)**: Monitoring for `STABILITY GAP` logs continues.

---
*Generated for chat resumption. All authoritative documents (SoT, issues, archive) are synchronized.*
