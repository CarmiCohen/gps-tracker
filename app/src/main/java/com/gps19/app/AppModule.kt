package com.gps19.app

import android.content.Context
import androidx.room.Room
import com.gps19.core.engine.*
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
    abstract fun bindBootLifecycleAuthority(impl: AndroidBootLifecycleAuthority): BootLifecycleAuthority

    @Binds
    @Singleton
    abstract fun bindSignalingProvider(impl: CommunicationManager): SignalingProvider

    @Binds
    @Singleton
    abstract fun bindNetworkProvider(impl: AndroidNetworkProvider): NetworkProvider

    @Binds
    @Singleton
    abstract fun bindSignalingTransport(impl: HttpSignalingTransport): SignalingTransport

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
                AppDatabase.MIGRATION_71_72,
                AppDatabase.MIGRATION_72_73,
                AppDatabase.MIGRATION_73_74,
                AppDatabase.MIGRATION_74_75,
                AppDatabase.MIGRATION_75_76,
                AppDatabase.MIGRATION_76_77,
                AppDatabase.MIGRATION_77_78,
                AppDatabase.MIGRATION_78_79,
                AppDatabase.MIGRATION_79_80,
                AppDatabase.MIGRATION_80_81,
                AppDatabase.MIGRATION_81_82,
                AppDatabase.MIGRATION_82_83,
                AppDatabase.MIGRATION_83_84,
                AppDatabase.MIGRATION_84_85,
                AppDatabase.MIGRATION_85_86,
                AppDatabase.MIGRATION_86_87,
                AppDatabase.MIGRATION_87_88
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
        fun providePendingViolationDao(db: AppDatabase) = db.pendingViolationDao()

        @Provides
        @Singleton
        fun provideDomainEventBus(): DomainEventBus {
            return DomainEventBus()
        }

        @Provides
        @Singleton
        fun provideLocationProcessor(timeProvider: TimeProvider, domainEventBus: DomainEventBus): LocationProcessor {
            return LocationProcessor(timeProvider, domainEventBus)
        }

        @Provides
        @Singleton
        fun provideViolationProcessor(timeProvider: TimeProvider): ViolationProcessor {
            return ViolationProcessor(timeProvider)
        }
    }
}
