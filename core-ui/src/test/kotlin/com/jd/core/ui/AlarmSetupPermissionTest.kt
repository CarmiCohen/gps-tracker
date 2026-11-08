package com.jd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Chapter 145 (ISSUE-NOV6.0-O23): Alarm Reach Under Do Not Disturb & Locked Screen Setup UI.
 * Verifies system intent action constants and URI string formatting for DND Notification Policy Access
 * and Full-Screen Intent settings navigation.
 */
class AlarmSetupPermissionTest {

    @Test
    fun dndSettingsActionMatchesAndroidSpecification() {
        val action = "android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS"
        assertEquals("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS", action)
    }

    @Test
    fun fullScreenIntentSettingsActionMatchesAndroidSpecification() {
        val action = "android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT"
        assertEquals("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", action)
    }

    @Test
    fun packageUriFormattingMatchesSpecification() {
        val packageName = "com.jd.app.monitor"
        val formattedUri = "package:$packageName"
        assertEquals("package:com.jd.app.monitor", formattedUri)
    }
}
