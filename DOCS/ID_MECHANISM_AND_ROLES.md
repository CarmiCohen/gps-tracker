# ID Mechanism & Roles (v8.8.35)

This document describes the identity management system used to pair devices and define their behavior within the GPS Tracker ecosystem.

## 1. Identity Concepts
The system uses a simple string-based identification system that serves as both a **Security Key** and a **Routing Address**.

### A. Tracker ID (`deviceId`)
- **Purpose**: The primary identifier for the tracking machine (e.g., the device installed in a tractor).
- **Default**: `גוןדיר` (John Deere).
- **Role**: This ID defines the "Room Name" on the relay server. All telemetry and logs sent by the tracker are tagged with this ID.

### B. Viewer ID (`viewerId`)
- **Purpose**: The identifier for the monitoring device (e.g., the owner's phone).
- **Default**: `צופה` / `Viewer`.
- **Role**: Used to identify specific viewers when multiple phones are monitoring the same tracker. It allows the tracker to log which specific person is viewing the live feed or changing settings.

## 2. Pairing & Routing Mechanism (v8.8.35 baseline)

### A. Room-Based Signaling
The connection is established using a **Matching ID** logic:
1. The **Tracker** connects to the relay and joins a room named after its `deviceId`.
2. The **Viewer** connects to the relay and also joins the room named after the *Tracker's* `deviceId`.
3. The Relay Server broadcasts all packets (Location, Logs, etc.) only to members of that specific room.

### B. Mutual Authentication
Since the `deviceId` is chosen by the user, it acts as a shared secret.
- **Tracker Side**: Only accepts commands or settings update packets if they are relayed into its specific room and come from an authorized Viewer.
- **Viewer Side**: Only processes location update or log update packets if they carry the matching `id`.

## 3. Forensic Role Identification (v8.8.35)
Every telemetry packet and forensic log entry now includes a mandatory `role` field ("tracker" or "viewer"). This ensures that even in unified exported logs, the source of each event is unambiguous. Legacy version tags (`ver`, `vid`) have been removed from data models in favor of a simplified forensic model.

## 4. Dynamic ID Management
Users can change these IDs at any time through the "Settings" overlay:
- **Persistence**: IDs are persisted in the application's DataStore.
- **Hot-Reload**: When a user changes the ID in the UI, the system disconnects from the old room and immediately rejoins the new room without requiring a service restart.

## 5. UI Indicators
- **Tracker Mode**: Shows "ID: [Name]" at the top to indicate its identity. It also shows a list of "Connected Viewers" by tracking their `viewerId` pulses.
- **Viewer Mode**: Shows "Monitoring: [Tracker Name]" to confirm which remote device is being tracked. It also shows its own "Viewer ID" in settings for identification.

## 6. Summary of Architecture Involved
- `Constants.kt`: Defines the storage keys and global defaults. Updated to `https://gps-survival-relay.onrender.com`.
- `TrackerService.kt` / `ViewerService.kt`: Loads IDs on start and handles identity refresh.
- `AppNetworkManager.kt`: Uses the IDs to perform the socket "join" call and tags outgoing packets.
- `MainViewModel.kt`: Manages the reactive state of IDs and handles the logic for syncing IDs across the UI.
- `OverlayComponents.kt`: Provides the text fields for editing these identifiers.
