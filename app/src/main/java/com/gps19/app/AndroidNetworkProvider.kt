package com.gps19.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.gps19.core.engine.NetworkListener
import com.gps19.core.engine.NetworkProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AndroidNetworkProvider: Production implementation of NetworkProvider 
 * using ConnectivityManager.NetworkCallback.
 */
@Singleton
class AndroidNetworkProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkProvider {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = mutableSetOf<NetworkListener>()
    
    private val networkCallback = object : ManagedNetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(listeners) {
                listeners.forEach { it.onNetworkAvailable() }
            }
        }

        override fun onLost(network: Network) {
            synchronized(listeners) {
                listeners.forEach { it.onNetworkLost() }
            }
        }
    }

    private var isRegistered = false

    override fun registerListener(listener: NetworkListener) {
        synchronized(listeners) {
            listeners.add(listener)
            if (!isRegistered) {
                try {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        connectivityManager.registerNetworkCallback(
                            request, 
                            networkCallback, 
                            Handler(Looper.getMainLooper())
                        )
                    } else {
                        connectivityManager.registerNetworkCallback(request, networkCallback)
                    }
                    isRegistered = true
                } catch (e: Exception) {
                    Timber.e(e, "Failed to register network callback")
                }
            }
        }
    }

    override fun unregisterListener(listener: NetworkListener) {
        synchronized(listeners) {
            listeners.remove(listener)
            if (listeners.isEmpty() && isRegistered) {
                networkCallback.unregister(connectivityManager)
                isRegistered = false
            }
        }
    }
}
