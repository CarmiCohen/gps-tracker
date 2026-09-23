# 🏛️ Resolution Archive - Sep.23.70

## 🏁 Issue #1230: Shared Storage Key Leakage & Cross-Role State Corruption
*   **Resolved**: Sep.23.70
*   **Root Cause**: `TrackerService` and `ViewerService` shared the same flat keys in `DataStore` for high-frequency logic state (ticks, accuracy, alarm latches). Switching roles caused state "leakage" where the previous role's state corrupted the current role's baseline.
*   **Remediation**:
    *   Expanded `app_settings.proto` with type-specific namespaced maps (`role_longs`, `role_doubles`, `role_bools`, `role_ints`, `role_strings`, `role_states`).
    *   Refactored `SettingsRepository` and `MainRepository` to intercept keys starting with `"T_"` or `"V_"` and route them to these isolated map partitions.
    *   Refactored `MainRepository.lastAlarmAckTsFlow` to use `flatMapLatest` on `appModeFlow`, ensuring the UI observes the correct role partition reactively.
    *   Updated `AlertUseCase` and `CommandRouter` to inject `ConfigManager` and apply role-based prefixes (`"T_"` or `"V_"`) to alarm acknowledgment timestamps during user interactions.
    *   Updated `TrackerService` and `ViewerService` to apply role prefixes to all high-frequency logic state keys.
    *   Updated `AppAlarmManager` to be role-prefixed, ensuring geofence debounce and power latches are isolated per role.
    *   Updated `RemoteStatusRepository` to use the Viewer prefix (`"V_"`) for remote peer telemetry, preventing overwrites of local device state.
    *   Updated `MaintenanceWorker` to use role-based prefixes when auditing service vitality.
*   **R-ID**: 453

## 🏁 Issue #1236: Race Conditions during Asynchronous Initialization
*   **Resolved**: Sep.23.70
*   **Root Cause**: The background services performed asynchronous initialization within `onCreate`, but tick and heartbeat loops could be triggered prematurely by external pulses, leading to execution against unhydrated state.
*   **Remediation**:
    *   Introduced `initializationDeferred: CompletableDeferred<Unit>` in `BaseMonitorService`.
    *   Updated `BaseMonitorService.onCreate` to complete the deferred after `onServiceInitialize()` finishes.
    *   Refactored `startTickLoop()`, `startHeartbeatLoop()`, and forensic sampling loops to `await()` initialization.
*   **R-ID**: 424

... [Previous entries preserved] ...
