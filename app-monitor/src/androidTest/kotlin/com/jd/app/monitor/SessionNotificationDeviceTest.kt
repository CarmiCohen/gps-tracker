package com.jd.app.monitor

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chapter 213.1: the Monitor's session notification opens the Monitor when tapped, also from Android's Active apps
 * list, the way the launcher does (ISSUE-NOV17.14-O113).
 */
@RunWith(AndroidJUnit4::class)
class SessionNotificationDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theOpenIntentStartsTheMonitorLikeTheLauncher() {
        val intent = monitorOpenIntent(context)

        assertEquals(MonitorActivity::class.java.name, intent.component?.className)
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_LAUNCHER))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED != 0)
        // The launcher entry resolves to the same activity, so an open Monitor task is brought back, not doubled.
        val resolved = context.packageManager.resolveActivity(intent, 0)
        assertEquals(MonitorActivity::class.java.name, resolved?.activityInfo?.name)
    }

    @Test
    fun theSessionNotificationHasATapAction() {
        createMonitorNotificationChannels(context)
        val notification = buildSessionNotification(context)

        assertNotNull(notification.contentIntent)
        assertEquals(context.packageName, notification.contentIntent.creatorPackage)
        assertEquals(MonitorApp.SESSION_CHANNEL_ID, notification.channelId)
    }
}
