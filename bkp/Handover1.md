# Forensic Handover - GPS Tracker (v8.8.37)

## **Project Status**
The project is currently at **v8.8.37**. It maintains a high-assurance modular architecture with strict separation between the Android `:app` layer and the pure Kotlin `:core:engine`.

### **Core Accomplishments (v8.8.37)**
1.  **Dead Code Cleanup (Issue 177):** Formally removed `SyncManager.broadcastIntegrityUpdate` and `SyncManager.pushStatusUpdateOnly`. The telemetry pipeline is now consolidated around `pushCurrentStatus`.
2.  **Forensic Parity - verticalVelocity (Issue 178):**
    *   **Engine Integration:** Added `verticalVelocity` to `EngineConnectionPoint` for historical reconstruction.
    *   **Persistence Layer:** Added `verticalVelocity` to `HistoryEntity` (Database.kt) and `ConnectionPoint` (Models.kt).
    *   **Migration Audit:** Hardened `MIGRATION_32_33` to explicitly initialize `verticalVelocity` to 0 for historical data consistency.
    *   **Telemetry Pipeline:** Updated `HistoryManager`, `TrackerService`, `ViewerService`, and `MainRepository` to ensure 1:1 mapping and propagation of `verticalVelocity` and SIT metrics (`sitBaro`, `sitTilt`, `sitShock`) into forensic ribbons.
3.  **RemoteHandler Audit (Issue 179):** Verified 100% field parity in `RemoteHandler.init` and `handleRemoteUpdate`, ensuring Tracker-side SIT events are accurately mirrored on the Viewer.

## **Current Architecture State**
*   **Monotonic Timing:** Enforced via `TimeProvider` across all logic and persistence layers.
*   **Database v33:** Schema is purged of legacy `ver`/`vid` weight; forensic depth is now standardized across `connection_history` and `pending_status_updates`.
*   **Service Stability:** `TrackerService` includes a GPS Stability Audit suite (10Hz tracking) and temporal muzzling for state transition logs.

## **Open Issues (from issues.md)**
| ID | Rank | Task |
|:---|:---|:---|
| 133 | 8 | **Xiaomi Background Stability Test:** Verify 10Hz polling (`HIGH_FREQUENCY_GPS_POLLING_MS`) and `isXiaomiAutostartGranted` effectiveness on physical hardware. |

## **Context for Resumption**
The system is now forensically complete and architecturally lean. The next phase involves physical hardware verification, specifically targeting the Xiaomi background persistence logic implemented in v8.8.36.

### **Files of Interest**
*   `:app/TrackerService.kt` (GPS Stability Audit source)
*   `:core:engine/MainAlarmLogic.kt` (Xiaomi gating logic)
*   `:app/Database.kt` (Migration v33 source of truth)

**Status:** RIGOROUSLY REVIEWED. ARCHITECTURALLY ALIGNED. NO PENDING REGRESSIONS.
