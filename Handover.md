# Forensic Handover - v9.3.1 (Sanitization Visibility)

## 📌 Status: Stable / Build PASS / Release Ready
This cycle implements R976, ensuring visibility when the system automatically resets malformed IDs to maintain storage integrity.

### 🟢 Completed: Issue #042 (Sanitization Visibility)
*   **Persistence Hardening**: Added `identity_sanitized` flag to `AppSettings` Protobuf schema (tag 63).
*   **Migration Awareness**: Updated `identitySanitizationMigration` in `SettingsRepository` to set the flag when purging invalid Tracker/Viewer IDs.
*   **UI Feedback**: Implemented a reactive `AlertDialog` in `MainAppContent` that notifies the user if their IDs were reset, requiring manual acknowledgment.
*   **State Management**: Added `DismissIdentitySanitization` event to `UiEvent` and integrated it through `MainViewModel` and `SettingsRepository`.
*   **Requirement R976**: Formalized the sanitization visibility requirement in `STATUS/requirements_sot.md`.

### 🟢 Pre-existing State: v9.3.0
*   **R400 (Map Metadata Alignment)**: Bayesian uncertainty messages moved to bottom-center.
*   **R994 (Screen-Off Optimization)**: 5s polling throttle when device is locked.

### 🛠 Instructions for Resumption
1.  **Verification of #042**:
    *   Simulate a malformed ID in DataStore (e.g., shell-injected "pm clear" side-effects).
    *   Verify that upon app start, the "Identity Sanitized" dialog appears.
    *   Confirm that clicking "Dismiss" persists the false state and clears the dialog.

---
*Generated for chat resumption. All authoritative documents (SoT, issues, archive) are synchronized.*
