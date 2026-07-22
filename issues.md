# Project Issues & Hardening Tracking (July.22.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 316 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #122: SIT Propagation Depth**: Forensic fields are now 12+ deep (sitVz, snrIdx, etc.). Ensure relay-server (external) can handle this expanded JSON payload without dropping fields.
*   **Issue #121: Provider Latency**: The use of `Provider<T>` to resolve circular dependencies between `LogManager` and `ConnectivitySuite` introduces a small overhead. Monitor call-site performance.
*   **Issue #120b: Budget Hardware Initialization Spikes**: Budget devices (A15) remain sensitive to Main-thread blocking. Future initialization "cascades" must remain off-loaded.
*   **Issue #119: Boot Persistence Integrity**: The `isSystemActive` flag is critical for UX. If DataStore fails during a crash, service revival may fail.
*   **Issue #116: Foreground Service Notification Latency**: Throttling updates to 30s (R993) reduces UI granularity. This is a design choice to reduce overhead but may be perceived as lag.
*   **Issue #114: Monotonic/Wall-Clock Desync**: Extreme system clock manipulation while the app is killed will result in intentional timeline gaps. This is an accepted forensic trade-off.

---

## 🔴 Open Issues
### Issue #113: R405c Fallback Efficacy Verification (Samsung A15)
*   **Description**: Perform long-term field testing on SM-A155F to confirm Accelerometer-based pulse prevents OS-level eviction when the Step Detector fails.

---

## 🟢 Recently Resolved Issues (July.22.01)
*   **Issue #120: Hilt Hardening & Dependency Restoration**.
    *   **Resolution**: Systematically added `@Inject` constructors and `@Singleton` annotations to 20+ core components. Resolved circularities using `Provider<T>`.
*   **Issue #118: Forensic Matrix Synchronization**.
    *   **Resolution**: Synchronized `LocationUpdate`, `TrackerStatus`, `HistoryEntity`, and Database `v58` to a unified forensic standard.

## 🟢 Recently Resolved Issues (July.20.07)
*   **Issue #117: ViewerService Compilation Restoration**.
    *   **Resolution**: Corrected variable names in `ViewerService.evaluateAlarmsInternal` to match `RemoteHandler` implementation.
*   **Issue #107: Step Detector Hardware Registration Hardening**.
    *   **Resolution**: Implemented full permission lifecycle (Manifest entry + `MainAppContent` runtime request + `DiagnosticsScreen` health check) for `ACTIVITY_RECOGNITION`.
*   **Issue #114: Monotonic Timeline Boundary Audit**.
    *   **Resolution**: Audited `TelemetryAggregator.kt` backfill logic. Verified `MAX_BACKFILL_POINTS` (1000) cap provides robust protection against memory exhaustion.
*   **Issue #115: Startup Scope Hardening (GlobalScope Removal)**.
    *   **Resolution**: Migrated `osmdroid` and `WorkManager` setup to a managed `@ApplicationScope`.
*   **Issue #109 & #111: Startup Performance Hardening**.
    *   **Resolution**: Offloaded I/O tasks to `Dispatchers.IO` and decoupled pruning from the UI path.
*   **Issue #110 & #112: mbrainSDK Log Suppression**.
    *   **Resolution**: Filtered vendor-specific `libmbrainSDK` noise in the global `Timber` tree.
*   **Issue #108: MaintenanceWorker Startup Recovery Race**.
    *   **Resolution**: Implemented immediate timestamp refresh in service `onCreate()` methods.
