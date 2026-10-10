package com.gps19.app

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.gps19.core.engine.ActivityType
import com.gps19.core.engine.TimeProvider
import com.gps19.core.engine.VIBRATION_SUSPICIOUS_THRESHOLD_G
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ActivityContextProvider: Unified authority for tracking user activity context.
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to property-based 
 *   TimeProvider API and fixed property invocation errors.
 */
@Singleton
class ActivityContextProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    @Volatile var currentActivityType: ActivityType = ActivityType.UNKNOWN
        private set

    @Volatile private var lastActivityUpdateRt = 0L
    private val activityRecognitionClient by lazy { ActivityRecognition.getClient(context) }
    private val ACTIVITY_RECEIVER_ACTION = "com.gps19.app.ACTIVITY_RECEIVER_ACTION"
    private var activityPendingIntent: PendingIntent? = null

    private val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTIVITY_RECEIVER_ACTION) {
                val result = ActivityRecognitionResult.extractResult(intent) ?: return
                val mostProbable = result.mostProbableActivity
                
                val nextActivity = when (mostProbable.type) {
                    DetectedActivity.STILL -> ActivityType.STILL
                    DetectedActivity.WALKING -> ActivityType.WALKING
                    DetectedActivity.RUNNING -> ActivityType.RUNNING
                    DetectedActivity.ON_BICYCLE -> ActivityType.BICYCLING
                    DetectedActivity.IN_VEHICLE -> ActivityType.IN_VEHICLE
                    DetectedActivity.TILTING -> ActivityType.TILTING
                    else -> ActivityType.UNKNOWN
                }
                
                if (nextActivity != ActivityType.UNKNOWN) {
                    currentActivityType = nextActivity
                    lastActivityUpdateRt = timeProvider.elapsedRealtime
                    Timber.d("ActivityContextProvider: Activity Recognition Update: $nextActivity (${mostProbable.confidence}%)")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Timber.w("ActivityContextProvider: Activity Recognition permission NOT granted.")
            return
        }

        try {
            val intent = Intent(ACTIVITY_RECEIVER_ACTION).setPackage(context.packageName)
            activityPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            
            context.registerReceiver(activityReceiver, IntentFilter(ACTIVITY_RECEIVER_ACTION), 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_NOT_EXPORTED else 0
            )

            activityRecognitionClient.requestActivityUpdates(60000L, activityPendingIntent!!)
                .addOnSuccessListener { Timber.i("ActivityContextProvider: Activity Recognition requested.") }
                .addOnFailureListener { Timber.e(it, "ActivityContextProvider: Activity Recognition failed to start.") }
        } catch (e: SecurityException) {
            Timber.e(e, "ActivityContextProvider: Activity Recognition permission revoked mid-session.")
        } catch (e: Exception) {
            Timber.e(e, "ActivityContextProvider: Error starting Activity Recognition.")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            activityPendingIntent?.let { activityRecognitionClient.removeActivityUpdates(it) }
            context.unregisterReceiver(activityReceiver)
        } catch (e: SecurityException) {
            Timber.e(e, "ActivityContextProvider: SecurityException during removeActivityUpdates.")
        } catch (e: Exception) {
            // Ignore unregistration errors
        } finally {
            activityPendingIntent = null
        }
    }

    fun updateActivityHeuristic(speedMps: Double, vibe: Double, isStationary: Boolean, adaptiveVibrationFloor: Double) {
        // If we haven't had an Activity Recognition update in 2 minutes, fallback to heuristics
        if (timeProvider.elapsedRealtime - lastActivityUpdateRt < 120000L) return

        currentActivityType = when {
            speedMps > 10.0 -> ActivityType.IN_VEHICLE
            speedMps > 1.2 -> ActivityType.WALKING
            isStationary && vibe < (adaptiveVibrationFloor * 0.8) -> ActivityType.STILL
            vibe > VIBRATION_SUSPICIOUS_THRESHOLD_G -> ActivityType.WALKING
            else -> ActivityType.UNKNOWN
        }
    }

    fun reset() {
        currentActivityType = ActivityType.UNKNOWN
        lastActivityUpdateRt = 0L
    }
}
