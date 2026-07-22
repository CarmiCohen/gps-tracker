package com.gps19.app

import android.content.Context
import androidx.room.Room
import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * AppContainer: Manual Dependency Injection container.
 * July.21.00:
 * - Forensic Hardening: Corrected MainRepository instantiation with full persistence stack.
 * - Issue #115: CommunicationManager constructor alignment.
 * July.17.00:
 * - Issue #526: Performance Hardening. Consolidated all UseCases into lazy properties.
 */
class AppContainer(private val context: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val timeProvider: TimeProvider = AndroidTimeProvider()

    val database: AppDatabase by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Room.databaseBuilder(context, AppDatabase::class.java, "gps_tracker_db")
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(AppDatabase.MIGRATION_56_57)
            .fallbackToDestructiveMigration()
            .build()
    }

    val logDao by lazy(LazyThreadSafetyMode.PUBLICATION) { database.logDao() }
    val trailDao by lazy(LazyThreadSafetyMode.PUBLICATION) { database.trailDao() }
    val historyDao by lazy(LazyThreadSafetyMode.PUBLICATION) { database.historyDao() }
    val violationDao by lazy(LazyThreadSafetyMode.PUBLICATION) { database.violationDao() }
    val pendingStatusDao by lazy(LazyThreadSafetyMode.PUBLICATION) { database.pendingStatusDao() }

    val settingsRepository by lazy(LazyThreadSafetyMode.PUBLICATION) { SettingsRepository(context, timeProvider) }
    val telemetryRepository by lazy(LazyThreadSafetyMode.PUBLICATION) { TelemetryRepository() }
    val logRepository by lazy(LazyThreadSafetyMode.PUBLICATION) { LogRepository(logDao, applicationScope, timeProvider) }
    val offlineRepository by lazy(LazyThreadSafetyMode.PUBLICATION) { OfflineRepository(pendingStatusDao, telemetryRepository) }

    val mainRepository by lazy(LazyThreadSafetyMode.PUBLICATION) {
        MainRepository(
            context, trailDao, historyDao, violationDao, pendingStatusDao, database, 
            settingsRepository, telemetryRepository, logRepository, offlineRepository, timeProvider
        )
    }

    val configManager by lazy(LazyThreadSafetyMode.PUBLICATION) { ConfigManager(context, mainRepository) }
    val systemStatusProvider: SystemStatusProvider by lazy(LazyThreadSafetyMode.PUBLICATION) { SystemStatusProviderImpl(context) }
    val gpsManager by lazy(LazyThreadSafetyMode.PUBLICATION) { GpsManager(context, timeProvider) }
    val appSensorManager by lazy(LazyThreadSafetyMode.PUBLICATION) { AppSensorManager(context, applicationScope, timeProvider) }
    val locationProcessor by lazy(LazyThreadSafetyMode.PUBLICATION) { LocationProcessor(timeProvider) }
    val violationProcessor by lazy(LazyThreadSafetyMode.PUBLICATION) { ViolationProcessor(timeProvider) }

    val logManager: LogManager by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LogManager(logRepository, telemetryRepository, { connectivitySuite }, configManager, timeProvider)
    }

    val communicationManager: CommunicationManager by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CommunicationManager(context, configManager, logManager, telemetryRepository, logRepository, timeProvider)
    }

    val systemMonitor by lazy(LazyThreadSafetyMode.PUBLICATION) { SystemMonitor(context, timeProvider) }
    val appNotificationManager by lazy(LazyThreadSafetyMode.PUBLICATION) { AppNotificationManager(context) }
    val sessionManager by lazy(LazyThreadSafetyMode.PUBLICATION) { SessionManager(mainRepository, timeProvider) }
    val serviceForensicUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { ServiceForensicUseCase(mainRepository) }
    val integrityMonitor by lazy(LazyThreadSafetyMode.PUBLICATION) { IntegrityMonitor(context, mainRepository, timeProvider) }
    val appAlarmManager by lazy(LazyThreadSafetyMode.PUBLICATION) { AppAlarmManager(context, mainRepository, sessionManager, appNotificationManager, timeProvider) }
    
    val historyManager by lazy(LazyThreadSafetyMode.PUBLICATION) {
        HistoryManager(context, mainRepository, timeProvider, gpsManager, appSensorManager, locationProcessor)
    }
    
    val connectivitySuite: ConnectivitySuite by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ConnectivitySuite(context, settingsRepository, telemetryRepository, { logManager }, timeProvider, communicationManager, sessionManager, gpsManager, locationProcessor, offlineRepository, mainRepository, appAlarmManager, serviceForensicUseCase)
    }

    val serviceBehaviorUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { ServiceBehaviorUseCase(timeProvider) }
    val gpsStatusManager by lazy(LazyThreadSafetyMode.PUBLICATION) { GpsStatusManager(telemetryRepository, settingsRepository) }
    val stateSubscriptionUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StateSubscriptionUseCase(mainRepository, gpsStatusManager, systemStatusProvider, timeProvider)
    }

    val commandRouter: CommandRouter by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CommandRouter(context, configManager, logManager, connectivitySuite, appAlarmManager, appNotificationManager, sessionManager, locationProcessor, mainRepository, integrityMonitor, timeProvider)
    }

    // UseCase Consolidation (Deferred creation)
    val homePointUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { HomePointUseCase(mainRepository) }
    val dashboardUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { DashboardUseCase() }
    val navigationUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { NavigationUseCase() }
    val settingsUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { SettingsUseCase(mainRepository, settingsRepository, timeProvider, logManager) }
    val telemetryUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { TelemetryUseCase(timeProvider) }
    val sessionUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { SessionUseCase(mainRepository, timeProvider) }
    val behaviorUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { BehaviorUseCase() }
    val alertUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { AlertUseCase(mainRepository, timeProvider, logManager) }
    val mapUseCase by lazy(LazyThreadSafetyMode.PUBLICATION) { MapUseCase() }
}
