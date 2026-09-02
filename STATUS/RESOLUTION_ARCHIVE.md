# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.02.55 (vSep.02.55)
*   **Issue #119 RESOLVED: Battery Steep Discharge Refinement**.
    *   **Problem**: Aggressive Power Save entries and false "Battery Health" alerts were being triggered by erratic battery percentage drops on budget hardware (Samsung A15) or during high-load forensic bursts.
    *   **Remediation**: Hardened thresholds in `EngineConstants.kt`. Reduced `CRITICAL_BATTERY_THRESHOLD` from 20% to 10% to provide more headroom before emergency logic engages. Increased `BATTERY_STEEP_DISCHARGE_THRESHOLD_NORMAL` to 5% and `BATTERY_STEEP_DISCHARGE_THRESHOLD_HIGH_LOAD` to 10% per 10-minute window. Updated `IntegrityMonitor` and `MainAlarmLogic` to consume these refined gates (R119).

## 🟢 Sep.03.01 (vSep.03.01)
*   **Issue #197 RESOLVED: Forensic Teardown Timing Logs**.
    *   **Problem**: Identifying OS-level disposal delays during component unregistration required high-precision timing across all core services to match the hardware layer's auditing.
    *   **Remediation**: Enhanced `ConnectivitySuite.stop()` and `CommunicationManager.disconnect()` with forensic duration tracking. Implemented high-precision log summaries to report unregistration times for network callbacks and socket cleanup, ensuring forensic parity across the entire teardown sequence (R-ID 197).
*   **Issue #238 RESOLVED: Location Model Unification**.
    *   **Problem**: Redundant mapping between `LocationUpdate` (Engine) and `LocationState` (UI) caused excessive allocation churn on high-frequency telemetry updates.
    *   **Remediation**: Promoted the core engine `LocationUpdate` model to be the unified source of truth. Removed the obsolete `LocationState` class and refactored `MainUiState`, `KinematicState`, and `TelemetryUseCase` to consume the unified model directly. This eliminates O(N) mapping overhead and reduces GC pressure during active tracking sessions (R-ID 238).

## 🟢 Sep.02.50 (vSep.02.50)
*   **Issue #005 RESOLVED: Log Spillage Hardening**.
    *   **Problem**: Diagnostic log spillage into `logcat` on Samsung G990 (S21FE) and A15 hardware created forensic risks and could lead to performance degradation during high-frequency telemetry bursts. Direct use of `android.util.Log` bypassed central suppression controls.
    *   **Remediation**: Replaced all direct `android.util.Log` calls with `Timber` across the app module. Established Architectural Rule 1.18 (R759) to strictly prohibit direct `Log` calls, ensuring long-term silence on audited hardware (R-ID 239).

---
*For older resolutions, see prior sub-versions.*
