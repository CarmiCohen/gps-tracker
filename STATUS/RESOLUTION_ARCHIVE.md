# Resolution Archive - GPS Tracker (Oct22.10)

## ISSUE-OCT22.0-F2: Setup Exit Authority & Lifecycle Failure (Oct22.10)
- **Resolved**: Oct22.10
- **Description**: The Setup overlay could not be closed via UI; the system Back button caused the entire application to exit instead of just dismissing the overlay.
- **Remediation**:
    - **BackHandler Integration**: Implemented `BackHandler` in `SettingsOverlay` and `PhoneSetupOverlay` to intercept system Back events and route them to overlay dismissal (R-ID 614).
    - **Exit Authority**: Added an explicit Close (X) icon to the `PhoneSetupOverlay` header to allow users to exit the setup flow without killing the process (R-ID 615).
    - **Sub-setting Navigation**: Hardened `BackHandler` in `SettingsOverlay` to prioritize closing active sub-settings (Alerts/Sound/Clean) before dismissing the main overlay.
- **Outcome**: 🟢 **PASSED**. (SOT ID 811).

## ISSUE-OCT22.0-F1: Setup Configuration Failure (Oct22.0)
- **Resolved**: Oct22.0
- **Description**: ID persistence was blocked in `SettingsUseCase`, and the Setup overlay lacked exit authority.
- **Remediation**:
    - **Persistence Fix**: Corrected the logic block in `SettingsUseCase` to ensure draft settings are properly committed to DataStore.
    - **Authority Restoration**: Added Close icon to `SettingsOverlay`.
    - **Factory Reset**: Implemented `LoadDefaults` in `UiEventCoordinator`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 810).

## ISSUE-OCT21.05-F2: Asymmetric Connectivity & Geofence Sync failure (Oct21.06)
- **Resolved**: Oct21.06
- **Description**: The Viewer showed a green TRK badge (connected), but the Tracker showed a red VWR badge (disconnected) and failed to display Home Points synced from the Viewer.
- **Remediation**:
    - **Viewer Pulse Restoration**: Integrated `viewer_status` pulse into `AppEventCoordinator.kt` to ensure the Tracker receives regular heartbeats (R-ID 605).
    - **Proactive Geofence Sync**: Updated `ConnectivitySuite.kt` to trigger `syncHomePoints()` immediately upon connection in Viewer mode (R-ID 606).
    - **Protocol Protocol**: Added `createViewerStatusUpdate` to `SignalPayloadGenerator.kt`.
    - **Verification Test**: Added `verifyBidirectionalConnectivityAndSync` to `ProductionReadinessAuditTest.kt` to enforce peer sync parity in Chapter 0 audits.
- **Outcome**: 🟢 **PASSED**. (SOT ID 801).

---
**NOTE**: Historical resolutions (SOT ID 1-800) preserved in `STATUS/docs_history_archive.md`.
