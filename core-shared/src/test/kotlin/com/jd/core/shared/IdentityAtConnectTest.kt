package com.jd.core.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Chapter 167 (ISSUE-NOV8.101-O59, ISSUE-NOV8.13-O56 / Version Nov9.5-DefaultId): each app connects with its
 * default ID (`T` or `V`) unless another was assigned.
 *
 * A Sensor started at boot never opens its first screen, so it must create its ID itself before it connects;
 * otherwise it joins the relay as `PENDING_IDENTITY`. The ID is created only when none is stored, so an assigned
 * ID is kept.
 */
class IdentityAtConnectTest {

    private fun read(path: String) = File("../$path").readText(Charsets.UTF_8)

    private fun assertCreatedBeforeRead(path: String, prefix: String) {
        val source = read(path)
        val ensure = source.indexOf("ensureIdentityGenerated(ForensicIdentity.$prefix)")
        val firstRead = source.indexOf("identityManager.selfId")
        assertTrue(ensure >= 0, "$path must create its ID before connecting")
        assertTrue(firstRead > ensure, "$path must create its ID before it reads selfId")
    }

    @Test
    fun servicesCreateTheirIdBeforeConnecting() {
        assertCreatedBeforeRead("app-sensor/src/main/kotlin/com/jd/app/sensor/SensorService.kt", "PREFIX_SENSOR")
        assertCreatedBeforeRead("app-monitor/src/main/kotlin/com/jd/app/monitor/MonitorSessionService.kt", "PREFIX_MONITOR")
    }

    @Test
    fun anAssignedIdIsKept() {
        val manager = read("core-network/src/main/kotlin/com/jd/core/network/ForensicIdentityManager.kt")
        assertTrue(Regex("""if \(prefs\[selfIdKey] == null\)\s*\{\s*generate\(""").containsMatchIn(manager), "the default ID must be written only when no ID is stored")
        assertEquals("T", ForensicIdentity.getDefaultId(ForensicIdentity.PREFIX_SENSOR))
    }
}
