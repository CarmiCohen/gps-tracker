package com.gps19.app

import com.gps19.core.engine.PowerStateProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PowerModule {
    @Binds
    @Singleton
    abstract fun bindPowerStateProvider(impl: AndroidPowerStateProvider): PowerStateProvider
}
