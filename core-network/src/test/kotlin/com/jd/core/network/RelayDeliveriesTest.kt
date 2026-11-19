package com.jd.core.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Chapter 227 (ISSUE-NOV10.8-O86): the Sensor's setup report goes to the relay on every change while the Sensor is
 * connected, also with no Monitor online, so the relay can push it to a Monitor frozen in the background.
 */
class RelayDeliveriesTest {

    /** Lets the single test thread run the collector, which combine reaches through internal channels. */
    private suspend fun settle() = delay(50.milliseconds)

    @Test
    fun `a change is sent while connected, also with no Monitor online`() = runBlocking {
        val report = MutableStateFlow("complete")
        val connected = MutableStateFlow(false)
        val monitorOnline = MutableStateFlow(false)
        val sent = mutableListOf<String>()
        val job = launch { relayDeliveries(report, connected, monitorOnline).toList(sent) }
        settle()
        report.value = "battery missing"; settle()
        assertEquals("Nothing can be sent before the relay is reached", emptyList<String>(), sent)

        connected.value = true; settle()
        assertEquals("The current report when the relay is reached", listOf("battery missing"), sent)

        report.value = "complete"; settle()
        assertEquals("A change with no Monitor online", listOf("battery missing", "complete"), sent)
        job.cancel()
    }

    @Test
    fun `a Monitor coming online gets the current report again, and a lost relay link sends nothing`() = runBlocking {
        val report = MutableStateFlow("complete")
        val connected = MutableStateFlow(true)
        val monitorOnline = MutableStateFlow(false)
        val sent = mutableListOf<String>()
        val job = launch { relayDeliveries(report, connected, monitorOnline).toList(sent) }
        settle()
        monitorOnline.value = true; settle()
        assertEquals(listOf("complete", "complete"), sent)

        connected.value = false; monitorOnline.value = false; settle()
        report.value = "battery missing"; settle()
        assertEquals("No relay link, nothing sent", 2, sent.size)

        connected.value = true; settle()
        assertEquals("Sent once the link is back", "battery missing", sent.last())
        job.cancel()
    }
}
