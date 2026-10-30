package com.gps19.app.sensor

import android.content.Context
import com.gps19.core.shared.TickOrchestrator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
