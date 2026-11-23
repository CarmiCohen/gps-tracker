package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Chapter 247 (SIMP-NOV22.11-S1): the vibration is a plain value on the row, so a snapshot outlives the pooled
 * row it was taken from, and a reset clears it with the rest.
 */
class LocationUpdateTest {

    @Test
    fun snapshotKeepsItsValuesAfterThePooledRowIsReset() {
        val pooled = LocationUpdate(sequenceId = 7L, lightLux = 412.5f, vibrationG = 0.3f)

        val kept = pooled.snapshot()
        pooled.reset()

        assertEquals(7L, kept.sequenceId)
        assertEquals(412.5f, kept.lightLux!!, 0.001f)
        assertEquals(0.3f, kept.vibrationG, 0.001f)
    }

    @Test
    fun resetClearsTheVibrationWithTheRest() {
        val row = LocationUpdate(sequenceId = 7L, lightLux = 412.5f, vibrationG = 0.3f).apply { reset() }

        assertEquals(LocationUpdate(), row)
        assertEquals(0f, row.vibrationG, 0f)
        assertNull(row.lightLux)
    }
}
