# Resolution Archive

## [Oct11.20] - Mapping Centralization & HUD Clarity
**Issue ID**: #ISSUE-OCT12.13-5, #ISSUE-OCT12.14-3, #ISSUE-OCT12.16-6
**Status**: RESOLVED
**Description**: Purged redundant `UiStateCoordinator` to eliminate stale mapping logic. Hardened HUD error indicators with contextual labels and resolved navigation bar overlap on gesture-enabled devices.
**Root Cause**: `UiStateCoordinator` contained duplicated projection logic that lacked critical Oct11.7 and Oct12.19 hardening fixes. Global HUD error icons lacked labels, making it difficult to distinguish relay disconnection from telemetry staleness. Map overlays (version tag/uncertainty) lacked sufficient vertical buffering from system navigation bars.
**Remediation**:
- **Dead Code Purge**: Emptied `UiStateCoordinator.kt` and centralized all UI state mapping within `MainViewModel.kt` using validated engine polling thresholds.
- **HUD Clarity**: Updated `StatusBar` in `SharedUiComponents.kt` to include "DATA ERR" labels for link health failures. Refined `mapHudConnectivity` logic to correctly evaluate health based on active role (Tracker vs Viewer).
- **Inset Hardening**: Applied `windowInsetsPadding(WindowInsets.navigationBars)` and increased safety margins (44dp/82dp) for map overlays in `MapComponents.kt`.
- **Versioning**: Standardized internal engine version strings to `Oct11.20`.

## [Oct12.19] - Signaling Resilience & UI Density Hardening
**Issue ID**: #ISSUE-OCT12.16-5, #ISSUE-OCT12.13-2, #ISSUE-OCT12.16-4
**Status**: RESOLVED
**Description**: Hardened the signaling provider to survive rapid network handovers. Harmonized UI staleness thresholds with dynamic polling tiers. Increased touch target density in the HeaderBar for safety-critical usability.
**Root Cause**: `CommunicationManager.connect()` was destroying/reconstructing sockets on every interface change, causing relay "reset-bombing" during Wi-Fi/LTE flapping. UI freshness thresholds used a fixed 60s fallback which didn't account for the 45s Screen-Off interval margin. Portrait HeaderBar icons had 38dp touch targets, which were insufficient for reliable field use.
**Remediation**:
- **Signaling Resilience**: Implemented an idempotency check in `CommunicationManager.connect()`. If a connection attempt with identical parameters is already in progress or active, reconstruction is suppressed.
- **Threshold Harmonization**: Refined `MainViewModel.kt` staleness logic to explicitly account for the `SCREEN_OFF_GPS_POLLING_MS` (45s) and `STATIONARY_GPS_POLLING_MS` (60s) tiers, adding appropriate safety margins.
- **Layout Density**: Increased `HeaderBar` icon sizes to 24dp and touch targets to 48dp in `SharedUiComponents.kt`. Resolved compilation errors in Analytical Ribbon selectors.

## [Oct12.18] - Temporal Integrity & I/O Optimization
**Issue ID**: #ISSUE-OCT12.13-11, #ISSUE-OCT12.13-10
**Status**: RESOLVED
**Description**: Eliminated extreme jitter spikes in viewer forensics caused by system suspension. Optimized database maintenance to prevent UI stutter during pruning operations.
**Root Cause**: `ForensicAuditor` was calculating GNSS jitter across system sleep intervals, resulting in 1000s+ false positives. Database pruning used a monolithic transaction block that held a write-lock for up to 3.4s, blocking telemetry persistence and UI updates.
**Remediation**:
- **Jitter Hardening**: Implemented role-isolation in `ForensicAuditor.kt`. Added a 15s gap protection gate to `recordGnssStatus` and `recordGpsFix` to ignore intervals representing system suspension.
- **I/O Optimization**: Refactored `MainRepository.kt` background pruning to remove giant `withTransaction` blocks. Implemented `yield()` points and chunked deletion to allow high-priority telemetry tasks to interleave with maintenance.
- **Role Synchronization**: Updated `MonitorService.kt` and `HardwareSuite.kt` to explicitly synchronize the active `AppRole` for isolated forensic auditing.

## [Oct12.17] - HUD Remediation & Engine Hardening
**Issue ID**: #ISSUE-OCT12.16-1, #ISSUE-OCT12.16-3, #ISSUE-OCT12.13-1, #ISSUE-OCT12.13-3, #ISSUE-OCT12.13-7, #ISSUE-OCT12.13-6
**Status**: RESOLVED
**Description**: Remediated UI collisions and legibility gaps identified in Oct12.16 audit. Hardened GPS and Signaling engines to resolve confirmed stalls.
**Root Cause**: Insufficient HUD background contrast (40% alpha) against detailed map tiles. Z-stacking collision between ScaleBar and Version Tag. GPS UI staleness threshold (35s) was lower than Screen-Off polling (45s). GPS Hardware Lock lacked an auto-recovery path. Signaling connection logic was vulnerable to latching in 'isConnecting' state during relay failures.
**Remediation**:
- **HUD Visibility**: Increased `StatusBar` background opacity to `0.75f` in `SharedUiComponents.kt`.
- **UI Collision**: Added `42.dp` bottom padding to map version tag in `MapComponents.kt` to clear `ScaleBarOverlay`.
- **Threshold Harmonization**: Increased `TELEMETRY_UI_STALE_THRESHOLD_MS` and `GPS_UI_FAIL_THRESHOLD_MS` to `60s` in `EngineConstants.kt`.
- **GPS Recovery**: Implemented 10-minute auto-reset for `GPS_HARDWARE_LOCK` in `HardwareSuite.kt`. Added acquisition flutter protection via `GPS_REGISTRATION_DEBOUNCE_MS`.
- **Signaling Hardening**: Integrated `EVENT_CONNECT_ERROR` handling and a 35s safety timeout to `CommunicationManager.kt` to prevent connection stalls.
- **Viewer Fix**: Corrected smoothing coordinate logic in `MainViewModel.kt` for local viewer position to prevent (0,0) defaulting.

## [Oct12.12] - Forensic Integrity & Batch Persistence
**Issue ID**: #SIMP-1012-3, #SIMP-1012-4
**Status**: RESOLVED
...
