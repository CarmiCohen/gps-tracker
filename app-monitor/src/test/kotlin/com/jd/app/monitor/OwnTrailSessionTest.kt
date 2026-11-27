package com.jd.app.monitor

import android.content.pm.ServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ISSUE-NOV27.2-O170: the Monitor's own trail stayed almost empty (83 rows in 48 h on the S21FE) with a green GPS LED,
 * as it was recorded by the screen only and the session service had no location type; Android feeds no locations to an
 * app in the background without one.
 */
class OwnTrailSessionTest {
    private val dataSync = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    private val location = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION

    @Test
    fun aFirstStartWithPermissionTriesLocationThenDataSync() {
        assertEquals(listOf(dataSync or location, dataSync), MonitorSessionService.sessionForegroundTypes(true, isForeground = false))
    }

    @Test
    fun aFirstStartWithoutPermissionUsesDataSyncOnly() {
        assertEquals(listOf(dataSync), MonitorSessionService.sessionForegroundTypes(false, isForeground = false))
    }

    @Test
    fun aRunningSessionOnlyTriesToGainLocation() {
        assertEquals(listOf(dataSync or location), MonitorSessionService.sessionForegroundTypes(true, isForeground = true))
        assertEquals(emptyList<Int>(), MonitorSessionService.sessionForegroundTypes(false, isForeground = true))
    }

    @Test
    fun noRowsAreFiledUnderAPlaceholderId() {
        assertFalse(OwnTrailRecorder.isRealMonitorId("PENDING_IDENTITY"))
        assertFalse(OwnTrailRecorder.isRealMonitorId("LOADING..."))
        assertFalse(OwnTrailRecorder.isRealMonitorId(" "))
        assertTrue(OwnTrailRecorder.isRealMonitorId("V"))
    }
}
