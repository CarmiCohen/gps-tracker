package com.jd.core.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the app's own identity store. It is a module of its own so that instrumented tests can
 * replace only this binding with a test file and never write the installed app's pairing or
 * identity, since `am instrument` keeps app data (ISSUE-NOV12.6-O93).
 */
@Module
@InstallIn(SingletonComponent::class)
object IdentityModule {
    @Provides
    @Singleton
    fun provideForensicIdentityManager(@ApplicationContext context: Context): ForensicIdentityManager =
        ForensicIdentityManager(context)
}
