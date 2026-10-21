# Resolution Archive - GPS Tracker (Oct21.06)

## ISSUE-OCT21.05-F2: Asymmetric Connectivity & Geofence Sync failure (Oct21.06)
- **Resolved**: Oct21.06
- **Description**: The Viewer showed a green TRK badge (connected), but the Tracker showed a red VWR badge (disconnected) and failed to display Home Points synced from the Viewer.
- **Remediation**:
    - **Viewer Pulse Restoration**: Integrated `viewer_status` pulse into `AppEventCoordinator.kt` to ensure the Tracker receives regular heartbeats (R-ID 605).
    - **Proactive Geofence Sync**: Updated `ConnectivitySuite.kt` to trigger `syncHomePoints()` immediately upon connection in Viewer mode (R-ID 606).
    - **Protocol Protocol**: Added `createViewerStatusUpdate` to `SignalPayloadGenerator.kt`.
    - **Verification Test**: Added `verifyBidirectionalConnectivityAndSync` to `ProductionReadinessAuditTest.kt` to enforce peer sync parity in Chapter 0 audits.
- **Outcome**: 🟢 **PASSED**. (SOT ID 801).

## ISSUE-OCT21.05-F1: Unit Test Version Mismatch (Oct21.05)
- **Resolved**: Oct21.05
- **Description**: The unit test `MainViewModelMappingTest > Verify Dashboard Connectivity Watchdog` was failing due to a hardcoded version string expectation that did not match the release target `Oct21.05`.
- **Remediation**:
    - **Test Synchronization**: Updated the `expected` version string literal in `MainViewModelMappingTest.kt` to match `Oct21.05`.
    - **Audit Continuity**: Verified 43/43 unit tests passing following the fix.
- **Outcome**: 🟢 **PASSED**. (SOT ID 800).

## ISSUE-OCT20.33-F4: UI Overlap (Version/Scale) (Oct21.04)
- **Resolved**: Oct21.04
- **Description**: The version tag was overlapping with the Map Scale bar in the bottom-left corner of the Tracker UI on certain screen densities.
- **Remediation**:
    - **Scale Bar Alignment**: Updated `MapComponents.kt` to explicitly anchor the `ScaleBarOverlay` to the bottom-right corner (`setAlignBottom(true)`, `setAlignRight(true)`).
    - **Corner Separation**: Maintained version tag at `BottomStart` (bottom-left) per R-ID 603 to ensure zero overlap.
    - **Regression Fix**: Corrected typo in `MapToolsOverlay` geofence button container color logic.
- **Outcome**: 🟢 **PASSED**. (SOT ID 799).

## ISSUE-OCT20.33-F5: Debug Artifact Leak (Oct21.03)
- **Resolved**: Oct21.03
- **Description**: The `UI_READY` status label was visible in production builds (PLATINUM), leaking internal state and violating stealth aesthetics.
- **Remediation**:
    - **UI Suppression**: Wrapped the `UI_READY` Text component in `MainAppContent.kt` with a `BuildConfig.DEBUG` check.
    - **Architecture**: Introduced R-ID 610 (Debug Artifact Suppression) to establish a baseline rule for diagnostic label management.
- **Outcome**: 🟢 **PASSED**. (SOT ID 798).

## ISSUE-OCT20.33-F3: Settings Gear Restoration (Oct21.02)
- **Resolved**: Oct21.02
- **Description**: Reversal of the partial R872 purge. The Settings gear and associated sub-settings (Geofence Radius, Alerts, Sound) are now available on the Tracker for diagnostic verification.
- **Remediation**:
    - **UI Restoration**: Updated `SettingsComponents.kt` to remove role-based suppression for `maxDistance` and the `ALERTS`/`SOUND` navigation buttons.
    - **Diagnostic Parity**: Restored read/write access to core configuration fields on the Tracker to allow field-level state audits without requiring a Viewer connection.
- **Outcome**: 🟢 **PASSED**. (SOT ID 797).

