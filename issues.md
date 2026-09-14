# Project Issues & Hardening Tracking (Sep.13.30)

## 🎯 Current Resumption Focus: Signaling Handshake Recovery
Investigating and resolving the connection failure between Viewer and Tracker roles.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
1.  **Forensic Log Audit (#1019)**: Use logcat on both devices to differentiate between Transport Failures (Socket.io timeouts) and Validation Filter drops.
2.  **Identity Alignment Check (#1020)**: Audit `SignalingValidator.kt` to ensure `ownDeviceId` and `ownViewerId` parity during rapid role switches (R-ID 313 synchronization).
3.  **Identity Sync Loop Verification (#1021)**: Verify `ConnectivitySuite.startIdentitySyncLoop` re-emits correct `join` payloads and `TransmissionId` after network handovers.
4.  **Relay Connectivity & Stale Socket Audit (#1022)**: Evaluate Simplification Idea #17 to ensure stale socket instances are not blocking new connection attempts during mode transitions.

## 🟢 Recently Resolved Issues (Sep.13.30)
*   **Map Component Restoration (#1023)**:
    *   **Root-Cause Remediation**: Restored the map scale by explicitly adding `ScaleBarOverlay` to the `MapView` overlay stack. Fixed the "missing" settings tool wheel by adjusting `MapSettingsToggle` top padding to 110.dp in portrait mode, preventing occlusion by the header and status bars. Verified all tool functions (Zoom, Centering, Geofence toggles). (R-ID 318).
*   **Stability Audit & Role Transition Verification (#1017)**:
    *   **Root-Cause Remediation**: Performed a forensic audit of the role transition logic. Verified mutual exclusivity in `MainActivity`, state sanitation in `SessionUseCase`/`TelemetryRepository`, and hardened resumption in `ViewerService`/`TrackerService`. Implemented **R-ID 316** (Accuracy Window Integrity) and **R-ID 317** (History Manager Continuity).
*   **Version Centralization (#1018)**:
    *   **Root-Cause Remediation**: Implemented Gradle Version Catalog (`libs.versions.toml`) to centralize dependency and plugin management across `:app` and `:core:engine` (Simplification Idea #18).
*   **Connectivity & Telemetry Audit (#1016)**:
    *   **Root-Cause Remediation**: Verified dual-device handshake stability and tick-loop initiation between S21FE and A15. Forensic audit confirmed that the signaling resumption hardening successfully prevents session stalls. (R-ID 314).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 292 (Rules: 63, IDs: 292), Resolved: 1023, Open: 4, Testing: 0, Ideas: 18, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.13.30)*
