# Forensic Handover - v9.2.9 (Screen-Off Optimization)

## 📌 Status: Stable / Build PASS / Release Ready
This cycle implements R994, introducing intelligent GPS polling reduction when the screen is off to significantly improve battery life.

### 🟢 Completed: Requirement R994 (Screen-Off Optimization)
*   **Audited WakeLocks**: Confirmed `PARTIAL_WAKE_LOCK` necessity for background persistence. No changes made to lock acquisition logic.
*   **Screen State Tracking**: Enhanced `AppSensorManager` to track `Display.STATE_ON` via `DisplayManager`. Added `isScreenOn()` helper.
*   **Dynamic GPS Throttling**: 
    *   Introduced `SCREEN_OFF_GPS_POLLING_MS` (5000ms).
    *   Modified `ServiceBehaviorUseCase` to down-sample GPS polling when the device is locked, regardless of movement state (unless in Suspicious/Cooling modes).
*   **Architecture Integrity**: Screen-off logic is integrated into the existing `processTick` loop in `TrackerService`, maintaining consistency with the project's reactive behavior model.

### 🟢 Pre-existing State: v9.2.8
*   **R993 (Notification Throttling)**: Standardized dual-rate refresh (1s/10s) verified.

### 🛠 Instructions for Resumption
1.  **Verification of R994**: 
    *   In Tracker mode, verify via logcat that "GPS Frequency adapted to 5000ms" appears shortly after the screen is turned off.
    *   Verify it returns to `MOVING_GPS_POLLING_MS` (200ms) or `A15_STABLE_GPS_POLLING_MS` (1000ms) immediately when the screen is turned on.
2.  **Soak Test (#031)**: Monitor for any impact on GPS fix stability during long screen-off periods.

---
*Generated for chat resumption. All authoritative documents (SoT, issues, archive) are synchronized.*
