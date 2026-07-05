# Forensic Handover - v8.9.98 (Identity Persistence Hardening)

## 📌 Status: Stable / Build PASS / Identity Hardened
This cycle resolves critical identity persistence failures where Viewer and Tracker IDs could be swapped or cross-contaminated.

### 🟢 Completed: Issue #027 (Identity Persistence)
*   **Root Cause Remediation**: Hardened `MainRepository.saveSettingsBulk` with atomic identity uniqueness validation. The repository now rejects and logs updates that would result in Tracker/Viewer ID collisions.
*   **Persistence Integrity**: Verified that `ViewerService.handleTrackerPulse` correctly routes peer identities to the `TRACKER_ID_KEY` and does not overwrite the local `VIEWER_ID_KEY`.
*   **Role Enforcement**: Aligned identity management with SoT R182, ensuring persistent roles across atomic save cycles.

### 🟢 Completed: Issue #030 (Proto Schema Discrepancy)
*   **Authority Established**: `app/src/main/proto` is now the sole authoritative path for all `.proto` schemas.

### 🟢 Completed: Issue #032 (UI Refresh Consistency)
*   **Staleness Gate**: Implemented `isForensicFresh` logic in `DashboardUseCase`.

### 🟡 Pending Validation
*   **A15 Jitter Verification**: (#036) Confirm state stability on A15 Tracker under clear sky vs. indoor transition.
*   **Identity Soak Test**: Verify that the Viewer identity ("V") remains stable during prolonged tracking sessions with frequent peer reconnects.

### 🛠 Instructions for Resumption
1.  **Identity Audit**: Inspect Logcat for "Identity Collision in saveSettingsBulk" to catch any misconfigured clients attempting to sync invalid IDs.
2.  **Verification**: Deploy **v8.9.98** and verify that changing a Tracker ID in settings does not impact the stored Viewer ID.
