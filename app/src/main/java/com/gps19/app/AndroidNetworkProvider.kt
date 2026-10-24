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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AndroidNetworkProvider: Production implementation of NetworkProvider 
 * using ConnectivityManager.NetworkCallback.
 * 
 * Oct.20.23:
 * - ISSUE-OCT20.23-F1: Redundant Connectivity Callback Leaks. Transitioned to 
 *   SharingStarted.Eagerly to ensure a single registration for the provider's 
 *   entire lifecycle, preventing redundant calls detected by StackLog.
 * Oct.18.24:
 * - ISSUE-OCT18.22-F2: Redundant Network Callback Registrations. Consolidated 
 *   all callback management into a single provider and added isNetworkAvailableFlow.
 *   Unified listener management via Flow collection.
 * Oct.18.6:
 * - ISSUE-OCT18.6-F1: Synthetic Physical Event Injection. Implemented simulateNetworkLoss 
 *   to allow manual triggering of connectivity failover logic.
 * Oct.24.16:
 * - ISSUE-OCT24.14-F2: StackLog Leak. Hardened registration lifecycle. Moved 
 *   system callback registration to the Main thread and added a process-level 
 *   idempotency guard to prevent StackLog diagnostic traces on Samsung A15.
 */
@Singleton
class AndroidNetworkProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val timeProvider: TimeProvider
) : NetworkProvider {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = mutableSetOf<NetworkListener>()
    private val listenerJobs = mutableMapOf<NetworkListener, Job>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isLossSimulated = AtomicBoolean(false)

    companion object {
        // Oct.24.16: Process-level guard to prevent multiple registrations 
        // across provider instances (primarily for Hilt test stability).
        private val isSystemCallbackRegistered = AtomicBoolean(false)
    }
    
    override val isNetworkAvailable: Boolean
        get() {
            if (isLossSimulated.get()) return false
            return try {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (e: Exception) { false }
        }

    override val isNetworkAvailableFlow: Flow<Boolean> = callbackFlow {
        val callback = object : ManagedNetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!isLossSimulated.get()) {
                    trySend(true)
                }
            }
            override fun onLost(network: Network) {
                if (!isLossSimulated.get()) {
                    trySend(false)
                }
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (!isLossSimulated.get()) {
                    val available = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    trySend(available)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        // Oct.24.16: Ensure registration happens on Main thread to satisfy 
        // Samsung platform diagnostic constraints and prevent StackLog leaks.
        withContext(Dispatchers.Main.immediate) {
            if (isSystemCallbackRegistered.compareAndSet(false, true)) {
                try {
                    Timber.d("AndroidNetworkProvider: Registering system network callback.")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        connectivityManager.registerNetworkCallback(request, callback, mainHandler)
                    } else {
                        connectivityManager.registerNetworkCallback(request, callback)
                    }
                    trySend(isNetworkAvailable)
                } catch (e: Exception) {
                    isSystemCallbackRegistered.set(false)
                    Timber.e(e, "AndroidNetworkProvider: Registration failed")
                    trySend(false)
                }
            } else {
                Timber.w("AndroidNetworkProvider: System callback already registered. Skipping to prevent StackLog.")
                trySend(isNetworkAvailable)
            }
        }

        awaitClose {
            Timber.d("AndroidNetworkProvider: awaitClose triggered for system network callback.")
            // Oct.24.16: Perform unregistration via the managed helper to ensure 
            // the static guard is reset ONLY after the platform call completes.
            ManagedUnregistrationHelper.safeUnregister(
                "AndroidNetworkProvider_Final",
                mainHandler,
                timeProvider
            ) {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (e: Exception) {
                    Timber.w("ManagedNetworkCallback: Unregister failed")
                }
                isSystemCallbackRegistered.set(false)
            }
        }
    }.flowOn(Dispatchers.IO)
     .distinctUntilChanged()
     .shareIn(
        scope = externalScope,
        started = SharingStarted.Eagerly,
        replay = 1
     )

    override fun registerListener(listener: NetworkListener) {
        synchronized(listeners) {
            if (listeners.add(listener)) {
                val job = externalScope.launch {
                    isNetworkAvailableFlow.collect { available ->
                        if (available) listener.onNetworkAvailable() else listener.onNetworkLost()
                    }
                }
                listenerJobs[listener] = job
            }
        }
    }

    override fun unregisterListener(listener: NetworkListener) {
        synchronized(listeners) {
            if (listeners.remove(listener)) {
                listenerJobs.remove(listener)?.cancel()
            }
        }
    }

    override fun simulateNetworkLoss(active: Boolean) {
        if (isLossSimulated.getAndSet(active) != active) {
            Timber.i("AndroidNetworkProvider: Network Loss Simulation -> $active")
            // Emission will be triggered if the flow is active
            // but we need to notify listeners immediately if simulated
            synchronized(listeners) {
                listeners.toList().forEach {
                    if (active) it.onNetworkLost() else if (isNetworkAvailable) it.onNetworkAvailable()
                }
            }
        }
    }
}
