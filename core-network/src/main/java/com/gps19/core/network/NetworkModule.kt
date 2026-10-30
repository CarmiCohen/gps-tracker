package com.gps19.core.network

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindSignalingClient(impl: SocketioSignalingClient): SignalingClient

    companion object {
        @Provides
        @Singleton
        fun provideForensicIdentityManager(@ApplicationContext context: Context): ForensicIdentityManager {
            return ForensicIdentityManager(context)
        }
    }
}