## ISSUE-OCT20.33-F1: VWR Connection Asymmetry (Oct21.01)
- **Resolved**: Oct21.01
- **Description**: Tracker reports a Red (Disconnected) VWR badge while the Viewer reports Green. Root cause was missing mandatory routing IDs in signaling pulses and unhandled viewer status events.
- **Remediation**:
    - **Protocol Protocol**: Updated `SignalPayloadGenerator.kt` to include mandatory `id` (Tracker ID) in `createViewerPulseUpdate` and `createTrackerPulseUpdate`.
    - **Event Handling**: Hardened `ConnectivitySuite.kt` to explicitly process `viewer_status` messages and update peer activity timestamps for all valid signals.
    - **Versioning**: Incremented protocol version to `Oct21.01` in `CommunicationManager.kt`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 796).

## ISSUE-OCT20.33-F2: HUD Layout Compression (Oct21.00)
- **Resolved**: Oct21.00
- **Description**: The telemetry HUD was vertically squeezed, making text illegible, while action buttons had excessive padding. Observed primarily on high-DPI A15 devices.
- **Remediation**:
    - **Button Scaling**: Reduced HeaderBar portrait button size from 48dp to 40dp and icon size from 24dp to 22dp.
    - **Vertical Spacing**: Increased StatusRowData vertical padding to 2.dp and global StatusBar padding to 4.dp bottom.
    - **Safe Area**: Increased `requiresExtraTopPadding` from 8.dp to 12.dp for better status bar clearance.
- **Outcome**: 🟢 **PASSED**. (SOT ID 795).

## ISSUE-OCT20.31-F1: Forbidden Tracker Authority & Stealth Breach (Oct21.00)
- **Resolved**: Oct21.00
- **Description**: Tracker Mode UI contained forbidden interactive elements (Home Point management, Alert settings) and telemetry badges (ALM/SIREN/LOCK) that violated R872 (Absolute Stealth).
- **Remediation**:
    - **UI Purge**: Strictly removed Alerts, Sound, and Geofence radius configuration from Tracker settings.
    - **Tool Suppression**: Removed ADD/DEL Home Point buttons from map tools in Tracker mode.
    - **HUD Stealth**: Suppressed `ALM`, `SIREN`, and `LOCK` status badges on Trackers.
- **Outcome**: 🟢 **PASSED**. (SOT ID 794).

## ISSUE-OCT20.32-F5: Home Point Synchronization Failure (Oct21.00)
- **Resolved**: Oct21.00
- **Description**: Geofence home points added on the Viewer were not being propagated to the Tracker.
- **Remediation**:
    - **Signaling**: Added `settings_relay` listener to CommunicationManager.
    - **Proactive Sync**: Implemented `syncHomePoints()` in ConnectivitySuite.
- **Outcome**: 🟢 **PASSED**. (SOT ID 793).

## ISSUE-OCT20.32-F4: Asymmetrical Connection State (Oct21.00)
- **Resolved**: Oct21.00
- **Description**: The Tracker device failed to recognize an active Viewer connection (remaining Red).
- **Remediation**:
    - **Unified Pulse**: Updated ConnectivitySuite to ensure peer state updates for both roles.
- **Outcome**: 🟢 **PASSED**. (SOT ID 792).

## ISSUE-OCT20.32-F3: HUD Header Scrim/Occlusion (Oct21.00)
- **Resolved**: Oct21.00
- **Description**: HUD header buttons were nearly invisible in some map areas.
- **Remediation**:
    - **Scrim Implementation**: Added Slate900 background (0.6 alpha) to upper HUD.
- **Outcome**: 🟢 **PASSED**. (SOT ID 791).

## ISSUE-OCT20.32-F2: Version String Alignment (Oct21.00)
- **Resolved**: Oct21.00
- **Description**: The version identifier was floating above the bottom edge.
- **Remediation**:
    - **Positioning**: Pinned version text to BottomStart with 4.dp padding.
- **Outcome**: 🟢 **PASSED**. (SOT ID 790).

## ISSUE-OCT20.32-F1: Back Button Termination Bug (Oct21.00)
- **Resolved**: Oct21.00
- **Description**: Tapping back on main screens stopped services.
- **Remediation**:
    - **BackPress Audit**: Verified BackHandler usage to ensure service persistence.
- **Outcome**: 🟢 **PASSED**. (SOT ID 789).

---
**NOTE**: Historical resolutions (SOT ID 1-788) preserved in `STATUS/docs_history_archive.md`.
