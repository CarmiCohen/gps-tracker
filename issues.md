# Project Issues & Hardening Tracking (July.22.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 322 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #121: Provider Latency**: Circularity resolution via `Provider<T>` is stable but introduces minor lookup overhead in `LogManager`.
*   **Issue #120b: Budget Hardware Initialization Spikes**: Budget devices (A15) remain sensitive. The 500ms staggered startup is critical.

---

## 🔴 Open Issues
### Issue #113: R405c Fallback Efficacy Verification (Samsung A15)
*   **Description**: Perform long-term field testing on SM-A155F to confirm Accelerometer-based pulse prevents OS-level eviction.

---

## 🟢 Recently Resolved Issues (July.22.03)
*   **Issue #511: DataStore Singleton Violation**.
    *   **Resolution**: Refactored `SettingsRepository` to use a single `DataStore` instance via `Context` extension delegate. This ensures that even during the Hilt transition, multiple repository instances share the same underlying `DataStore` connection, preventing `IllegalStateException`.

## 🟢 Recently Resolved Issues (July.22.02)
*   **Issue #119: Boot Persistence Integrity**.
    *   **Resolution**: Hardened the system revival master switch. `BootServiceStartWorker` and `MaintenanceWorker` now rigorously check `isSystemActive` before attempting to start or recover foreground services.
*   **Issue #120: Hilt Worker Hardening**.
    *   **Resolution**: Systematic conversion of all background workers (`MaintenanceWorker`, `BootServiceStartWorker`) to `@HiltWorker` with assisted injection to ensure dependency integrity across process boundaries.
*   **Issue #122: SIT Propagation Depth & Relay Audit**.
    *   **Resolution**: Hardened the binary and JSON telemetry pipelines. Updated `RealtimeStatus` Protobuf and `TrackerStatus` mapping to include all 15+ forensic parameters (tiltIdx, baroIdx, sitBaro, sitTilt, sitShock, etc.). Verified relay-server schema-agnostic compatibility.

## 🟢 Recently Resolved Issues (July.22.01)
*   **Issue #118: Final Forensic Parity Synchronization**.
    *   **Resolution**: Standardized 15+ forensic parameters (`snrIdx`, `noiseIdx`, `luxIdx`, `vibeIdx`, `liftIdx`, `proxIdx`, and SIT fields) across `LocationUpdate`, `TrackerStatus`, `SystemHealthState`, `HistoryEntity`, and Protobuf (`RealtimeStatus`).
*   **Issue #123: Version Consolidation**.
    *   **Resolution**: Updated all version references to `July.22.01` and synchronized `app/build.gradle`.

## 🟢 Recently Resolved Issues (July.20.07)
*   **Issue #117: ViewerService Compilation Restoration**.
*   **Issue #107: Step Detector Hardware Registration Hardening**.
*   **Issue #114: Monotonic Timeline Boundary Audit**.
*   **Issue #115: Startup Scope Hardening (GlobalScope Removal)**.
