package com.gps19.app

import android.content.Context
import androidx.room.Room
import com.gps19.core.engine.LocationProcessor
import com.gps19.core.engine.LocationProcessorListener
import com.gps19.core.engine.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSystemStatusProvider(impl: SystemStatusProviderImpl): SystemStatusProvider

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: AndroidTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindSignalingProvider(impl: CommunicationManager): SignalingProvider

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "gps_tracker_db"
            )
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(
                AppDatabase.MIGRATION_56_57
            )
            .fallbackToDestructiveMigration()
            .build()
        }

        @Provides
        fun provideLogDao(db: AppDatabase) = db.logDao()

        @Provides
        fun provideTrailDao(db: AppDatabase) = db.trailDao()

        @Provides
        fun provideHistoryDao(db: AppDatabase) = db.historyDao()

        @Provides
        fun provideViolationDao(db: AppDatabase) = db.violationDao()

        @Provides
        fun providePendingStatusDao(db: AppDatabase) = db.pendingStatusDao()

        @Provides
        @Singleton
        fun provideGpsManager(@ApplicationContext context: Context, timeProvider: TimeProvider): GpsManager {
            return GpsManager(context, timeProvider)
        }

        @Provides
        @Singleton
        fun provideAppSensorManager(
            @ApplicationContext context: Context, 
            @ApplicationScope scope: CoroutineScope,
            timeProvider: TimeProvider
        ): AppSensorManager {
            return AppSensorManager(context, scope, timeProvider)
        }

        @Provides
        @Singleton
        fun provideLocationProcessor(timeProvider: TimeProvider): LocationProcessor {
            return LocationProcessor(timeProvider)
        }
    }
}
