# Hardening Resolution Archive (Sep.13.30)

## 🟢 Resolved in Sep.13.30
*   **Map Component Restoration (#1023)**:
    *   **Remediation**: Restored the map scale bar by adding `ScaleBarOverlay` to the `MapView` stack. Fixed the "disappearing" settings wheel by adjusting its top padding in portrait mode (110.dp) to prevent occlusion by the header and status bars. Verified all tool functions (Zoom, Centering, Geofence toggles) are functional via `MapViewState` propagation. (R-ID 318).

## 🟢 Resolved in Sep.12.46
*   **Stability Audit & Role Transition Verification (#1017)**:
    *   **Remediation**: Performed a forensic audit of the role transition logic. Verified mutual exclusivity in `MainActivity`, state sanitation in `SessionUseCase` and `TelemetryRepository`, and hardened resumption in `ViewerService` and `TrackerService` (R-ID 314). Implemented **R-ID 316** (Accuracy Window Integrity) and **R-ID 317** (History Manager Continuity).
*   **Version Centralization (#1018)**:
    *   **Remediation**: Implemented Gradle Version Catalog (`libs.versions.toml`) to centralize dependency and plugin management. Migrated `:app` and `:core:engine` to use the catalog, eliminating hardcoded version strings and ensuring build consistency (Simplification Idea #18).

## 🟢 Resolved in Sep.12.45
*   **Connectivity & Telemetry Audit (#1016)**:
    *   **Remediation**: Verified dual-device handshake stability and tick-loop initiation between S21FE and A15. Forensic code audit confirmed that the hardening in #1015 (R-ID 314) successfully prevents session stalls by proactively initiating operational loops on every peer pulse. Telemetry parity and coordinate synchronization confirmed.
*   **Signaling Resumption Hardening (#1015)**:
    *   **Remediation**: Hardened `ViewerService` and `TrackerService` to ensure the operational tick loop is initiated on *every* peer pulse if the job is not active. (R-ID 314).

---
*For older records, see historical git logs. (vSep.13.30)*
