# ID Mechanism & Roles (v9.1.0)

This document describes the identity management system used to pair devices and define their behavior within the GPS Tracker ecosystem.

## 1. Identity Concepts
The system uses a simple string-based identification system that serves as both a **Security Key** and a **Routing Address**.

### A. Tracker ID (`deviceId`)
- **Purpose**: The primary identifier for the tracking machine (e.g., the device installed in a tractor).
- **Default**: `T` (Standardized per R182).
- **Role**: This ID defines the "Room Name" on the relay server. All telemetry and logs sent by the tracker are tagged with this ID.

### B. Viewer ID (`viewerId`)
- **Purpose**: The identifier for the monitoring device (e.g., the owner's phone).
- **Default**: `V` (Standardized per R182).
- **Role**: Used to identify specific viewers. **Identity Locking (R982)** ensures that only a Viewer with the matching `viewerId` can control or ping a Tracker.

## 2. Pairing & Routing Mechanism (v9.1.0 Hardening)

### A. Room-Based Signaling
The connection is established using a **Matching ID** logic:
1. The **Tracker** connects to the relay and joins a room named after its `deviceId`.
2. The **Viewer** connects to the relay and also joins the room named after the *Tracker's* `deviceId`.
3. The Relay Server broadcasts all packets (Location, Logs, etc.) only to members of that specific room.

### B. Mutual Authentication & Identity Locking (R982)
Since the `deviceId` is chosen by the user, it acts as a shared secret.
- **Tracker Side**: Only accepts commands, pings, or settings update packets if they originate from a Viewer carrying the authorized `viewerId`. Packets from mismatched Viewer IDs are silently discarded at the signaling layer.
- **Viewer Side**: Only processes location update or log update packets if they carry the matching `id` (Tracker ID).
- **R182 Uniqueness**: Tracker and Viewer IDs must be unique from each other to prevent loopback forensic collisions.
- **Identity Sanitization (R975)**: All IDs are strictly validated against the regex `^[a-zA-Z0-9_-]{1,32}$`.

## 3. Forensic Role Identification
Every telemetry packet and forensic log entry includes a mandatory `role` field (Issue #182). Every log is also **geographically anchored** with `lat`/`lng` coordinates, `accuracy`, and authoritative `maxAccuracy` to enable forensic path reconstruction (Issue #325).

## 4. Dynamic ID Management
Users can change these IDs at any time through the "Settings" overlay:
- **Persistence**: IDs are persisted in the application's DataStore.
- **Hot-Reload**: When a user changes the ID in the UI, the system disconnects from the old room and immediately rejoins the new room.
- **Draft Settings**: The UI utilizes a draft buffer to ensure ID changes are committed atomically, preventing partial configuration states.

## 5. UI Indicators
- **Tracker Mode**: Shows "ID: [Name]" at the top.
- **Viewer Mode**: Shows "Monitoring: [Tracker Name]".
- **Ghost Mode (R338)**: Visual indicators dim to `Slate500` if the remote peer's identity pulse is > 15s old. (Aligned with v8.9.54 Hardening mandate).

## 6. Summary of Architecture Involved
- `SettingsRepository.kt`: Defines storage keys, defaults, and sanitization logic.
- `SignalingValidator.kt`: Enforces role-based filtering and Identity Locking (R982).
- `CommunicationManager.kt`: Implements socket listeners and applies validation logic to incoming traffic.
- `LogManager.kt`: Attaches dual-metric anchors to forensic logs (Issue #325).
