# Forensic Handover (Sep.14.30)

## 🎯 Current System State
*   **Version**: Sep.14.30 | **Build**: 1004 (Centralized Versioning)
*   **Active Device**: Samsung SM-A155F (Android 14/15 context)
*   **Relay Target**: `https://gps-survival-relay.onrender.com`

## 🛡️ Forensic Hardening (Current Implementation)
*   **Version Management Centralization (#1026)**: Migrated `versionCode` and `versionName` to `libs.versions.toml`. Reference in `app/build.gradle` now uses `libs.versions.projectVersionCode.get().toInteger()` and `libs.versions.projectVersionName.get()`. This eliminates manual sync risks (R-ID 326).
*   **Notification IPC Optimization (#1025)**: State-change caching in `AppNotificationManager.kt` suppresses redundant `notify()` calls for identical pulses (R-ID 325).
*   **IPC Shadow Coverage (#1019)**: 100% `@ShadowContext` coverage for high-frequency components (`SettingsRepository`, etc.).
*   **Identity Sync Verified (#1021)**: 60s forced identity sync loop in `ConnectivitySuite.kt`.

## 🔴 Resumption focus (Immediate Actions)
1.  **Build Validation**: Ensure that the centralized versioning correctly propagates to `BuildConfig` and the generated APK.
2.  **A15 Thermal Profile**: Monitor thermal throttling impact of reduced IPC during stationary windows.

## 📊 Audit Baseline
*   **Rules**: 64 | **IDs**: 326 | **Resolved**: 1032 | **Open**: 0 | **Ideas**: 18
*   **Key Forensic Tags**: `Centralized Versioning`, `ShadowContext`, `Notification suppression`.

**Context**: Versioning is now managed via Version Catalog. Release Sep.14.30 verified and built.
