# Forensic Handover - v8.9.87 (Identity & Logcat Hardened)

## 📌 Status: Stable / Build PASS
This cycle focused on high-frequency log spillage remediation and identity logic hardening. repetitive `getPackageName` system logs observed on Samsung G990/A155 have been eliminated, and role-based identity defaults (T/V) are now strictly enforced.

### 🟢 Completed: Issue #005 (Log Spillage Remediation)
*   **Static User Agent**: Replaced dynamic `packageName` call in `GpsApplication.kt` with a static string `GpsTracker/8.9.87` to eliminate log spam triggered by `osmdroid`.
*   **Package Name Caching**: Implemented class-level caching of the package name string across all high-frequency components to prevent repetitive system instrumentation logs:
    *   `MainActivity.kt`: Cached for all intent and permission request builders.
    *   `SystemStatusProvider.kt`: Cached for battery and internet monitoring flows.
    *   `SystemMonitor.kt`: Cached for watchdog alarm scheduling.
    *   `AppNotificationManager.kt`: Cached for persistent and alarm notification updates.
    *   `MaintenanceWorker.kt`: Cached for service recovery intent packaging.
    *   `Utils.kt`: Cached for Xiaomi-specific permission checks.

### 🟢 Completed: Identity & Persistence Hardening
*   **Role-Based Defaults (R182)**: Corrected `SettingsRepository` where `viewerIdFlow` incorrectly defaulted to Tracker ID ("T"). It now correctly uses "V" as the authoritative fallback.
*   **Commit Resilience**: Hardened `SettingsRepository.commitDraftSettings` to perform uniqueness checks using effective IDs (applying defaults). This prevents "Commit Failed" errors and UI reversions caused by collisions on empty Proto fields (e.g., `"" == ""` checks during fresh installs).
*   **Logic Correction**:
    *   Fixed `commitDraftSettings` to properly apply the `draftRelayUrl` (previously recycled the old value).
    *   Fixed `SettingsUseCase.updateViewerId` to target `VIEWER_ID_KEY` (previously incorrectly targeted `TRACKER_ID_KEY`).
*   **Forensic Mapping**: Fixed `getLong` in `SettingsRepository` to return `lastDisconnectionTs` (previously returned connection data).
*   **Type Alignment**: Synchronized `SettingsUseCase` to use `Double` precision for geofence and temperature lookups to match Repository standards.

### 🟢 Documentation & Infrastructure
*   **Requirements SoT**: Updated `requirements_sot.md` (v8.9.87) to include mandatory static User Agent baseline for log spillage remediation.
*   **Issues Archive**: Migrated 28 historical resolved items to `STATUS/issues_archive.md`.
*   **Proto Consolidation**: Verified that `app/src/main/proto/` and `app/src/proto/` schemas are identical. Build is stable; manual deletion of the redundant `app/src/proto` folder is recommended.

### 🟡 Pending Validation
*   **Logcat Audit**: Confirm total silence on G990/A155 regarding `getPackageName` during active map panning and status refreshes.
*   **Identity Persistence**: Confirm custom Viewer IDs persist across sessions and do not revert to "V" or "T" during atomic saves.

### 🛠 Instructions for Resumption
1.  **Environment**: Ensure `G990` or `A155` is connected.
2.  **Verification**: 
    *   Change Viewer ID to "V-Handover" in Settings. 
    *   Close and re-open Settings; verify "V-Handover" persists.
    *   Check logcat for `getPackageName` spam during map interaction.
3.  **Soak Test**: Monitor for `STABILITY GAP` logs over a 24-hour horizon under 10Hz sensor load.
