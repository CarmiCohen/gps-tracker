package com.gps19.app.monitor

import android.content.Context
import androidx.room.Room
import com.gps19.core.network.SignalingClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MonitorModule {

    @Provides
    @Singleton
    fun provideMonitorDatabase(@ApplicationContext context: Context): MonitorDatabase {
        return Room.databaseBuilder(
            context,
            MonitorDatabase::class.java,
            "monitor_db"
        )
        .fallbackToDestructiveMigration() // Revol30.3: Allow schema changes for sequenceId
        .build()
    }

    @Provides
    fun provideTrailDao(db: MonitorDatabase): TrailDao = db.trailDao()

    @Provides
    @Singleton
    fun provideMonitorDataStore(@ApplicationContext context: Context): MonitorDataStore {
        return MonitorDataStore(context)
    }

    @Provides
    @Singleton
    fun provideMonitorRepository(
        trailDao: TrailDao,
        signalingClient: SignalingClient
    ): MonitorRepository {
        return MonitorRepository(trailDao, signalingClient)
    }
}
