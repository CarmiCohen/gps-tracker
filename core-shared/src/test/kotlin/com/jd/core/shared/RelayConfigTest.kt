package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun malformedUrlsAreRejected() {
        listOf("not a url", "htp:/bad", "ftp://relay.example.com", "http://", "gps-survival-relay.onrender.com")
            .forEach { assertFalse(it, RelayConfig.isValid(it)) }
    }

    @Test
    fun supportedSchemesAreAccepted() {
        listOf("http://10.0.0.2:3000", "https://relay.example.com", "ws://relay.example.com", "wss://relay.example.com")
            .forEach { assertTrue(it, RelayConfig.isValid(it)) }
    }

    @Test
    fun malformedSavedUrlFallsBackToTheDefault() {
        assertEquals(RelayConfig.DEFAULT_URL, RelayConfig.resolve("not a url"))
    }

    @Test
    fun savedUrlIsKept() {
        assertEquals("http://127.0.0.1:3000", RelayConfig.resolve("http://127.0.0.1:3000"))
    }
}
