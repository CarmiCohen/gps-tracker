# Issue #511: DataStore Singleton Violation

## Status: Resolved (July.22.04)
## Requirement: R511

### Description
Jetpack DataStore must be initialized exactly once per process to avoid `IllegalStateException`. In a Hilt-managed application with multiple entry points (Services, Workers, Activities), manual instantiation of DataStore in different classes led to concurrent access violations.

### Resolution
- **Singleton Authority**: Refactored `SettingsRepository` to access DataStore via a `Context.dataStore` extension property delegate, as recommended by Google.
- **Hilt Integration**: The `SettingsRepository` is now injected as a `@Singleton`, ensuring all components share the same DataStore instance through a unified repository.
- **Race Condition Prevention**: Initializing via the property delegate ensures the internal DataStore instance is thread-safe and lazily created on first access.

### Verification
- [x] Verified that multiple rapid startup attempts of `TrackerService` and `ViewerService` do not trigger DataStore exceptions.
- [x] Settings persistence remains consistent across process restarts.
