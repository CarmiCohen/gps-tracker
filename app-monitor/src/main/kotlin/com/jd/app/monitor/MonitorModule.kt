package com.jd.app.monitor

import android.content.Context
import androidx.room.Room
import com.jd.core.network.SignalingClient
import com.jd.core.shared.EventLog
import com.jd.core.shared.EventSource
import com.jd.core.shared.GraphHistory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
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

    /** The Monitor's events and the Sensor's events it received (ISSUE-NOV10.8-O84). */
    @Provides
    @Singleton
    fun provideEventLog(@ApplicationContext context: Context): EventLog =
        EventLog(File(context.filesDir, EventLog.FILE_NAME), EventSource.MONITOR)

    /** The paired Sensor's samples it received and the Monitor's own, for the Graphs (ISSUE-NOV10.8-O84 part 3). */
    @Provides
    @Singleton
    fun provideMonitorGraphs(@ApplicationContext context: Context): MonitorGraphs = MonitorGraphs(
        sensor = GraphHistory(File(context.filesDir, GraphHistory.SENSOR_FILE_NAME)),
        own = GraphHistory(File(context.filesDir, GraphHistory.MONITOR_FILE_NAME))
    )

    @Provides
    @Singleton
    fun provideMonitorRepository(
        trailDao: TrailDao,
        signalingClient: SignalingClient
    ): MonitorRepository {
        return MonitorRepository(trailDao, signalingClient)
    }
}
