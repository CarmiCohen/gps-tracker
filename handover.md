# Forensic Handover - GPS Tracker (v8.9.2)

## **Current State: v8.9.2 (Branding & Forensic Finalization)**
The project has successfully transitioned to the **v8.9.2** baseline. All core background services, repositories, and UI controllers have been synchronized. Forensic integrity for high-fidelity event reconstruction is fully implemented and verified across the module boundary.

### **Forensic State Audit (This Session)**
1.  **Issue 182: Global Version Synchronization**
    *   **Services**: `TrackerService.kt`, `ViewerService.kt`, and `BaseMonitorService.kt` source headers bumped to v8.9.2.
    *   **Core Logic**: `LogManager.kt`, `GpsApplication.kt`, and `MainViewModel.kt` synchronized.
    *   **Data Layer**: `SettingsRepository.kt` and `Database.kt` (Room Migration v33) updated to the latest baseline.
    *   **Emission**: `SyncManager` and `LogManager` now utilize `BuildConfig.VERSION_NAME` for version injection, eliminating legacy hardcoded strings.

2.  **Settings Persistence Recovery & Hardening**
    *   **Fix**: Resolved critical compilation errors in `SettingsRepository.kt` caused by field name desync between the Kotlin layer and `app_settings.proto`.
    *   **Field Mapping**: Corrected `lux` (Kotlin) to `value_lux` (Proto) and ensured `is_cooling_mode_active` and `is_storage_critical` are properly persisted in `DataStore`.
    *   **Integrity**: Verified `loadTrackerState` and `saveTrackerState` map 100% of forensic SIT and OS-restriction fields.

3.  **Role Forensic Parity (Issue 185)**
    *   **Implementation**: `ViewerService.kt` fully implements the `localProcessorListener`. Viewers now locally persist peer trails and engine logs, ensuring discrete events (Jumps, SIT triggers) are reconstructed on the Viewer's map and database.

### **Forensic Marker Verification**
*   **verticalVelocity**: Preserved across the telemetry pipeline.
*   **SIT Metrics**: `sitBaro`, `sitTilt`, and `sitShock` are recorded in `HistoryEntity` and `PendingStatusEntity`.
*   **Visual Jumps**: Tracker-calculated jumps are explicitly latched and recorded via `ServiceForensicUseCase` on both devices.

## **Resumption Context (Prioritized Open Items)**
| ID | Rank | Task | Forensic Context |
|:---|:---|:---|:---|
| **133** | 9 | **Xiaomi Background Stability** | Mandatory physical testing of 10Hz persistence and Autostart gating effectiveness. |
| **183** | 7 | **Physical Asset Purge** | REDUNDANCY DETECTED: Files `z.xml`, `z2.xml`, and `splash.xml` still exist in `res/mipmap-anydpi-v26` despite being marked FIXED in docs. Delete them. |
| **184** | 5 | **Muzzle Window Validation** | Verify if 500ms `MUZZLE_WINDOW_DURATION_MS` prevents false-positives on devices with high I/O latency. |
| **180** | 5 | **Dashboard SIT Audit** | Sanity check the display of SIT metrics on the analytical ribbons in Viewer mode. |

## **Resumption Command**
"Resuming on v8.9.2 baseline. Prioritize physical asset removal (Issue 183) followed by Xiaomi hardware verification (Issue 133)."

**Status:** ARCHITECTURALLY ALIGNED. FORENSICALLY COMPLETE. SETTINGS HARDENED.
