package com.jd.core.network

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindSignalingClient(impl: SocketioSignalingClient): SignalingClient

    @Binds
    @Singleton
    abstract fun bindSocketFactory(impl: DefaultSocketFactory): SocketFactory

    companion object {
        @Provides
        @Singleton
        fun provideForensicIdentityManager(@ApplicationContext context: Context): ForensicIdentityManager {
            return ForensicIdentityManager(context)
        }

        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
