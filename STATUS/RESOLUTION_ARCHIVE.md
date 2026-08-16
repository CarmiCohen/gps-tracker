# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 628**

## 54. Map Hydration & Sensor Hardening (Aug.16.14)
*   **Issue #186: Gated Sensor Startup**.
    - **Resolution**: Implemented a deferred sensor registration mechanism in `AppSensorManager`. High-frequency sensors (Accelerometer, Linear Accel) are now gated by a 2000ms settling delay (`SENSOR_SETTLING_DELAY_MS`) upon service start. This prevents IPC/Binder saturation during the critical first 2 seconds of Tracker/Viewer entry, ensuring the UI thread has exclusive priority for Map hydration and Dashboard composition. (R186)
*   **Issue #185: Startup ANR during Map Hydration**.
    - **Resolution**: Eliminated main-thread saturation by offloading trail segment hashing and simplification to background threads. `MapTrailSegment` now carries a pre-computed `checksum` calculated in the `MainViewModel`, allowing `MapOverlayManager.updateTrails` to perform O(1) change detection. This ensures the UI thread remains responsive during hydration of the initial 2,000 points. (R185)
*   **Issue #184: Stress Test IO Race Condition**.
    - **Resolution**: Hardened the forensic stress test `ioJob` in `TrackerService` to use unique timestamps in filenames and internal try-catch blocks. This prevents `FileNotFoundException` and service crashes during high-frequency disk contention on restricted hardware. (R184)
*   **Issue #183: Startup OOM in Tracker Mode**.
    - **Resolution**: Reduced trail and violation retrieval limits from 10,000 to 2,000 in `Database.kt` to align with the memory budget of API 35/36 emulator environments. (R183)

## 53. Map & Startup Hardening (Aug.16.00)
*   **Issue #182: Startup ANR & GC Thrashing**.
    - **Resolution**: Eliminated the massive allocation churn in the map rendering pipeline. `MapOverlayManager` now reuses cached `GeoPoint` objects within `TrailPoint` and `ViolationPoint`, preventing the repeated mapping and list creation that caused 50MB+ GC cycles and Startup ANRs. Increased `STARTUP_SETTLING_DELAY_MS` to 10s and deferred `GpsApplication` osmdroid setup to clear the main-thread critical path during frame rendering. (R182)
*   **Issue #181: DeadSystemException on Startup**.
    - **Resolution**: Addressed Binder exhaustion and system-server stalls by increasing the startup settling delay to 10,000ms. This ensures that heavy database migrations (v56-v71) and UI hydration complete before the high-frequency (100Hz) telemetry engine initiates, stabilizing the environment on resource-constrained devices. (R181)

## 52. Forensic Stability & Memory Hardening (Aug.15.01)
*   **Issue #180: SQLite UNIQUE constraint failure on regular logs**.
    - **Resolution**: Transitioned the `UNIQUE` constraint in the `logs` table from the broad `(type, timestamp, spillIdx)` composite to the specific `localId` column (Migration 70). Updated `LogDao` to utilize `OnConflictStrategy.IGNORE`. This resolved the critical regression where non-forensic logs (e.g., heartbeats) sharing the same millisecond timestamp caused `SQLiteConstraintException` and database lockups. (R180)
*   **Issue #179: Persistent Heap Exhaustion & ANR at 100Hz**.
    - **Resolution**: Eliminated the ~120MB/s allocation churn that overwhelmed the heap during 100Hz telemetry. Implemented a throttled **2Hz UI History Emitter** in `MainRepository` using a `ConcurrentLinkedQueue` to decouple high-frequency engine ticks from UI Flow emissions. Replaced memory-intensive manual signature checks in `LogRepository` (which previously created massive `HashSet` objects) with database-level unique indexing and `IGNORE` logic. (R179)
*   **Issue #178: Sustained 100Hz Heap Exhaustion & ANR**.
    - **Resolution**: Optimized the UI data pipeline by gating heavy log mapping operations by visibility. The `eventLogsFlow` in `MainViewModel` now yields `emptyList()` when the log viewer is closed, preventing redundant list-copying and string-formatting churn. Tightened the forensic signature lookback window to a fixed 10-minute period (`FORENSIC_SIGNATURE_LOOKBACK_MS`) to maintain predictable memory usage. (R178)

... (rest of archive)
