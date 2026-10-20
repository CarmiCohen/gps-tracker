# Issue #1428: Home Point Synchronization Failure

## 🎯 Objective
Enable real-time synchronization of geofence home points from the Viewer to the Tracker to ensure boundary enforcement is consistent across both devices.

## 🚩 Problem Statement
Users observed that home points added or modified on the Viewer were not being propagated to the Tracker. This meant the Tracker remained unaware of the safe zones defined by the Viewer, leading to potential boundary enforcement gaps.

## 🛠️ Proposed Mediation
*   **Settings Relay Implementation**: Utilize the relay's `settings_update` event to broadcast geofence changes.
*   **Proactive Sync**: Trigger a full home point sync from `ConnectivitySuite` whenever a geofence mutation (add/remove/clear) occurs via `UiEventCoordinator`.
*   **Remote Listener**: Implement a handler on the Tracker side to receive and persist these updates in the local `SettingsRepository`.

## 📊 Requirements
*   **R-ID 606**: Home point updates on the Viewer MUST be propagated to the Tracker within 5 seconds under normal network conditions.
