package com.gps19.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.gps19.core.engine.PowerStateProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * PowerIntegrationAuditTest: High-fidelity integration test for Doze awareness.
 * Uses adb shell commands to force the device into Doze mode and verifies 
 * the AndroidPowerStateProvider correctly reports the state.
 * Oct.13.3:
 * - Hardening: Added "dumpsys deviceidle step" loop to ensure the device 
 *   reaches the deep IDLE state reliably across different manufacturers.
 * Sep.16.13: Issue #1050/1052 Verification (R-ID 351).
 */
@HiltAndroidTest
class PowerIntegrationAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var powerStateProvider: PowerStateProvider

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("dumpsys deviceidle unforce")
    }

    @After
    fun teardown() {
        device.executeShellCommand("dumpsys deviceidle unforce")
    }

    @Test
    fun testDozeStateDetection_RealIntegration() {
        // 1. Initial State: Should not be idle
        assertFalse("Device should not be in idle mode initially", powerStateProvider.isDeviceIdleMode)

        // 2. Force Idle (Doze)
        device.executeShellCommand("dumpsys deviceidle force-idle")
        
        // Hardening: Step through phases until IDLE is reached
        var attempts = 0
        while (!powerStateProvider.isDeviceIdleMode && attempts < 10) {
            device.executeShellCommand("dumpsys deviceidle step")
            Thread.sleep(500)
            attempts++
        }

        // 3. Verify Deferral Detection
        assertTrue("PowerStateProvider should detect Doze mode after force-idle/step sequence", powerStateProvider.isDeviceIdleMode)

        // 4. Restore State
        device.executeShellCommand("dumpsys deviceidle unforce")
        Thread.sleep(1000)
        
        assertFalse("PowerStateProvider should detect exit from Doze mode after unforce", powerStateProvider.isDeviceIdleMode)
    }
}
