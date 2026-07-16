package com.gps19.app

import android.content.Context
import androidx.room.Room
import com.gps19.core.engine.LocationProcessor
import com.gps19.core.engine.TimeProvider
import com.gps19.core.engine.ViolationProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * AppContainer: Manual Dependency Injection container.
 * July.16.24:
 * - Issue #526: Landing Page Hang. Converted all components to lazy initialization
 *   to prevent Main thread spikes during application startup.
 */
class AppContainer(private val context: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val timeProvider: TimeProvider = AndroidTimeProvider()

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gps_tracker_db"
        )
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .addMigrations(AppDatabase.MIGRATION_56_57)
        .fallbackToDestructiveMigration()
        .build()
    }

    val logDao by lazy { database.logDao() }
    val trailDao by lazy { database.trailDao() }
    val historyDao by lazy { database.historyDao() }
    val violationDao by lazy { database.violationDao() }
    val pendingStatusDao by lazy { database.pendingStatusDao() }

    val settingsRepository by lazy { SettingsRepository(context, timeProvider) }
    
    val telemetryRepository by lazy { TelemetryRepository() }
    
    val logRepository by lazy { LogRepository(logDao, applicationScope, timeProvider) }
    
    val offlineRepository by lazy { OfflineRepository(pendingStatusDao, telemetryRepository) }

    val mainRepository by lazy {
        MainRepository(
            context = context,
            trailDao = trailDao,
            historyDao = historyDao,
            violationDao = violationDao,
            pendingStatusDao = pendingStatusDao,
            database = database,
            settings = settingsRepository,
            telemetry = telemetryRepository,
            logRepository = logRepository,
            offlineRepository = offlineRepository,
            timeProvider = timeProvider
        )
    }

    val configManager by lazy { ConfigManager(context, mainRepository) }

    val systemStatusProvider: SystemStatusProvider by lazy { SystemStatusProviderImpl(context) }

    val gpsManager by lazy { GpsManager(context, timeProvider) }

    val appSensorManager by lazy { AppSensorManager(context, applicationScope, timeProvider) }

    val locationProcessor by lazy { LocationProcessor(timeProvider) }
    
    val violationProcessor by lazy { ViolationProcessor(timeProvider) }

    val logManager: LogManager by lazy {
        LogManager(
            logRepository = logRepository,
            telemetry = telemetryRepository,
            connectivitySuite = { connectivitySuite },
            configManager = configManager,
            timeProvider = timeProvider
        )
    }

    val communicationManager: CommunicationManager by lazy {
        CommunicationManager(context, configManager, logManager, timeProvider)
    }

    val systemMonitor by lazy { SystemMonitor(context, timeProvider) }
    
    val appNotificationManager by lazy { AppNotificationManager(context, configManager, timeProvider) }

    val sessionManager by lazy { SessionManager(mainRepository, timeProvider) }
    
    val serviceForensicUseCase by lazy { ServiceForensicUseCase(mainRepository) }
    
    val integrityMonitor by lazy { IntegrityMonitor(context, mainRepository, timeProvider) }
    
    val appAlarmManager by lazy { AppAlarmManager(context, mainRepository, sessionManager, appNotificationManager, timeProvider) }
    
    val historyManager by lazy {
        HistoryManager(
            context = context,
            repository = mainRepository,
            timeProvider = timeProvider,
            gpsManager = gpsManager,
            sensorManager = appSensorManager,
            locationProcessor = locationProcessor
        )
    }
    
    val connectivitySuite: ConnectivitySuite by lazy {
        ConnectivitySuite(
            context = context,
            settingsRepository = settingsRepository,
            telemetryRepository = telemetryRepository,
            logManager = { logManager },
            timeProvider = timeProvider,
            signalingProvider = communicationManager,
            sessionManager = sessionManager,
            gpsManager = gpsManager,
            locationProcessor = locationProcessor,
            offlineRepository = offlineRepository,
            mainRepository = mainRepository,
            alarmManager = appAlarmManager,
            forensicUseCase = serviceForensicUseCase
        )
    }

    val serviceBehaviorUseCase by lazy { ServiceBehaviorUseCase(timeProvider) }

    val gpsStatusManager by lazy { GpsStatusManager(telemetryRepository, settingsRepository) }

    val stateSubscriptionUseCase by lazy {
        StateSubscriptionUseCase(
            mainRepository,
            gpsStatusManager,
            systemStatusProvider,
            timeProvider
        )
    }

    val commandRouter: CommandRouter by lazy {
        CommandRouter(
            context = context,
            configManager = configManager,
            logManager = logManager,
            connectivitySuite = connectivitySuite,
            alarmManager = appAlarmManager,
            notificationManager = appNotificationManager,
            sessionManager = sessionManager,
            locationProcessor = locationProcessor,
            repository = mainRepository,
            integrityMonitor = integrityMonitor,
            timeProvider = timeProvider
        )
    }
}
