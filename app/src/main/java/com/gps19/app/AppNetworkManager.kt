package com.gps19.app

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level Network Manager for the Service.
 * Orchestrates the SignalingProvider (Socket.io) and the HTTP Keep-alive logic.
 * v9.3.25:
 * - R988: Activated binary telemetry channel for trackers to reduce relay overhead.
 * - R988: Implemented routingId-aware emitBinary calls.
 * v8.9.99:
 * - Issue #041: Identity Sanitization. Added strict ID validation before 
 *   initiating relay connections to prevent using corrupted identities.
 */
@Singleton
class AppNetworkManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val settingsRepository: SettingsRepository,
    private val telemetryRepository: TelemetryRepository,
    private val logManager: LogManager,
    private val onRemoteUpdateWrapper: RemoteUpdateWrapper,
    private val timeProvider: TimeProvider,
    private val signalingProvider: SignalingProvider
) {
    private var isStopped = false
    private val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException || isStopped) return@CoroutineExceptionHandler
        Log.e("GPS19_NET", "CRITICAL: Network loop failure: ${throwable.message}")
        logManager.logServiceEvent("CRITICAL: Network loop failure: ${throwable.message}", true)
        startKeepAliveLoop()
    }

    private var relayUrl = ""
    private var deviceId = ""
    private var viewerId = ""
    private var isTrackerMode = true
    private var lastReconnectTs = timeProvider.elapsedRealtime()

    private val consecutiveHttpFailures = AtomicInteger(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + networkExceptionHandler)
    private var keepAliveJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (isStopped || relayUrl.isEmpty()) return
            scope.launch {
                val now = timeProvider.elapsedRealtime()
                if (now - lastReconnectTs < 3000L || signalingProvider.isConnected()) return@launch
                
                if (!SignalingConstants.isValidTrackerId(deviceId) || !SignalingConstants.isValidViewerId(viewerId)) {
                    Log.e("GPS19_NET", "Handover aborted: Invalid identity detected (T:$deviceId, V:$viewerId)")
                    return@launch
                }

                logManager.logServiceEvent("Network Handover: Interface Available. Reconnecting relay.", false)
                lastReconnectTs = now
                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                wakeUpRelay()
            }
        }

        override fun onLost(network: Network) {
            if (isStopped) return
            logManager.logServiceEvent("Network Handover: Interface Lost.", false)
            telemetryRepository.updateRelayStatus(false)
        }
    }

    private fun startKeepAliveLoop() {
        if (isStopped) return
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                if (relayUrl.isNotEmpty()) {
                    try {
                        performKeepAlive()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        if (isActive && !isStopped) {
                            Log.e("GPS19_NET", "Keep-alive loop internal error: ${e.message}")
                        }
                    }
                }
                delay(NET_REJOIN_THRESHOLD_MS) 
            }
        }
    }

    private suspend fun performKeepAlive() {
        withContext(Dispatchers.IO) {
            try {
                val latestMode = settingsRepository.getAppMode() ?: (if (isTrackerMode) "tracker" else "viewer")
                val latestDeviceId = settingsRepository.getString(SettingsRepository.TRACKER_ID_KEY, deviceId)
                val latestViewerId = settingsRepository.getString(SettingsRepository.VIEWER_ID_KEY, viewerId)
                val latestRelayUrl = settingsRepository.getString(SettingsRepository.RELAY_URL_KEY, relayUrl)
                val latestIsTracker = latestMode == "tracker"

                if (latestDeviceId != deviceId || latestViewerId != viewerId || latestRelayUrl != relayUrl || latestIsTracker != isTrackerMode) {
                    
                    if (SignalingConstants.isValidTrackerId(latestDeviceId) && SignalingConstants.isValidViewerId(latestViewerId)) {
                        this@AppNetworkManager.deviceId = latestDeviceId
                        this@AppNetworkManager.viewerId = latestViewerId
                        this@AppNetworkManager.relayUrl = latestRelayUrl
                        this@AppNetworkManager.isTrackerMode = latestIsTracker
                        
                        if (isActive && !isStopped) {
                            withContext(Dispatchers.Main) {
                                lastReconnectTs = timeProvider.elapsedRealtime()
                                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                                wakeUpRelay()
                            }
                        }
                    } else {
                        Log.e("GPS19_NET", "Keep-alive identity update rejected: Malformed ID detected")
                    }
                    return@withContext 
                }

                val urlConnection = (URL(relayUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000; readTimeout = 30000
                    setRequestProperty("User-Agent", "GPS19-Monitor")
                }
                val code = urlConnection.responseCode
                urlConnection.disconnect()
                consecutiveHttpFailures.set(0)
                
                val now = timeProvider.elapsedRealtime()
                val isSocketReportedConnected = signalingProvider.isConnected()
                val lastTraffic = signalingProvider.getLastRelayTrafficTs()
                val trafficAge = now - lastTraffic

                if (isSocketReportedConnected) {
                    if (trafficAge > NET_REJOIN_THRESHOLD_MS) {
                        if (isActive && !isStopped) {
                            withContext(Dispatchers.Main) {
                                Log.w("GPS19_NET", "Socket silent for ${trafficAge}ms (HTTP $code). Forcing re-join.")
                                signalingProvider.updateIdentity(deviceId, viewerId, isTrackerMode, force = true)
                                wakeUpRelay() 
                            }
                        }
                    }
                } else {
                    if (isActive && !isStopped && (now - lastReconnectTs > NET_REJOIN_THRESHOLD_MS)) {
                        if (SignalingConstants.isValidTrackerId(deviceId) && SignalingConstants.isValidViewerId(viewerId)) {
                            withContext(Dispatchers.Main) {
                                lastReconnectTs = now
                                signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
                                wakeUpRelay()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException || !isActive || isStopped) throw e
                consecutiveHttpFailures.incrementAndGet()
                if (consecutiveHttpFailures.get() > 3) wakeUpRelay()
            }
        }
    }

    private fun wakeUpRelay() {
        if (relayUrl.isEmpty() || isStopped) return
        scope.launch(Dispatchers.IO) {
            repeat(4) { attempt ->
                try {
                    Log.d("GPS19_NET", "Wake-up (Attempt ${attempt+1}): GET $relayUrl")
                    val conn = URL(relayUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 30000; conn.readTimeout = 30000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (GPS19-Wakeup)")
                    val code = conn.responseCode
                    Log.d("GPS19_NET", "Wake-up (Attempt ${attempt+1}): Code $code")
                    logManager.logServiceEvent("Relay Wake-up (Attempt ${attempt+1}): Response $code", false)
                    conn.disconnect()
                    if (code == 200 || code == 404) return@launch 
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("GPS19_NET", "Wake-up (Attempt ${attempt+1}): ${e.message}")
                    if (!isStopped && isActive) {
                        logManager.logServiceEvent("Relay Wake-up (Attempt ${attempt+1}) Failed: ${e.message}", false)
                    }
                }
                delay(6000)
            }
        }
    }

    fun start(url: String, deviceId: String, viewerId: String, isTrackerMode: Boolean) {
        this.isStopped = false
        this.relayUrl = url; this.deviceId = deviceId; this.viewerId = viewerId; this.isTrackerMode = isTrackerMode
        this.lastReconnectTs = timeProvider.elapsedRealtime()
        
        try {
            val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) { Log.e("GPS19_NET", "Failed to register network callback") }

        signalingProvider.setConnectionLostCallback {
            if (!isStopped && relayUrl.isNotEmpty()) {
                val now = timeProvider.elapsedRealtime()
                if (now - lastReconnectTs > 10000L) {
                    lastReconnectTs = now
                    Log.i("GPS19_NET", "Reactive trigger: Transport lost. Waking up relay.")
                    wakeUpRelay()
                }
            }
        }

        if (relayUrl.isNotEmpty() && SignalingConstants.isValidTrackerId(deviceId) && SignalingConstants.isValidViewerId(viewerId)) {
            signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
            wakeUpRelay()
        } else {
            Log.e("GPS19_NET", "Initial start aborted: Invalid identity (T:$deviceId, V:$viewerId)")
        }
        startKeepAliveLoop()
    }

    fun updateIdentity(deviceId: String, viewerId: String, isTracker: Boolean) {
        if (isStopped) return
        if (!SignalingConstants.isValidTrackerId(deviceId) || !SignalingConstants.isValidViewerId(viewerId)) {
            Log.e("GPS19_NET", "Identity update rejected: Malformed ID")
            return
        }
        this.deviceId = deviceId; this.viewerId = viewerId; this.isTrackerMode = isTracker
        signalingProvider.updateIdentity(deviceId, viewerId, isTracker)
    }

    fun connect(url: String) {
        if (isStopped) return
        this.relayUrl = url; this.lastReconnectTs = timeProvider.elapsedRealtime()
        if (SignalingConstants.isValidTrackerId(deviceId) && SignalingConstants.isValidViewerId(viewerId)) {
            signalingProvider.connect(relayUrl, deviceId, viewerId, isTrackerMode)
            wakeUpRelay()
        }
    }

    fun stop() { 
        isStopped = true
        keepAliveJob?.cancel()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        signalingProvider.disconnect() 
    }

    fun isConnected() = signalingProvider.isConnected()
    fun getRtt() = signalingProvider.getRtt()
    fun clearRtt() = signalingProvider.clearRtt()

    fun emit(event: String, data: JSONObject) {
        if (isStopped) return
        signalingProvider.emit(event, data)
    }

    fun emitBinary(event: String, routingId: String, data: ByteArray) {
        if (isStopped) return
        signalingProvider.emitBinary(event, routingId, data)
    }

    fun pushSettings() {
        if (isStopped) return
        signalingProvider.pushSettings()
    }

    fun updateRelayStatus(connected: Boolean) { telemetryRepository.updateRelayStatus(connected) }

    fun sendTelemetry(status: TrackerStatus): Boolean {
        if (!isConnected()) return false
        // R988: Activate optimized binary telemetry for trackers.
        if (isTrackerMode) {
            val routingId = SignalingConstants.getTransmissionId(deviceId)
            emitBinary("location_update_bin", routingId, status.toProto(fromViewer = false).toByteArray())
        } else {
            emit("location_update", status.toJSONObject(fromViewer = true))
        }
        return true
    }
}
