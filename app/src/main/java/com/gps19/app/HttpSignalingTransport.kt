package com.gps19.app

import com.gps19.core.engine.SignalingTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HttpSignalingTransport: Production implementation of SignalingTransport 
 * using standard HttpURLConnection.
 */
@Singleton
class HttpSignalingTransport @Inject constructor() : SignalingTransport {

    override suspend fun performKeepAlive(url: String): Int = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 30000
                setRequestProperty("User-Agent", "GPS19-Monitor")
            }
            conn.responseCode
        } finally {
            conn?.disconnect()
        }
    }

    override suspend fun wakeUpRelay(url: String) = withContext(Dispatchers.IO) {
        repeat(4) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "GPS19-Wakeup")
                conn.responseCode
                return@withContext
            } catch (e: Exception) {
                delay(6000)
            } finally {
                conn?.disconnect()
            }
        }
    }
}
