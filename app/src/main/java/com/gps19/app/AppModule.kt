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
                AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23, AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25,
                AppDatabase.MIGRATION_25_26, AppDatabase.MIGRATION_26_27, AppDatabase.MIGRATION_27_28,
                AppDatabase.MIGRATION_28_29, AppDatabase.MIGRATION_29_30, AppDatabase.MIGRATION_30_31,
                AppDatabase.MIGRATION_31_32, AppDatabase.MIGRATION_32_33, AppDatabase.MIGRATION_33_34,
                AppDatabase.MIGRATION_34_35, AppDatabase.MIGRATION_35_36, AppDatabase.MIGRATION_36_37,
                AppDatabase.MIGRATION_37_38, AppDatabase.MIGRATION_38_39, AppDatabase.MIGRATION_39_40,
                AppDatabase.MIGRATION_40_41, AppDatabase.MIGRATION_41_42, AppDatabase.MIGRATION_42_43,
                AppDatabase.MIGRATION_43_44, AppDatabase.MIGRATION_44_45, AppDatabase.MIGRATION_45_46,
                AppDatabase.MIGRATION_46_47, AppDatabase.MIGRATION_47_48, AppDatabase.MIGRATION_48_49,
                AppDatabase.MIGRATION_49_50, AppDatabase.MIGRATION_50_51, AppDatabase.MIGRATION_51_52,
                AppDatabase.MIGRATION_52_53, AppDatabase.MIGRATION_53_54, AppDatabase.MIGRATION_54_55,
                AppDatabase.MIGRATION_55_56, AppDatabase.MIGRATION_56_57
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
