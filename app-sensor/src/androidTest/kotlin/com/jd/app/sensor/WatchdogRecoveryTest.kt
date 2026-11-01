package com.jd.app.sensor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.ServiceTestRule
import com.jd.core.shared.TickOrchestrator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * WatchdogRecoveryTest: Audits the Forensic Watchdog (R-ID 85).
 * Verifies that the heartbeat mechanism correctly tracks loop activity.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WatchdogRecoveryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val serviceRule = ServiceTestRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    )

    @Inject
    lateinit var orchestrator: TickOrchestrator

    @Before
    fun init() {
        hiltRule.inject()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val serviceChannel = NotificationChannel(
                SensorApp.CHANNEL_ID,
                "GPS Sensor Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    @Test
    fun testWatchdogHeartbeatDetection() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val intent = Intent(context, SensorService::class.java)
            
            // Start the service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            // Give it a moment to start the loop and notification
            delay(3000)
            
            // 1. Verify loop is running and heartbeat is updated
            val elapsedNanos = orchestrator.getNanosecondsSinceLastTick("sensing_loop")
            assertTrue("Sensing loop should be running, but heartbeat is missing", elapsedNanos != -1L)
            
            // 2. Wait for a few ticks and check if elapsed time remains fresh.
            // Note: In stationary mode, the interval is 10s (10000ms).
            // Threshold is set to 12s to account for the slowest possible tick interval.
            delay(2000)
            val elapsedNanosAfter = orchestrator.getNanosecondsSinceLastTick("sensing_loop")
            val elapsedMs = elapsedNanosAfter / 1_000_000
            assertTrue("Heartbeat should be fresh (elapsed: ${elapsedMs}ms, threshold: 12000ms)", elapsedMs < 12000L)

            context.stopService(intent)
        }
    }
}
