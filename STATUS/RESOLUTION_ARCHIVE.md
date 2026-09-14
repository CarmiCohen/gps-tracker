# Resolution Archive

## Sep.14.30
*   **Version Management Centralization (#1026)**: Migrated `versionCode` and `versionName` declarations to `gradle/libs.versions.toml`. This ensures single-source-of-truth for project versioning, eliminating manual synchronization overhead and risk of version mismatch across documentation and build artifacts (Idea #18, R-ID 326).

## Sep.14.20
*   **Notification IPC Optimization (#1025)**: Implemented state-change caching in `AppNotificationManager.kt`. Suppresses redundant `notify()` calls when pulse content is identical, reducing IPC overhead and framework diagnostic noise on budget hardware (R-ID 325).

## Sep.14.10
*   **IPC Shadow Coverage (#1019)**: Migrated all remaining data-path and configuration repositories (`SettingsRepository`, `AppAlarmManager`, `ConfigManager`, `ForensicSpillBuffer`, `HistoryManager`) to `@ShadowContext`. This ensures 100% `ShadowCache` coverage for package name lookups, mitigating high-frequency IPC overhead during system service interactions (R-ID 324).
*   **Identity Sync Verification (#1021)**: Verified the 60s forced sync loop in `ConnectivitySuite.kt`. Confirmed it successfully manages relay room occupancy during stationary periods by handling server-side socket timeouts through proactive identity re-joining.
*   **Forensic Drop Analysis (#1020)**: Refactored signaling pipeline to ensure authoritative forensic visibility. Simplified `CommunicationManager.kt` to a transport-only role and centralized validation in `ConnectivitySuite.kt`. Rejection warnings now reliably log descriptive reasons (e.g., "Echo suppression", "Unauthorized Viewer") for all signaling packets. (R-ID 323).
*   **Cleanup Logic Simplification (#1022)**: Converted hardware unregistration in `ManagedHardware.kt` to fire-and-forget asynchronous mode. Removed `CountDownLatch` and `Tasks.await` blocks to ensure rapid teardown and prevent synchronous stalls that delayed signaling disconnects during mode transitions. (Idea #17, R-ID 322).

## Sep.14.00
*   **Build Restoration (#1024)**: Resolved `Unresolved reference` errors in `TrackerService` and `ViewerService` by re-consolidated `ConnectivityEvent` as a top-level sealed class in `ConnectivitySuite.kt`. (R-ID 321).
*   **Forensic Visibility Enhancement (#1019)**: Integrated `SignalingValidator.getDropReason` into `ConnectivitySuite` logs. Rejection warnings now include descriptive reasons (e.g., "Unauthorized Viewer", "Echo suppression") to enable faster triage of signaling stalls. (R-ID 320).

## Sep.13.30
*   **Map Component Restoration (#1023)**: Restored ScaleBarOverlay and fixed settings wheel occlusion on Samsung A15. (R-ID 318).
*   **Stability Audit (#1017)**: Verified role transition safety and implemented R-ID 316/317 for state continuity.
*   **Version Catalog Migration (#1018)**: Centralized dependencies in `libs.versions.toml`.

... (Previous entries preserved) ...
