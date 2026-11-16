package com.jd.app.sensor

import androidx.test.core.app.ApplicationProvider
import com.jd.core.shared.GraphHistory
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.File
import javax.inject.Singleton

/**
 * Gives the Sensor's instrumented tests their own sample file: `am instrument` keeps app data, so a run must never write
 * into the installed Sensor's Graphs, which its Monitor also receives (ISSUE-NOV10.8-O84 part 3).
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [GraphHistoryModule::class])
object TestGraphHistoryModule {
    @Provides
    @Singleton
    fun provideGraphHistory(): GraphHistory = GraphHistory(
        File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "graph_history_test.jsonl")
    )
}
