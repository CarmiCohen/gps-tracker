package com.jd.app.sensor

import android.content.Context
import com.jd.core.shared.EventLog
import com.jd.core.shared.EventSource
import com.jd.core.shared.GraphHistory
import com.jd.core.shared.TickOrchestrator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideTickOrchestrator(): TickOrchestrator {
        return TickOrchestrator()
    }

    @Provides
    @Singleton
    fun provideSensorDataStore(@ApplicationContext context: Context): SensorDataStore {
        return SensorDataStore(context)
    }
}

/**
 * The Sensor's events (ISSUE-NOV10.8-O84). The service runs before the first unlock (Direct Boot), so the file is in
 * device-protected storage, as the forensic trace is. A module of its own, so instrumented tests replace it and never
 * write into the installed Sensor's Events list.
 */
@Module
@InstallIn(SingletonComponent::class)
object EventLogModule {
    @Provides
    @Singleton
    fun provideEventLog(@ApplicationContext context: Context): EventLog =
        EventLog(File(context.createDeviceProtectedStorageContext().filesDir, EventLog.FILE_NAME), EventSource.SENSOR)
}

/**
 * The Sensor's samples for the Graphs (ISSUE-NOV10.8-O84 part 3), in device-protected storage like its events, and in a
 * module of its own for the same reason: instrumented tests replace it.
 */
@Module
@InstallIn(SingletonComponent::class)
object GraphHistoryModule {
    @Provides
    @Singleton
    fun provideGraphHistory(@ApplicationContext context: Context): GraphHistory =
        GraphHistory(File(context.createDeviceProtectedStorageContext().filesDir, GraphHistory.SENSOR_FILE_NAME))
}
