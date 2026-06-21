# ID Mechanism & Roles (v8.9.10)

This document describes the identity management system used to pair devices and define their behavior within the GPS Tracker ecosystem.

## 1. Identity Concepts
The system uses a simple string-based identification system that serves as both a **Security Key** and a **Routing Address**.

### A. Tracker ID (`deviceId`)
- **Purpose**: The primary identifier for the tracking machine (e.g., the device installed in a tractor).
- **Default**: `Ttk`.
- **Role**: This ID defines the "Room Name" on the relay server. All telemetry and logs sent by the tracker are tagged with this ID.

### B. Viewer ID (`viewerId`)
- **Purpose**: The identifier for the monitoring device (e.g., the owner's phone).
- **Default**: `Cohen`.
- **Role**: Used to identify specific viewers when multiple phones are monitoring the same tracker.

## 2. Pairing & Routing Mechanism (v8.9.10 baseline)

### A. Room-Based Signaling
The connection is established using a **Matching ID** logic:
1. The **Tracker** connects to the relay and joins a room named after its `deviceId`.
2. The **Viewer** connects to the relay and also joins the room named after the *Tracker's* `deviceId`.
3. The Relay Server broadcasts all packets (Location, Logs, etc.) only to members of that specific room.

### B. Mutual Authentication
Since the `deviceId` is chosen by the user, it acts as a shared secret.
- **Tracker Side**: Only accepts commands or settings update packets if they are relayed into its specific room.
- **Viewer Side**: Only processes location update or log update packets if they carry the matching `id`.

## 3. Forensic Role Identification (v8.9.10)
Every telemetry packet and forensic log entry includes a mandatory `role` field. In v8.9.10, every log is also **geographically anchored** with `lat`/`lng` coordinates to enable map reconstruction.

## 4. Dynamic ID Management
Users can change these IDs at any time through the "Settings" overlay:
- **Persistence**: IDs are persisted in the application's DataStore.
- **Hot-Reload**: When a user changes the ID in the UI, the system disconnects from the old room and immediately rejoins the new room.

## 5. UI Indicators
- **Tracker Mode**: Shows "ID: [Name]" at the top.
- **Viewer Mode**: Shows "Monitoring: [Tracker Name]".

## 6. Summary of Architecture Involved
- `SettingsRepository.kt`: Defines storage keys and defaults.
- `TrackerService.kt` / `ViewerService.kt`: Loads IDs on start.
- `SyncManager.kt`: Handles the socket "join" and tags outgoing packets.
- `LogManager.kt`: Attaches coordinates to forensic logs (v8.9.10).
