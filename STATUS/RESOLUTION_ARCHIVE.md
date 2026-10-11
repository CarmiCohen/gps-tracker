# Resolution Archive

## [Oct11.7] - GPS Acquisition Recovery
**Issue ID**: #SIMP-1107
**Status**: RESOLVED
**Description**: Resolved a regression where GPS status remained RED in both modes during stationary power-save periods.
**Root Cause**: Conflict between `STATIONARY_GPS_POLLING_MS` (60s) and failure detection thresholds (`GPS_GAP_THRESHOLD_MS` 15s, `GPS_UI_FAIL_THRESHOLD_MS` 35s). Aggressive GNSS revival loops were resetting hardware before the intentional 60s fix could be delivered.
**Remediation**:
- **Dynamic Gap Thresholding (#SIMP-1107)**: Implemented interval-aware gap detection in `HardwareSuite.kt` (scaled to 2x interval).
- **Revival Guard**: Hardened `checkRevivalLifecycle` to wait 1.5x the intended interval before triggering hardware restarts.
- **UI Freshness Harmonization**: Updated `MainViewModel.kt` to scale GPS LED freshness based on current state (`PARKING` or `ULTRA`).

## [Oct11.6] - Radio-Warm Keep-Alive
**Issue ID**: #SIMP-1011-21
**Status**: RESOLVED
**Description**: Implemented a periodic GNSS radio keep-alive strategy to prevent hardware residency lock on budget Samsung devices.
**Root Cause**: Samsung App Standby (Staggered Tier) forces the GNSS radio into a deep sleep state after ~8 minutes of background execution, ignoring standard recovery pulses.
**Remediation**:
- **Radio-Warm Keep-Alive (#SIMP-1011-21)**: Implemented `checkRadioResidency` in `HardwareSuite.kt`.
- **Periodic Bursts**: Added a 60s periodic keep-alive trigger using `PRIORITY_BALANCED_POWER_ACCURACY` to maintain radio residency during background or stationary periods.

...
