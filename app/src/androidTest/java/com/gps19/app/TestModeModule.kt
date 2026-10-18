package com.gps19.app

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * TestModeModule: Replaces the production InfrastructureModule during tests.
 * Resolves SIMP-OCT17.11-S1.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [InfrastructureModule::class]
)
object TestModeModule {

    @Provides
    @Singleton
    fun provideManualTestModeProvider(): ManualTestModeProvider {
        // Default to true for instrumentation tests
        return ManualTestModeProvider(enabled = true)
    }

    @Provides
    @Singleton
    fun provideTestModeProvider(manual: ManualTestModeProvider): TestModeProvider = manual
}
