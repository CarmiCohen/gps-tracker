package com.jd.app.sensor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.shared.ForensicIdentity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Verifies chapter 187: the Sensor's instrumented tests write only their own identity file, so a run
 * with `am instrument` never resets the installed Sensor's identity (ISSUE-NOV12.6-O93).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IdentityIsolationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var identityManager: ForensicIdentityManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testWritesNeverReachTheInstalledSensorsIdentity() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // The installed Sensor's own store, only read here.
        val installed = ForensicIdentityManager(context)
        val selfBefore = installed.selfId.first()

        identityManager.setSelfId("isolation_check")
        assertEquals("isolation_check", identityManager.selfId.first())
        identityManager.resetIdentity(ForensicIdentity.PREFIX_SENSOR)

        assertEquals("The installed identity must not change", selfBefore, installed.selfId.first())
        val testFile = com.jd.core.shared.BackupStoreFile.location(
            context.createDeviceProtectedStorageContext().filesDir, TestIdentityModule.TEST_IDENTITY_FILE
        )
        assertTrue("The tests must write their own identity file", testFile.exists())
    }
}
