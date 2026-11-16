package com.jd.app.monitor

import com.jd.core.shared.GraphField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chapter 192 (ISSUE-NOV10.8-O84 part 3): what the Monitor's own minute sample holds. */
class MonitorGraphsTest {

    @Test
    fun `the Monitor samples its link and the Sensor's rows`() {
        val sample = monitorGraphSample(1_800_000_000_000L, 100f, 259L, RowCounts(live = 60, backfilled = 120, missing = 4L))
        assertEquals(100f, sample[GraphField.LINK_UP_PERCENT])
        assertEquals(259f, sample[GraphField.LATENCY_MS])
        assertEquals(60f, sample[GraphField.ROWS_LIVE])
        assertEquals(120f, sample[GraphField.ROWS_BACKFILLED])
        assertEquals(4f, sample[GraphField.ROWS_MISSING])
        assertNull("The Sensor's fields", sample[GraphField.VIBRATION_G])
        assertNull("No latency while disconnected", monitorGraphSample(0L, 0f, null, RowCounts(0, 0, 0L))[GraphField.LATENCY_MS])
    }
}
