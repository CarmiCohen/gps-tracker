package com.jd.app.sensor

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.SignalingClient
import com.jd.core.ui.FixReading
import com.jd.core.ui.HudState
import com.jd.core.ui.NO_VALUE
import com.jd.core.ui.toFixReading
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** Verifies Chapter 128.2 and 128.3: the Sensor HUD shows the service's real fix, never an invented one (ISSUE-NOV6.1-O27). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SensorHudValuesTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var identityManager: ForensicIdentityManager
    @Inject lateinit var signalingClient: SignalingClient
    @Inject lateinit var sensorDataStore: SensorDataStore
    @Inject lateinit var fixStore: SensorFixStore
    @Inject lateinit var serviceStatus: SensorServiceStatus

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var viewModel: SensorViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        fixStore.clear()
        viewModel = SensorViewModel(identityManager, signalingClient, sensorDataStore, fixStore, serviceStatus, context)
    }

    @After
    fun tearDown() {
        fixStore.clear()
    }

    // The initial value has no battery reading, so a match is always a state the ViewModel computed.
    private fun hudMatching(predicate: (HudState) -> Boolean): HudState = runBlocking {
        withTimeout(5_000L) { viewModel.hudState.first { it.batteryLevel != null && predicate(it) } }
    }

    @Test
    fun noFixShowsNoPositionValues() {
        val state = hudMatching { it.trackerState == NO_VALUE }

        assertEquals(NO_VALUE, state.trackerState)
        assertNull(state.speedKmh)
        assertEquals("-- km/h", state.speedText)
        assertEquals(0f, state.accuracyMeters, 0f)
        assertEquals(-1L, state.telemetryAgeSec)
    }

    @Test
    fun showsThePublishedFix() {
        fixStore.publish(FixReading(System.currentTimeMillis() - 3_000L, 12.5f, 2f))

        val state = hudMatching { it.accuracyMeters == 12.5f }

        assertEquals("MOVING", state.trackerState)
        assertEquals("7.2 km/h", state.speedText)
        // The ticker supplies the clock and may be up to 1s behind the publish.
        assertTrue("Age ${state.telemetryAgeSec}", state.telemetryAgeSec in 2L..4L)
        assertTrue(state.telemetryText, state.telemetryText.contains("|A:±12.5m|AGE:${state.telemetryAgeSec}s"))
    }

    @Test
    fun clearingTheFixRemovesItsValues() {
        fixStore.publish(FixReading(System.currentTimeMillis(), 7f, 0f))
        hudMatching { it.trackerState == "STATIONARY" }

        fixStore.clear()

        val state = hudMatching { it.trackerState == NO_VALUE }
        assertEquals(-1L, state.telemetryAgeSec)
        assertEquals(0f, state.accuracyMeters, 0f)
    }

    @Test
    fun aLocationWithoutSpeedOrAccuracyReportsNeither() {
        val bare = Location(LocationManager.GPS_PROVIDER).apply { time = 1_234L }

        assertEquals(FixReading(1_234L, 0f, null), bare.toFixReading())
    }

    @Test
    fun aLocationKeepsItsSpeedAccuracyAndTime() {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            time = 5_678L
            accuracy = 4.5f
            speed = 3f
        }

        assertEquals(FixReading(5_678L, 4.5f, 3f), location.toFixReading())
    }
}
