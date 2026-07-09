# Project History & Versioning (v9.3.6)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## v9.3.6
- **Full Hilt Migration (Issue #058 / R978)**: Completed the architectural refactor of the background service layer. All 11 core components are now field-injected via Hilt into a consolidated `BaseMonitorService`.
- **EntryPoint Accessor Elimination**: Removed legacy `EntryPointAccessors` from `TrackerService`, `ViewerService`, `WatchdogReceiver`, and `GpsApplication`, moving the project toward a pure DI-driven lifecycle.
- **Service Lifecycle Hardening**: Standardized initialization and cleanup patterns (Listeners/Initialize) across all role-based services to prevent race conditions during cold starts.
- **Speed Zeroing Verification (#047 / R987)**: Confirmed that Viewer HUD speed drops to 0.0 km/h immediately upon Tracker GPS loss, preventing stale speed reporting.
- **State Sync Audit (#046 / R986)**: Verified simultaneous state transitions (MOVING/PARKING) between Tracker and Viewer HUDs under high-frequency telemetry load.

## v9.3.4
- **Identity Rejection Feedback (Issue #039 / R977)**: Implemented explicit UI and forensic log feedback for identity collisions and validation failures.

## v9.3.2
- **Sanitization Visibility (Issue #042 / R976)**: Implemented `identitySanitizedFlow` and AlertDialog notifications when Tracker/Viewer IDs are automatically reset during migration.

## v9.3.1
- **Proto Schema Consolidation (Issue #030 / R973)**: Resolved technical debt by consolidating all Protobuf schemas into `app/src/main/proto`.
- **Map Metadata Alignment (Issue #400 / R400)**: Re-anchored Bayesian Uncertainty status messages to the bottom-center cluster.
- **Status Integrity Restoration (#054, #055)**: Recovered 185 lost legacy resolutions.

## v9.2.9
- **Screen-Off Optimization (R994)**: Implemented dynamic GPS down-sampling (5000ms) when the device screen is off.

## v9.2.8
- **Notification Throttling (R993)**: Implemented dual-rate notification refreshes in `BaseMonitorService` (1s active / 10s background) to balance efficiency and visibility.

## v9.2.7
- **HUD Capability Grouping (R960)**: `GlobalStatusBar` groups fundamental local hardware indicators (Battery, Storage, Temp) for consistent health reporting.

## v9.2.6
- **HUD Context Mapping (R049)**: Corrected `GlobalStatusBar` binding logic for mode-aware telemetry.

## v9.2.3
- **HUD Health Standardization (R991)**: Standardized top-level HUD status badges to reflect physical device health.

## v9.2.2
- **Intelligent Uncertainty UX (R326)**: Enriched Location Pending state with specific reasons (GPS_GAP, JAMMER) propagated to the HUD.

## v9.2.0
- **HUD Freshness Duality (Issue #048 / R989)**: Differentiated Telemetry Age from GPS Age; HUD line elements now remain colorized if the data link is active, even if GPS is lost.

## v9.1.9
- **Binary Parity Synchronization (Issue #051 / R988)**: Resolved gaps in the binary telemetry contract to ensure all engine fields are preserved in `location_relay_bin` pulses.

## v9.1.7
- **Migration Hardening (Issue #043 / R985)**: Verified and hardened the v53 Room database migration with default values for new forensic columns.
- **System-Wide Type Safety (R014)**: Standardized all telemetry fields to `Double` across the engine and app layer.

---
*Full history available in the docs_history_archive.md.*
