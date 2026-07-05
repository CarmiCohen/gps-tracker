# Forensic Handover - v8.9.99 (Identity Sanitization COMPLETE)

## 📌 Status: Stable / Build PASS / Identity Hardened
This cycle resolves the critical identity corruption failure (Issue #041) where malformed pulse data injected shell commands into system identities.

### 🟢 Completed: Issue #041 (Identity Sanitization)
*   **Root Cause Remediation**: Established a strict alphanumeric contract in `SignalingConstants.kt` via Regex (`^[a-zA-Z0-9_-]{1,32}$`).
*   **Automatic Recovery**: Added a DataStore migration hook in `SettingsRepository.kt` that purges corrupted IDs from storage and resets them to defaults on startup.
*   **Service Hardening**: Pulse handlers in `TrackerService` and `ViewerService` now reject malformed peer IDs.
*   **Network Shielding**: `AppNetworkManager` refuses to connect to the relay with unsanitized identities.

### 🟢 Completed: Issue #027 (Identity Persistence)
*   **Resolved**: Reinforced uniqueness validation to prevent role cross-contamination.

### 🛠 Instructions for Resumption
1.  **Verification**: Deploy **v8.9.99**. The app will automatically clear the "pm clear" ID upon launch.
2.  **Validation**: Verify that the Tracker can now communicate with the Viewer using a valid alphanumeric ID.
3.  **UI Audit**: Check `issues.md` #042 regarding the lack of a "Sanitization Alert" notification for the user when a reset occurs.
