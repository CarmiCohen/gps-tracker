# Resolution Archive

## Sep.14.00
*   **Build Restoration (#1024)**: Resolved `Unresolved reference` errors in `TrackerService` and `ViewerService` by re-consolidating `ConnectivityEvent` as a top-level sealed class in `ConnectivitySuite.kt`. (R-ID 321).
*   **Forensic Visibility Enhancement (#1019)**: Integrated `SignalingValidator.getDropReason` into `ConnectivitySuite` logs. Rejection warnings now include descriptive reasons (e.g., "Unauthorized Viewer", "Echo suppression") to enable faster triage of signaling stalls. (R-ID 320).
*   **Identity Sync Hardening (#1021)**: Verified 60s periodic identity synchronization in `ConnectivitySuite`. Ensured `force=true` flag correctly triggers re-joining logic in `CommunicationManager` to maintain relay room state. (R254).

## Sep.13.30
*   **Map Component Restoration (#1023)**: Restored ScaleBarOverlay and fixed settings wheel occlusion on Samsung A15. (R-ID 318).
*   **Stability Audit (#1017)**: Verified role transition safety and implemented R-ID 316/317 for state continuity.
*   **Version Catalog Migration (#1018)**: Centralized dependencies in `libs.versions.toml`.

... (Previous entries preserved) ...
