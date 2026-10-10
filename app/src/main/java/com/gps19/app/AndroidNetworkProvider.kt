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
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AndroidNetworkProvider: Production implementation of NetworkProvider 
 * using ConnectivityManager.NetworkCallback.
 * 
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Implemented isNetworkAvailable 
 *   property for unified state access.
 * Sep.28.12:
 * - Issue #1360: Mismatched unregistration signatures. Injected TimeProvider
 *   to comply with updated ManagedNetworkCallback.unregister requirement.
 */
@Singleton
class AndroidNetworkProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) : NetworkProvider {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = mutableSetOf<NetworkListener>()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override val isNetworkAvailable: Boolean
        get() {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

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
            ensureCorrectRegistrationState()
        }
    }

    override fun unregisterListener(listener: NetworkListener) {
        synchronized(listeners) {
            listeners.remove(listener)
            ensureCorrectRegistrationState()
        }
    }

    private fun ensureCorrectRegistrationState() {
        if (Looper.myLooper() == mainHandler.looper) {
            updateRegistrationState()
        } else {
            mainHandler.post { updateRegistrationState() }
        }
    }

    private fun updateRegistrationState() {
        synchronized(listeners) {
            val shouldBeRegistered = listeners.isNotEmpty()
            if (shouldBeRegistered && !isRegistered) {
                performRegistration()
            } else if (!shouldBeRegistered && isRegistered) {
                performUnregistration()
            }
        }
    }

    private fun performRegistration() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectivityManager.registerNetworkCallback(
                    request, 
                    networkCallback, 
                    mainHandler
                )
            } else {
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }
            isRegistered = true
            Timber.d("AndroidNetworkProvider: Registered callback")
        } catch (e: Exception) {
            Timber.e(e, "Failed to register network callback")
        }
    }

    private fun performUnregistration() {
        networkCallback.unregister(connectivityManager, timeProvider, mainHandler)
        isRegistered = false
        Timber.d("AndroidNetworkProvider: Unregistered callback")
    }
}
