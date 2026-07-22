# Issue #120: Hilt Worker Hardening & Dependency Restoration

## Status: Resolved (July.22.02)
## Requirement: R120b

### Description
Background Workers managed by `WorkManager` require `@HiltWorker` and `AssistedInjection` to receive dependencies correctly across process boundaries. Without this, workers like `BootServiceStartWorker` fail to access the `SettingsRepository`, preventing system revival.

### Resolution
- **Worker Conversion**: Converted `BootServiceStartWorker`, `MaintenanceWorker`, and `LogCleanupWorker` to use `@HiltWorker`.
- **Factory Integration**: Updated `GpsApplication` to use `HiltWorkerFactory` via `Configuration.Provider`.
- **Dependency Restoration**: Re-implemented `@Inject` constructors for 20+ core repositories that were previously orphaned during the removal of the manual `AppContainer`.

### Verification
- [x] Verified that `MaintenanceWorker` correctly receives injected repositories.
- [x] Verified system revival logic triggers without `IllegalStateException`.
