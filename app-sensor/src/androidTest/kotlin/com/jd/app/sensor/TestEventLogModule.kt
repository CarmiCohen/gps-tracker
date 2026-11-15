package com.jd.app.sensor

import androidx.test.core.app.ApplicationProvider
import com.jd.core.shared.EventLog
import com.jd.core.shared.EventSource
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.File
import javax.inject.Singleton

/**
 * Gives the Sensor's instrumented tests their own event file: `am instrument` keeps app data, so a run must never write
 * into the installed Sensor's Events list, which its Monitor also receives (ISSUE-NOV10.8-O84).
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [EventLogModule::class])
object TestEventLogModule {
    @Provides
    @Singleton
    fun provideEventLog(): EventLog = EventLog(
        File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "event_log_test.jsonl"),
        EventSource.SENSOR
    )
}
