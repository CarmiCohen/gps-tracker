# Issue #058: TrackerService Initialization (Hilt Migration)
**Status**: Resolved (v9.3.6)
**Priority**: High
**Requirement**: R978

## Description
Refactor `TrackerService`, `ViewerService`, and common infrastructure to use Hilt dependency injection. Eliminate manual dependency instantiation and `EntryPointAccessors`.

## Resolution
- Consolidated 11 core components into `BaseMonitorService` using `@Inject`.
- Standardized initialization using `Listener` and `initialize(CoroutineScope)` patterns.
- Migrated `WatchdogReceiver` to `@AndroidEntryPoint`.
- Removed `EntryPointAccessors` from services and `GpsApplication`.
- Verified service lifecycle stability across role transitions.
