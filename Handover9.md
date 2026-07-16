# Project Handover: Issue #503 (Hilt Removal)

## Current Status: IN PROGRESS (Manual DI Migration - Final Phase)
**Version Context**: `July.16.17`

The project is nearing completion of the transition from Hilt to manual Dependency Injection. The core infrastructure is stable, and the majority of components have been stripped of Hilt annotations. The system is still in a non-compiling state primarily due to the few remaining `@AndroidEntryPoint` classes and the presence of the Hilt Gradle plugin.

## Forensic Status Information

### 1. Manual DI Infrastructure (Stable)
- **`AppContainer.kt`**: Fully populated central registry. Handles all singletons, including the complex `RemoteHandler` and `CommunicationManager` configurations.
- **`GpsApplication.kt`**: Successfully refactored. Initializes `AppContainer`. Implements a manual `WorkerFactory` for WorkManager.
- **`MainViewModelFactory.kt`**: Fixed. Now correctly maps all use cases and repositories to `MainViewModel`.

### 2. Refactored Components (Hilt Fully Removed)
The following files are now completely clean of Hilt annotations:
- **Repositories**: `MainRepository`, `SettingsRepository`, `LogRepository`, `OfflineRepository`.
- **Services**: `BaseMonitorService`, `TrackerService`, `ViewerService`.
- **Use Cases**: `DashboardUseCase`, `NavigationUseCase`, `SettingsUseCase`, `TelemetryUseCase`, `StateSubscriptionUseCase`, `SessionUseCase`, `BehaviorUseCase`, `AlertUseCase`, `MapUseCase`, `HomePointUseCase`, `ServiceBehaviorUseCase`, `ServiceForensicUseCase`.
- **Managers**: `GpsManager`, `AppSensorManager`, `ConfigManager`, `GpsStatusManager`, `AppNotificationManager`, `IntegrityMonitor`, `AppAlarmManager`, `SystemMonitor`, `SystemStatusProviderImpl`.
- **Workers**: `MaintenanceWorker`.

### 3. Lingering Hilt Dependencies (Immediate Actions Required)
These final components must be refactored to restore the build:
- **`HistoryManager.kt`**: Still has `@Singleton` and `@Inject`.
- **`RemoteUpdateWrapper.kt`**: Still has `@Singleton` and `@Inject`.
- **`WatchdogReceiver.kt`**: Still has `@AndroidEntryPoint` and `@Inject`.
- **`MainActivity.kt`**: Still has `@AndroidEntryPoint`. Needs update to use `MainViewModelFactory`.
- **`AlarmActivity.kt`**: Still has `@AndroidEntryPoint`. Needs update to use `MainViewModelFactory`.

### 4. Build Blockers & Cleanup
- **Blocker**: `@AndroidEntryPoint` in Activities/Receivers requires a `@HiltAndroidApp` which no longer exists in `GpsApplication`.
- **Cleanup**: `AppModule.kt` is still present and must be deleted.
- **Gradle**: `hilt-android-gradle-plugin` and its dependencies must be removed from `build.gradle` and `project/build.gradle`.

## Resumption Plan
1.  **Refactor Sections 3**: Remove Hilt annotations from `HistoryManager`, `RemoteUpdateWrapper`, and `WatchdogReceiver`.
2.  **Update Activities**: Transition `MainActivity` and `AlarmActivity` to manual `ViewModel` initialization.
3.  **Delete `AppModule.kt`**.
4.  **Gradle Purge**: Remove all Hilt-related plugins and dependencies.
5.  **Clean Rebuild**: Execute `./gradlew clean assembleDebug`.

## Environment Info
- **Database Version**: 57
- **Source of Truth**: `SIMPLIFICATION_PLAN.md`
- **Target Version**: `July.16.17`
