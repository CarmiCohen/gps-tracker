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
 * Part of Issue #503: Hilt Removal.
 */
class AppContainer(private val context: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val timeProvider: TimeProvider = AndroidTimeProvider()

    val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "gps_tracker_db"
    )
    .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    .addMigrations(AppDatabase.MIGRATION_56_57)
    .fallbackToDestructiveMigration()
    .build()

    val logDao = database.logDao()
    val trailDao = database.trailDao()
    val historyDao = database.historyDao()
    val violationDao = database.violationDao()
    val pendingStatusDao = database.pendingStatusDao()

    val settingsRepository = SettingsRepository(context, timeProvider)
    
    val telemetryRepository = TelemetryRepository()
    
    val logRepository = LogRepository(logDao, applicationScope, timeProvider)
    
    val offlineRepository = OfflineRepository(pendingStatusDao, telemetryRepository)

    val mainRepository = MainRepository(
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

    val configManager = ConfigManager(context, mainRepository)

    val systemStatusProvider: SystemStatusProvider = SystemStatusProviderImpl(context)

    val gpsManager = GpsManager(context, timeProvider)

    val appSensorManager = AppSensorManager(context, applicationScope, timeProvider)

    val locationProcessor = LocationProcessor(timeProvider)
    
    val violationProcessor = ViolationProcessor(timeProvider)

    val logManager: LogManager by lazy {
        LogManager(
            logRepository = logRepository,
            telemetry = telemetryRepository,
            networkManager = { appNetworkManager },
            configManager = configManager,
            timeProvider = timeProvider
        )
    }

    val remoteUpdateWrapper = RemoteUpdateWrapper()

    val communicationManager: CommunicationManager by lazy {
        CommunicationManager(context, configManager, logManager, timeProvider)
    }

    val appNetworkManager: AppNetworkManager by lazy {
        AppNetworkManager(
            context = context,
            settingsRepository = settingsRepository,
            telemetryRepository = telemetryRepository,
            logManager = logManager,
            onRemoteUpdateWrapper = remoteUpdateWrapper,
            timeProvider = timeProvider,
            signalingProvider = communicationManager
        )
    }

    val systemMonitor = SystemMonitor(context, timeProvider)
    
    val appNotificationManager = AppNotificationManager(context, configManager, timeProvider)

    val sessionManager = SessionManager(mainRepository, timeProvider)
    
    val serviceForensicUseCase = ServiceForensicUseCase(mainRepository)
    
    val integrityMonitor = IntegrityMonitor(context, mainRepository, timeProvider)
    
    val appAlarmManager = AppAlarmManager(context, mainRepository, sessionManager, appNotificationManager, timeProvider)
    
    val historyManager = HistoryManager(
        context = context,
        repository = mainRepository,
        timeProvider = timeProvider,
        gpsManager = gpsManager,
        sensorManager = appSensorManager,
        locationProcessor = locationProcessor
    )
    
    val syncManager = SyncManager(
        context = context,
        networkManager = appNetworkManager,
        sessionManager = sessionManager,
        gpsManager = gpsManager,
        locationProcessor = locationProcessor,
        telemetryRepository = telemetryRepository,
        offlineRepository = offlineRepository,
        logManager = logManager,
        timeProvider = timeProvider,
        repository = mainRepository
    )

    val serviceBehaviorUseCase = ServiceBehaviorUseCase(timeProvider)

    val gpsStatusManager = GpsStatusManager(telemetryRepository, settingsRepository)

    val stateSubscriptionUseCase = StateSubscriptionUseCase(
        mainRepository,
        gpsStatusManager,
        systemStatusProvider,
        timeProvider
    )

    val remoteHandler = RemoteHandler(
        context = context,
        repository = mainRepository,
        locationProcessor = locationProcessor,
        alarmManager = appAlarmManager,
        sessionManager = sessionManager,
        forensicUseCase = serviceForensicUseCase,
        timeProvider = timeProvider
    )

    val commandRouter = CommandRouter(
        context = context,
        configManager = configManager,
        logManager = logManager,
        networkManager = appNetworkManager,
        alarmManager = appAlarmManager,
        notificationManager = appNotificationManager,
        sessionManager = sessionManager,
        locationProcessor = locationProcessor,
        remoteHandler = remoteHandler,
        repository = mainRepository,
        syncManager = syncManager,
        integrityMonitor = integrityMonitor,
        timeProvider = timeProvider
    )
}
