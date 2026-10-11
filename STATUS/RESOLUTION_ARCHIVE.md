# Resolution Archive

## [Oct11.8] - Performance Tier Alignment (JNI & Bayesian)
**Issue ID**: #SIMP-1011-22
**Status**: RESOLVED
**Description**: Refined performance-sensitive thresholds to be hardware-aware, reducing false "Mali Anomaly" triggers on budget devices while stabilizing geofence reliability.
**Root Cause**: Static thresholds for JNI bridge latency (150ms) and Bayesian uncertainty growth (15m/s) were optimized for standard hardware, causing instability and false alerts on budget (Staggered tier) devices like the A15.
**Remediation**:
- **Tiered JNI Thresholds**: Implemented hardware-aware JNI bridge latency limits in `IntegrityMonitor.kt` (Standard: 150ms, Staggered: 250ms).
- **Bayesian Expansion Unification**: Updated `LocationProcessor.kt` to scale accuracy uncertainty growth based on `PerformanceTier` (Standard: 15m/s, Staggered: 25m/s).
- **State Integration**: Propagated performance tier authority from `SystemStatusProvider` through `MonitorService` into the engine's core processing state.

## [Oct11.7] - GPS Acquisition Recovery
**Issue ID**: #SIMP-1107
**Status**: RESOLVED
**Description**: Resolved a regression where GPS status remained RED in both modes during stationary power-save periods.
**Root Cause**: Conflict between `STATIONARY_GPS_POLLING_MS` (60s) and failure detection thresholds (`GPS_GAP_THRESHOLD_MS` 15s, `GPS_UI_FAIL_THRESHOLD_MS` 35s). Aggressive GNSS revival loops were resetting hardware before the intentional 60s fix could be delivered.
**Remediation**:
- **Dynamic Gap Thresholding (#SIMP-1107)**: Implemented interval-aware gap detection in `HardwareSuite.kt` (scaled to 2x interval).
- **Revival Guard**: Hardened `checkRevivalLifecycle` to wait 1.5x the intended interval before triggering hardware restarts.
- **UI Freshness Harmonization**: Updated `MainViewModel.kt` to scale GPS LED freshness based on current state (`PARKING` or `ULTRA`).

...
