# Project History & Versioning (Oct21.06)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct21.06 (Current - Bidirectional Connectivity & Sync Fix)
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

## Oct20.33 (Stealth Hardening)
- **Tracker UI Role-Lock (#Oct20.31-F1)**: Architectural enforcement of R872. Purged all forbidden interactive elements (Alerts, Sound, Geofence management) and alarm manifestations from Tracker devices.
- **Operational Forensic Audit Sharding**: Decoupled Chapter 0 of the test procedure into `OPERATIONAL_FORENSIC_AUDIT.md` for better preservation and audit focus.

## Oct20.32 (UI Hardening & Navigation Fixes)
- **Back Button Termination Fix (#1424)**: Addressing critical bug where the system "return" button closes the app.
- **Version String Alignment (#1425)**: Relocating the version identifier to the absolute bottom of the UI for better branding compliance.
- **HUD Header Scrim (#1426)**: Improving button visibility in the upper HUD by adding a background scrim to prevent map occlusion.
- **Home Point Synchronization (#1428)**: Implemented bidirectional geofence sync.
