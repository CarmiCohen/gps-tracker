package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies Chapter 119.1: both apps share one default relay and honor a saved URL. */
class RelayConfigTest {

    @Test
    fun defaultIsTheLiveRelay() {
        assertEquals("https://gps-survival-relay.onrender.com", RelayConfig.DEFAULT_URL)
    }

    @Test
    fun missingOrBlankSavedUrlUsesTheDefault() {
        assertEquals(RelayConfig.DEFAULT_URL, RelayConfig.resolve(null))
        assertEquals(RelayConfig.DEFAULT_URL, RelayConfig.resolve(""))
        assertEquals(RelayConfig.DEFAULT_URL, RelayConfig.resolve("   "))
    }

    @Test
    fun savedUrlIsKept() {
        assertEquals("http://127.0.0.1:3000", RelayConfig.resolve("http://127.0.0.1:3000"))
    }
}
