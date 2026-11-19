package com.jd.app.monitor

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ISSUE-NOV12.3-O90 (chapter 231.1): while instrumented tests run, [HiltTestRunner] keeps [MonitorMessagingService]
 * off, so a push from a running Sensor cannot start it in the test process and crash the run.
 * That it is on again after the run is checked on the device (chapter 231.2).
 */
@RunWith(AndroidJUnit4::class)
class MessagingServiceOffDuringRunsTest {

    @Test
    fun theMessagingServiceIsOffDuringTheRun() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val state = app.packageManager.getComponentEnabledSetting(ComponentName(app, MonitorMessagingService::class.java))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state)
    }
}
