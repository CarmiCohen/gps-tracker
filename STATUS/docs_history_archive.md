# Project History Archive (Legacy Records)

This document contains historical version records and resolutions for v8.9.x and older.

## 1. DI Purge & Global Startup Maintenance (July.22.09)
*   **Issue #126b**: DI Leftover Purge. Physically decommissioned `AppContainer.kt` and `MainViewModelFactory.kt`. Scrubbed all legacy comments and historical references from the primary application entry points and service infrastructure to ensure architectural purity.
*   **Issue #113**: Samsung A15 Fallback Hardening (R405c). Upgraded the Accelerometer "Stay-Alive Pulse" to perform a hardware "poke" via `SystemMonitor.acquireWakeLock(force = true)` every 10 seconds when the hardware Step Detector is missing or registration fails. This ensures budget hardware maintains process priority.
*   **Issue #104b**: Global Startup Maintenance Authority. Extended the proactive `deepPruneLogs` operation (Requirement R104) to the background service layer via `BaseMonitorService.kt`. This ensures that even background-initiated startups benefit from log pruning, preventing I/O bottlenecks on budget hardware.
*   **Issue #121**: Provider Latency Optimization. Implemented lazy thread-safe caching of the `ConnectivitySuite` instance in `LogManager.kt` to reduce the overhead of circularity resolution via `Provider<T>` in high-frequency logging paths.

## 2. Documentation Integrity & Version Sync (July.22.06)
*   **Issue #126**: Complete Hilt Migration and Decommission. Finalized the Hilt transition by decommissioning legacy `AppContainer.kt` and `MainViewModelFactory.kt`. Conducted code-wide audit to confirm zero remaining references to manual DI container.

## 3. Documentation Integrity & Version Sync (July.22.05)
*   **Issue #512**: Documentation Integrity Audit. Synchronized `SOT_MASTER_REQUIREMENTS.md`, `VERIFICATION_MANIFEST.md`, `QA_VALIDATION_STATUS.md`, and `README.md` to the `July.22.05` baseline. Harmonized implementation statuses across the audit trail.

## 4. Hilt Migration & Hardening (July.22.04)
*   **Issue #124**: Hilt Migration Completion & AppContainer Decommissioning. Fully migrated all services, activities, repositories, and managers to Hilt. Removed manual DI logic from `GpsApplication` and `BaseMonitorService`.
*   **Issue #511**: DataStore Singleton Violation. Refactored `SettingsRepository` to use a single `DataStore` instance via `Context` extension delegate.

## 5. Boot Persistence & Worker Hardening (July.22.02)
*   **Issue #119**: Boot Persistence Integrity. Hardened the system revival master switch. `BootServiceStartWorker` and `MaintenanceWorker` now rigorously check `isSystemActive` before attempting to start or recover foreground services.
*   **Issue #120**: Hilt Worker Hardening. Systematic conversion of all background workers to `@HiltWorker` with assisted injection to ensure dependency integrity across process boundaries.
*   **Issue #122**: SIT Propagation Depth & Relay Audit. Hardened the binary and JSON telemetry pipelines. Updated `RealtimeStatus` Protobuf and `TrackerStatus` mapping to include all 15+ forensic parameters. Verified relay-server schema-agnostic compatibility.

## 6. Hilt Hardening & Dependency Restoration (July.22.01)
*   **Issue #120**: Hilt Hardening & Dependency Restoration. Remediated the DI vacuum left by the removal of legacy fragments. Systematically implemented `@Inject` constructors and `@Singleton` annotations across 20+ core repositories, UseCases, and managers. 
*   **Issue #121**: Circular Dependency Resolution. Resolved the `LogManager` <-> `ConnectivitySuite` circularity using Dagger `Provider<T>` at the `LogManager` injection site, ensuring graph stability.
*   **Issue #118**: Forensic Matrix Synchronization. Achieved full parity between engine models (`LocationUpdate`), UI models (`TrackerStatus`, `ConnectionPoint`), and Persistence (`HistoryEntity`, Database `v58`) for all 10+ forensic SIT and Index parameters.
*   **Issue #123**: Version Consolidation. Updated all version references to `July.22.01` and synchronized `app/build.gradle`.

