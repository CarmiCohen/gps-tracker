package com.gps19.app

import android.content.Context
import androidx.room.Room
import com.gps19.core.engine.LocationProcessor
import com.gps19.core.engine.TimeProvider
import com.gps19.core.engine.ViolationProcessor
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

    @Binds
    @Singleton
    abstract fun bindDashboardStateProvider(impl: DashboardStateProviderImpl): DashboardStateProvider

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
                AppDatabase.MIGRATION_56_57,
                AppDatabase.MIGRATION_57_58,
                AppDatabase.MIGRATION_58_59,
                AppDatabase.MIGRATION_59_60,
                AppDatabase.MIGRATION_60_61,
                AppDatabase.MIGRATION_61_62,
                AppDatabase.MIGRATION_62_63,
                AppDatabase.MIGRATION_63_64,
                AppDatabase.MIGRATION_64_65,
                AppDatabase.MIGRATION_65_66,
                AppDatabase.MIGRATION_66_67,
                AppDatabase.MIGRATION_67_68,
                AppDatabase.MIGRATION_68_69,
                AppDatabase.MIGRATION_69_70,
                AppDatabase.MIGRATION_70_71,
                AppDatabase.MIGRATION_71_72
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
        fun provideLocationProcessor(timeProvider: TimeProvider): LocationProcessor {
            return LocationProcessor(timeProvider)
        }

        @Provides
        @Singleton
        fun provideViolationProcessor(timeProvider: TimeProvider): ViolationProcessor {
            return ViolationProcessor(timeProvider)
        }
    }
}
