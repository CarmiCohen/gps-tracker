package com.gps19.app

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.gps19.core.engine.TimeProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.util.GeoPoint

/**
 * MainFileHelper: Handles importing and exporting configuration and telemetry data.
 * v8.9.75:
 * - Issue #014: Type Safety Optimization. Standardized telemetry fields to Double 
 *   to eliminate redundant toDouble()/toFloat() conversions.
 * v8.9.42:
 * - Issue #325: Authoritative Spatial Anchoring (Dual-Metric).
 */
object MainFileHelper {

    private fun getPublicAppFolder(context: Context): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(docsDir, "GPS-Tracker")
        if (!appDir.exists()) appDir.mkdirs()
        return appDir
    }

    fun getUnifiedFileName(category: String, source: String, deviceId: String, isAuto: Boolean, timeProvider: TimeProvider, extension: String = "json"): String {
        val prefix = if (isAuto) "A_" else "M_"
        val sdf = SimpleDateFormat("d_HH.mm", Locale.getDefault())
        val baseName = "${category}_${source}_${deviceId}_${sdf.format(Date(timeProvider.currentTimeMillis()))}.$extension"
        return prefix + baseName.replace("_system", "")
    }

    fun importConfig(context: Context, viewModel: MainViewModel, uri: Uri) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return
            val json = JSONObject(content)
            val target = if (json.has("config")) json.getJSONObject("config") 
                        else if (json.has("settings_snapshot")) json.getJSONObject("settings_snapshot") 
                        else json
            
            val isTrackerMode = viewModel.uiState.value.appMode == "tracker"
            
            var deviceId: String? = null
            var viewerId: String? = null
            var relayUrl: String? = null
            var maxDistance: Double? = null
            var homePoints: List<GeoPoint>? = null
            var alertSettings: AlertSettings? = null

            if (target.has("deviceId")) deviceId = target.getString("deviceId")
            if (!isTrackerMode && target.has("viewerId")) viewerId = target.getString("viewerId")
            if (target.has("relayUrl")) relayUrl = target.getString("relayUrl")
            
            if (target.has("maxDistance")) {
                maxDistance = target.getDouble("maxDistance")
            }

            if (target.has("home_points")) {
                val arr = target.getJSONArray("home_points")
                val points = mutableListOf<GeoPoint>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    points.add(GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")))
                }
                homePoints = points
            }

            if (target.has("alertSettings")) {
                val s = target.getJSONObject("alertSettings")
                alertSettings = AlertSettings(
                    localInternet = s.optBoolean("localInternet", true),
                    serverConnection = s.optBoolean("serverConnection", true),
                    relayConnection = s.optBoolean("relayConnection", true),
                    jammerDetection = s.optBoolean("jammerDetection", true),
                    signalLoss = s.optBoolean("signalLoss", true),
                    gpsStalling = s.optBoolean("gpsStalling", true),
                    distance = s.optBoolean("distance", true),
                    power = s.optBoolean("power", true),
                    lowBattery = s.optBoolean("lowBattery", true),
                    longTimeGap = s.optBoolean("longTimeGap", true),
                    highTemperature = s.optBoolean("highTemperature", true),
                    overrideSilence = s.optBoolean("overrideSilence", true),
                    useMaxVolume = s.optBoolean("useMaxVolume", true),
                    vibrationEnabled = s.optBoolean("vibrationEnabled", true),
                    alarmVolume = s.optDouble("alarmVolume", 0.8).toFloat(),
                    useCustomVolume = s.optBoolean("useCustomVolume", false),
                    tiltAlert = s.optBoolean("tiltAlert", true),
                    acousticAlert = s.optBoolean("acousticAlert", true),
                    liftAlert = s.optBoolean("liftAlert", true),
                    tamperAlert = s.optBoolean("tamperAlert", true),
                    chairOccupied = s.optBoolean("chairOccupied", true)
                )
            }
            
            viewModel.onEvent(UiEvent.BulkUpdateSettings(
                deviceId = deviceId,
                viewerId = viewerId,
                relayUrl = relayUrl,
                maxDistance = maxDistance,
                homePoints = homePoints,
                alertSettings = alertSettings
            ))
            
            Toast.makeText(context, "Settings loaded successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Loading failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun importTrails(context: Context, viewModel: MainViewModel, uris: List<Uri>) {
        if (uris.isEmpty()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = viewModel.repository
                var totalPoints = 0
                var filesSuccess = 0

                for (uri in uris) {
                    try {
                        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream)).readText()
                        } ?: continue
                        
                        val json = JSONObject(content)
                        var filePoints = 0
                        
                        if (json.has("tracker_trail") || json.has("viewer_trail")) {
                            if (json.has("tracker_trail")) {
                                val arr = json.getJSONArray("tracker_trail")
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    val ts = obj.optLong("timestamp", 0L)
                                    val acc = obj.optDouble("accuracy", 0.0)
                                    val maxAcc = obj.optDouble("max_accuracy", 0.0)
                                    repository.saveTrailPoint(obj.getDouble("lat"), obj.getDouble("lng"), isViewer = false, timestamp = if (ts > 0) ts else null, force = true, accuracy = acc, maxAccuracy = maxAcc)
                                }
                                filePoints += arr.length()
                            }
                            if (json.has("viewer_trail")) {
                                val arr = json.getJSONArray("viewer_trail")
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    val ts = obj.optLong("timestamp", 0L)
                                    val acc = obj.optDouble("accuracy", 0.0)
                                    val maxAcc = obj.optDouble("max_accuracy", 0.0)
                                    repository.saveTrailPoint(obj.getDouble("lat"), obj.getDouble("lng"), isViewer = true, timestamp = if (ts > 0) ts else null, force = true, accuracy = acc, maxAccuracy = maxAcc)
                                }
                                filePoints += arr.length()
                            }
                        } 
                        else if (json.has("points")) {
                            val arr = json.getJSONArray("points")
                            val isViewer = json.optString("role") == "viewer" || json.optString("source") == "viewer"
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val ts = obj.optLong("timestamp", 0L)
                                val acc = obj.optDouble("accuracy", 0.0)
                                val maxAcc = obj.optDouble("max_accuracy", 0.0)
                                repository.saveTrailPoint(obj.getDouble("lat"), obj.getDouble("lng"), isViewer = isViewer, timestamp = if (ts > 0) ts else null, force = true, accuracy = acc, maxAccuracy = maxAcc)
                            }
                            filePoints += arr.length()
                        }
                        
                        if (filePoints > 0) {
                            totalPoints += filePoints
                            filesSuccess++
                        }
                    } catch (e: Exception) {
                        Log.e("GPS19", "Error importing file $uri: ${e.message}")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Restored $totalPoints points from $filesSuccess files", Toast.LENGTH_SHORT).show()
                    viewModel.addPersistentLog("user", "USER ACTION: Imported $filesSuccess trail files ($totalPoints points accumulated)", true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Trail loading failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun manualExportLogs(context: Context, viewModel: MainViewModel, timeProvider: TimeProvider) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = viewModel.uiState.value
                val deviceId = state.deviceId
                val appMode = state.appMode
                val settings = JSONObject().apply {
                    put("deviceId", state.deviceId)
                    put("viewerId", state.viewerId)
                    put("relayUrl", state.relayUrl)
                    put("maxDistance", state.maxDistance)
                    put("role", appMode)
                    put("home_points", JSONArray().apply {
                        state.homePoints.forEach { pt ->
                            put(JSONObject().apply { put("lat", pt.latitude); put("lng", pt.longitude) })
                        }
                    })
                }

                val logs = (viewModel.eventLogsFlow as StateFlow<List<LogEntry>>).value
                val root = JSONObject().apply {
                    put("exported_at", timeProvider.currentTimeMillis())
                    put("role", appMode)
                    put("settings_snapshot", settings)
                    put("logs", JSONArray().apply { logs.forEach { put(it.toJSONObject()) } })
                }
                
                val fileName = getUnifiedFileName("logs", appMode ?: "system", deviceId, isAuto = false, timeProvider)
                val file = File(getPublicAppFolder(context), fileName)
                FileOutputStream(file).use { it.write(root.toString(4).toByteArray()) }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Logs saved to Documents/GPS-Tracker", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("GPS19", "Manual logs save failed: ${e.message}")
                }
            }
        }
    }

    fun manualExportConfig(context: Context, viewModel: MainViewModel, timeProvider: TimeProvider) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = viewModel.uiState.value
                val deviceId = state.deviceId
                val alertSettings = state.alertSettings
                val appMode = state.appMode
                
                val settingsJson = JSONObject().apply {
                    put("deviceId", state.deviceId)
                    put("viewerId", state.viewerId)
                    put("relayUrl", state.relayUrl)
                    put("maxDistance", state.maxDistance)
                    put("role", appMode)
                    put("home_points", JSONArray().apply {
                        state.homePoints.forEach { pt ->
                            put(JSONObject().apply { put("lat", pt.latitude); put("lng", pt.longitude) })
                        }
                    })
                    put("alertSettings", JSONObject().apply {
                        put("localInternet", alertSettings.localInternet)
                        put("serverConnection", alertSettings.serverConnection)
                        put("relayConnection", alertSettings.relayConnection)
                        put("jammerDetection", alertSettings.jammerDetection)
                        put("signalLoss", alertSettings.signalLoss)
                        put("gpsStalling", alertSettings.gpsStalling)
                        put("distance", alertSettings.distance)
                        put("power", alertSettings.power)
                        put("lowBattery", alertSettings.lowBattery)
                        put("longTimeGap", alertSettings.longTimeGap)
                        put("highTemperature", alertSettings.highTemperature)
                        put("overrideSilence", alertSettings.overrideSilence)
                        put("useMaxVolume", alertSettings.useMaxVolume)
                        put("vibrationEnabled", alertSettings.vibrationEnabled)
                        put("alarmVolume", alertSettings.alarmVolume)
                        put("useCustomVolume", alertSettings.useCustomVolume)
                        put("tiltAlert", alertSettings.tiltAlert)
                        put("acousticAlert", alertSettings.acousticAlert)
                        put("liftAlert", alertSettings.liftAlert)
                        put("tamperAlert", alertSettings.tamperAlert)
                        put("chairOccupied", alertSettings.chairOccupied)
                    })
                }

                val root = JSONObject().apply {
                    put("exported_at", timeProvider.currentTimeMillis())
                    put("role", appMode)
                    put("config", settingsJson)
                }
                
                val fileName = getUnifiedFileName("config", "backup", deviceId, isAuto = false, timeProvider)
                val file = File(getPublicAppFolder(context), fileName)
                FileOutputStream(file).use { it.write(root.toString(4).toByteArray()) }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Config saved to Documents/GPS-Tracker", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Config save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun manualExportTrails(context: Context, viewModel: MainViewModel, timeProvider: TimeProvider) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = viewModel.uiState.value
                val deviceId = state.deviceId
                val trackerTrail = viewModel.trackerTrailFlow.value
                val viewerTrail = viewModel.viewerTrailFlow.value
                val appMode = state.appMode

                if (trackerTrail.isEmpty() && viewerTrail.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No trail data to save", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val root = JSONObject().apply {
                    put("exported_at", timeProvider.currentTimeMillis())
                    put("device_id", deviceId)
                    put("role", appMode)
                    put("tracker_trail", JSONArray().apply {
                        trackerTrail.forEach { pt ->
                            put(JSONObject().apply { 
                                put("lat", pt.lat)
                                put("lng", pt.lng)
                                put("timestamp", pt.timestamp)
                                put("role", "tracker")
                                if (pt.accuracy > 0) put("accuracy", pt.accuracy)
                                if (pt.maxAccuracy > 0) put("max_accuracy", pt.maxAccuracy)
                            })
                        }
                    })
                    put("viewer_trail", JSONArray().apply {
                        viewerTrail.forEach { pt ->
                            put(JSONObject().apply { 
                                put("lat", pt.lat)
                                put("lng", pt.lng)
                                put("timestamp", pt.timestamp)
                                put("role", "viewer")
                                if (pt.accuracy > 0) put("accuracy", pt.accuracy)
                                if (pt.maxAccuracy > 0) put("max_accuracy", pt.maxAccuracy)
                            })
                        }
                    })
                }

                val fileName = getUnifiedFileName("trails_backup", "unified", deviceId, isAuto = false, timeProvider)
                val file = File(getPublicAppFolder(context), fileName)
                FileOutputStream(file).use { it.write(root.toString(4).toByteArray()) }

                saveTrailDataInternal(context, trackerTrail, "trail", "tracker", deviceId, isAuto = false, timeProvider)
                saveTrailDataInternal(context, viewerTrail, "trail", "viewer", deviceId, isAuto = false, timeProvider)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Trails backup saved to Documents/GPS-Tracker", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Manual trail save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun autoExportData(context: Context, repository: MainRepository, timeProvider: TimeProvider) {
        try {
            val now = timeProvider.currentTimeMillis()
            val deviceId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
            val appMode = repository.getAppMode()
            
            val logs = repository.loadAllLogsStatic()
            val logsJson = JSONObject().apply {
                put("exported_at", now)
                put("device_id", deviceId)
                put("role", appMode)
                put("logs", JSONArray().apply { logs.forEach { put(it.toJSONObject()) } })
            }
            val logsFileName = getUnifiedFileName("logs", appMode ?: "system", deviceId, isAuto = true, timeProvider)
            val logsFile = File(getPublicAppFolder(context), logsFileName)
            FileOutputStream(logsFile).use { it.write(logsJson.toString(4).toByteArray()) }

            val trackerTrail = repository.loadTrackerState()?.let { repository.loadTrailStatic(isViewer = false) } ?: emptyList()
            saveTrailDataInternal(context, trackerTrail, "trail", "tracker", deviceId, isAuto = true, timeProvider)

            val viewerTrail = repository.loadTrailStatic(isViewer = true)
            saveTrailDataInternal(context, viewerTrail, "trail", "viewer", deviceId, isAuto = true, timeProvider)
            
            Log.i("GPS19", "AUTO-SAVE: Completed successfully")
        } catch (e: Exception) {
            Log.e("GPS19", "AUTO-SAVE: Failed: ${e.message}")
        }
    }

    private fun saveTrailDataInternal(context: Context, trail: List<TrailPoint>, category: String, source: String, deviceId: String, isAuto: Boolean, timeProvider: TimeProvider): Boolean {
        if (trail.isEmpty()) return false
        return try {
            val json = JSONObject().apply {
                val arr = JSONArray()
                trail.forEach { 
                    arr.put(JSONObject().apply {
                        put("lat", it.lat)
                        put("lng", it.lng)
                        put("timestamp", it.timestamp)
                        put("role", source)
                        if (it.accuracy > 0) put("accuracy", it.accuracy)
                        if (it.maxAccuracy > 0) put("max_accuracy", it.maxAccuracy)
                    })
                }
                put("points", arr)
                put("timestamp", timeProvider.currentTimeMillis())
                put("role", source) 
                put("source", source)
                put("device_id", deviceId)
            }
            val fileName = getUnifiedFileName(category, source, deviceId, isAuto, timeProvider)
            val file = File(getPublicAppFolder(context), fileName)
            FileOutputStream(file).use { it.write(json.toString().toByteArray()) }
            true
        } catch (e: Exception) {
            Log.e("GPS19", "Trail save failed ($source): ${e.message}")
            false
        }
    }

    fun performDailyArchiving(context: Context, timeProvider: TimeProvider) {
        try {
            val appFolder = getPublicAppFolder(context)
            val now = timeProvider.currentTimeMillis()
            val thresholdMs = 28 * 60 * 60 * 1000L
            val cutoff = now - thresholdMs
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetFolderDate = dateFormat.format(Date(now))
            
            val archiveFolder = File(appFolder, targetFolderDate)
            
            val files = appFolder.listFiles() ?: return
            var movedCount = 0
            
            for (file in files) {
                if (file.isFile && file.lastModified() < cutoff) {
                    if (!archiveFolder.exists()) archiveFolder.mkdirs()
                    
                    var targetFile = File(archiveFolder, file.name)
                    if (targetFile.exists()) {
                        val baseName = file.name.substringBeforeLast(".")
                        val extension = file.name.substringAfterLast(".", "")
                        val suffix = if (extension.isNotEmpty()) ".$extension" else ""
                        var counter = 1
                        while (targetFile.exists()) {
                            targetFile = File(archiveFolder, "${baseName}_$counter$suffix")
                            counter++
                        }
                    }

                    if (file.renameTo(targetFile)) {
                        movedCount++
                    } else {
                        Log.e("GPS19", "ARCHIVING: Failed to move ${file.name}")
                    }
                }
            }
            if (movedCount > 0) Log.i("GPS19", "ARCHIVING: Moved $movedCount files to $targetFolderDate folder")
        } catch (e: Exception) {
            Log.e("GPS19", "ARCHIVING: Failed: ${e.message}")
        }
    }
}