## 7. Forensic Hardening & Permission Lifecycle (July.20.07)
*   **Issue #117**: ViewerService Compilation Restoration. Corrected variable names in `ViewerService.evaluateAlarmsInternal` to match `RemoteHandler` implementation.
*   **Issue #107**: Step Detector Hardware Registration Hardening. Resolved missing `ACTIVITY_RECOGNITION` permission preventions for hardware Step Detector on API 29+. Implemented full permission lifecycle.
*   **Issue #114**: Monotonic Timeline Boundary Audit. Verified `TelemetryAggregator.kt` backfill logic. Protected against memory exhaustion during extreme system clock drifts (1000-point cap).
*   **Issue #115**: Startup Scope Hardening. Migrated `osmdroid` and `WorkManager` setup to a managed `@ApplicationScope`.
*   **Issue #109 & #111**: Startup Performance Hardening. Offloaded I/O tasks and decoupled pruning from the UI path.
*   **Issue #110 & #112**: mbrainSDK Log Suppression. Filtered vendor-specific `libmbrainSDK` noise in the global `Timber` tree.
*   **Issue #108**: MaintenanceWorker Startup Recovery Race. Implemented immediate timestamp refresh in service `onCreate()` methods.

## 8. Forensic Ribbon Continuity (July.20.06)
*   **Issue #106**: Unified Forensic Ribbon Continuity (R106). Implemented unified rendering for ribbons across all scales with "Black Gap" visualization for missing data.

## 9. Forensic Continuity & Startup Hardening (July.20.01)
*   **Issue #105**: Forensic Ribbon Continuity Verification. Remediated "monotonic reset" bug. Reconstructed monotonic timeline on startup using persisted drift references.
*   **Issue #104**: Startup ANR Hardening (Proactive Log Pruning). Integrated proactive pruning into `MainViewModel` startup sequence.

## 10. Temporal Integrity & Persistence (July.19.04)
*   **Issue #103**: Drift Reference Persistence. Ensured `clockDriftRef` survives process death.
*   **Issue #102**: Temporal Forensic Integrity. Standardized dual-time strategy (`rt` vs `ts`) across the system.

## 11. Historical Hardening Era (v9.0.4 - v9.3.0)
*   **Issue #049**: Corrected GlobalStatusBar mapping to use mode-aware location context (v9.2.6).
*   **Issue #044**: Standardized HUD status badges to reflect local device health (v9.2.3).
*   **Issue #030**: Proto Schema Duplication (R973). Consolidated all schemas into `app/src/main/proto` (v9.3.0).
*   **Issue #400**: Map Metadata Alignment (R400). Re-anchored Bayesian Uncertainty status messages to bottom-center (v9.3.0).
*   **Issue #326**: Intelligent Uncertainty UX. Enriched Location Pending state with reasons (v9.2.2).
*   **Issue #018**: Stationary Anchor Hard-Lock. Implemented coordinate clamping in `LocationProcessor.kt` (v9.2.1).
*   **Issue #048**: Viewer HUD Line Grayout. Differentiated Telemetry Age from GPS Age in status rows (v9.2.0).
*   **Issue #029**: Viewer Status Line Restoration. Propagated local telemetry to repository in Viewer mode (v9.0.3).

## 12. Early Hardening Resolutions (v8.9.65 - v9.1.7)
*   **Issue #041**: Identity Sanitization Hardening. Implemented R975 (Regex validation) (v8.9.99).
*   **Issue #005**: Log Spillage Hardening. Static user agent and manual storage paths for osmdroid (v8.9.91).
*   **Issue #014**: System-Wide Type Safety. Standardized telemetry fields to `Double` (v9.1.7).
*   **Issue #043**: Migration Integrity Audit (R985). Verified and hardened v53 Room database migration (v9.1.7).

## Legacy Milestone: v8.9.0 (Foundational Hardening)
- Established the dual-time strategy (Monotonic + Wall Clock).
- Initial implementation of the Jump Engine and spatial uncertainty scoring.
- Baseline power optimization for stationary devices.

## Legacy Milestone: v8.8.4 (Persistence Baseline)
- Migrated from SharedPreferences to early DataStore implementation.
- Standardized the binary Protobuf telemetry format.
