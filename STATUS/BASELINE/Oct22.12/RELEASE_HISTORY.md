# Project History & Versioning (Oct22.12)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct22.12 (Signaling Identity Symmetry)
- **Signaling Identity Symmetry (#ISSUE-OCT22.0-F3)**: Enforced strict Viewer ID symmetry in `SignalingValidator` for all signaling events (pulses, pings, pongs, telemetry).
- **Leak Prevention**: Explicitly blocked cross-viewer traffic in Viewer mode to prevent erroneous activity indications from unauthorized peers (R-ID 616).
- **Communication Hardening**: Integrated validator into `CommunicationManager` relay handlers to ensure UI status badges only transition to Green for authenticated peers.

## Oct22.10 (Setup Lifecycle Hardening)
- **Setup Exit Authority (#ISSUE-OCT22.0-F2)**: Integrated `BackHandler` into `SettingsOverlay` and `PhoneSetupOverlay` to prevent unintended application termination during configuration (R-ID 614).
- **Exit Authority Parity**: Added explicit Close (X) icon to `PhoneSetupOverlay` to ensure consistent exit mechanisms across all setup flows (R-ID 615).

## Oct22.0 (Setup & Configuration Hardening)
- **Setup Configuration Fix (#ISSUE-OCT22.0-F1)**: Resolved ID persistence blocks in `SettingsUseCase` to ensure customized Tracker/Viewer IDs are properly committed to DataStore.
- **Factory Reset Authority**: Implemented `LoadDefaults` to allow users to recover from invalid configurations.
- **UI Visibility**: Promoted Relay URL visibility and contrast in the Settings UI.

## Oct21.06 (Bidirectional Connectivity & Sync Fix)
- **Bidirectional Connectivity Fix (#ISSUE-OCT21.05-F2)**: Restored bidirectional signaling trust by integrating regular `viewer_status` pulses from Viewer to Tracker (R-ID 605). Resolved asymmetric Red VWR badge bug.
- **Proactive Geofence Synchronization (#ISSUE-OCT21.05-F2)**: Implemented automatic geofence sync upon connection establishment in Viewer mode (R-ID 606). Ensures Tracker parity without manual mutation.
- **Audit Hardening**: Integrated `verifyBidirectionalConnectivityAndSync` into `ProductionReadinessAuditTest.kt` to ensure future release-blocking failures if peer sync is broken.

## Oct21.05 (Unit Test Remediation)
- **Unit Test Version Synchronization (#ISSUE-OCT21.05-F1)**: Synchronized `MainViewModelMappingTest` with version string `Oct21.05` to resolve release-blocking forensic watchdog failure. Verified 43/43 tests PASS.

## Oct21.04 (UI & Map Hardening)
- **UI Overlap Resolution (#OCT20.33-F4)**: Explicitly anchored `ScaleBarOverlay` to bottom-right corner to resolve layout conflict with version tag in bottom-left.

## Oct21.03 (Debug Artifact Suppression)
- **Debug Artifact Suppression (#OCT20.33-F5)**: Wrapped `UI_READY` label in `BuildConfig.DEBUG` check to ensure zero-visibility in production PLATINUM builds.

## Oct21.02 (Settings Restoration)
- **Settings Gear Restoration (#OCT20.33-F3)**: Reverted the R872 purge of Settings capabilities on Trackers. Restored Geofence Radius, Alerts, and Sound sub-settings to allow field diagnostic verification of device state.

## Oct21.01 (Signaling & Connection Hardening)
- **Bidirectional Signaling Fix (#OCT20.33-F1)**: Restored telemetry trust by adding mandatory routing IDs to signaling pulses and explicitly handling `viewer_status` events. Ensured peer activity timestamps are updated for all valid incoming signals.

## Oct21.00 (HUD & Layout Hardening)
- **HUD Layout Correction (#OCT20.33-F2)**: Optimized HeaderBar button padding and increased StatusBar row vertical spacing to prevent "squeezing" on high-DPI A15 devices.
