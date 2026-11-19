package com.jd.app.monitor

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chapter 227.2 (ISSUE-NOV10.8-O86): on the device, a new Sensor phone warning posts the notification on its own
 * default-importance channel, the same warnings again do not post it anew, an older copy is ignored, and a fixed setup
 * removes it. The pairing is kept in a file of this test, so the app's own pairing is not touched. Needs the
 * notification permission (`adb shell pm grant com.jd.app.monitor android.permission.POST_NOTIFICATIONS`).
 */
@RunWith(AndroidJUnit4::class)
class SensorSetupNoticeDeviceTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val identity = ForensicIdentityManager(context, "sensor_setup_notice_test.preferences_pb")
    private val dataStore = MonitorDataStore(context)
    private val reports = SensorSetupReports(context, identity, dataStore)

    private fun setupNotification() = notifications.activeNotifications.singleOrNull { it.id == SENSOR_SETUP_NOTIFICATION_ID }

    private fun shownText() = setupNotification()?.notification?.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

    @Before
    fun setUp() = runBlocking {
        createMonitorNotificationChannels(context)
        identity.setTargetId("T")
        reports.forget()
    }

    @After
    fun tearDown() = runBlocking {
        reports.forget()
        identity.clearTargetId()
    }

    @Test
    fun theSetupChannelIsDefaultImportance() {
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, notifications.getNotificationChannel(MonitorApp.SETUP_CHANNEL_ID).importance)
    }

    @Test
    fun aNewWarningIsNotifiedOnceAndAFixedSetupRemovesIt() = runBlocking {
        val missing = SensorSetupReport(trackerId = "T", missing = listOf(SensorSetupItem.BATTERY_UNRESTRICTED), sentAtMs = 1_000L)
        reports.take(missing, "relay")
        val first = setupNotification()
        assertEquals(MonitorApp.SETUP_CHANNEL_ID, first?.notification?.channelId)
        assertEquals(setOf("Battery 'Unrestricted': missing"), dataStore.notifiedSetupWarnings.first())

        reports.take(missing, "push") // The same report by the other road.
        assertEquals("Not posted again", first?.postTime, setupNotification()?.postTime)

        val worse = missing.copy(faults = listOf(SensorFault.LOCATION_NEEDS_TAP), sentAtMs = 2_000L)
        reports.take(worse, "push")
        assertTrue("A new warning posts the list again: ${shownText()}", shownText()!!.contains(SensorFault.LOCATION_NEEDS_TAP.text))
        assertEquals(2, dataStore.notifiedSetupWarnings.first().size)

        reports.take(missing.copy(sentAtMs = 1_500L), "push") // Held while the phone dozed.
        assertEquals("The older copy is ignored", worse, reports.latest.value)

        reports.take(SensorSetupReport(trackerId = "T", sentAtMs = 3_000L), "relay")
        assertNull("A fixed setup removes the notification", setupNotification())
        assertEquals(emptySet<String>(), dataStore.notifiedSetupWarnings.first())
    }

    @Test
    fun anotherTrackersReportIsIgnored() = runBlocking {
        reports.take(SensorSetupReport(trackerId = "X", missing = listOf(SensorSetupItem.MICROPHONE), sentAtMs = 1_000L), "push")
        assertNull(reports.latest.value)
        assertNull(setupNotification())
    }
}
