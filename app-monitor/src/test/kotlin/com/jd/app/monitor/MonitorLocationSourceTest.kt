package com.jd.app.monitor

import android.location.Location
import android.location.LocationManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class MonitorLocationSourceTest {

    private fun createLocation(
        provider: String,
        accuracy: Float,
        timeMs: Long
    ): Location {
        val location = mock(Location::class.java)
        `when`(location.provider).thenReturn(provider)
        `when`(location.hasAccuracy()).thenReturn(true)
        `when`(location.accuracy).thenReturn(accuracy)
        `when`(location.time).thenReturn(timeMs)
        `when`(location.elapsedRealtimeNanos).thenReturn(timeMs * 1_000_000L)
        return location
    }

    @Test
    fun initialFixIsAccepted() {
        val fix = createLocation(LocationManager.GPS_PROVIDER, 10f, 1000L)
        assertTrue(MonitorLocationSource.isBetterLocation(fix, null))
    }

    @Test
    fun moreAccurateFixIsAccepted() {
        val oldFix = createLocation(LocationManager.NETWORK_PROVIDER, 50f, 1000L)
        val newFix = createLocation(LocationManager.GPS_PROVIDER, 10f, 2000L)
        assertTrue(MonitorLocationSource.isBetterLocation(newFix, oldFix))
    }

    @Test
    fun coarseNetworkFixRejectedWhenGpsFixIsFresh() {
        // ISSUE-NOV6.1-O28: Stationary GPS fix at 11m accuracy vs coarse Network fix at 77.6m accuracy 15s later
        val gpsFix = createLocation(LocationManager.GPS_PROVIDER, 11f, 10_000L)
        val coarseNetworkFix = createLocation(LocationManager.NETWORK_PROVIDER, 77.6f, 25_000L)
        assertFalse(MonitorLocationSource.isBetterLocation(coarseNetworkFix, gpsFix))
    }

    @Test
    fun coarseNetworkFixAcceptedWhenGpsFixIsStale() {
        // GPS fix is older than 2 minutes (120s), so coarse network fix is accepted as fallback
        val staleGpsFix = createLocation(LocationManager.GPS_PROVIDER, 11f, 10_000L)
        val networkFix = createLocation(LocationManager.NETWORK_PROVIDER, 77.6f, 140_000L)
        assertTrue(MonitorLocationSource.isBetterLocation(networkFix, staleGpsFix))
    }

    @Test
    fun gpsFixImmediatelyReplacesNetworkFix() {
        val networkFix = createLocation(LocationManager.NETWORK_PROVIDER, 77.6f, 10_000L)
        val gpsFix = createLocation(LocationManager.GPS_PROVIDER, 11f, 12_000L)
        assertTrue(MonitorLocationSource.isBetterLocation(gpsFix, networkFix))
    }

    @Test
    fun newerFixFromSameProviderAccepted() {
        val oldNetworkFix = createLocation(LocationManager.NETWORK_PROVIDER, 77.6f, 10_000L)
        val newNetworkFix = createLocation(LocationManager.NETWORK_PROVIDER, 75.0f, 20_000L)
        assertTrue(MonitorLocationSource.isBetterLocation(newNetworkFix, oldNetworkFix))
    }

    @Test
    fun significantlyOlderFixRejected() {
        val currentFix = createLocation(LocationManager.GPS_PROVIDER, 10f, 150_000L)
        val oldIncomingFix = createLocation(LocationManager.GPS_PROVIDER, 10f, 10_000L)
        assertFalse(MonitorLocationSource.isBetterLocation(oldIncomingFix, currentFix))
    }
}
