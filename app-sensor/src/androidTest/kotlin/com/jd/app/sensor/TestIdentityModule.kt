package com.jd.app.sensor

import androidx.test.core.app.ApplicationProvider
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.IdentityModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Gives the Sensor's instrumented tests their own identity file: `am instrument` keeps app data, so the
 * tests must never reset the installed Sensor's identity in [ForensicIdentityManager.FILE_NAME] (ISSUE-NOV12.6-O93).
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [IdentityModule::class])
object TestIdentityModule {
    const val TEST_IDENTITY_FILE = "forensic_identity_test.preferences_pb"

    @Provides
    @Singleton
    fun provideForensicIdentityManager(): ForensicIdentityManager =
        ForensicIdentityManager(ApplicationProvider.getApplicationContext(), TEST_IDENTITY_FILE)
}
