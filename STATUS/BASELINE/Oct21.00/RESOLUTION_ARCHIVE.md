# Resolution Archive - GPS Tracker (Oct21.00)

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
