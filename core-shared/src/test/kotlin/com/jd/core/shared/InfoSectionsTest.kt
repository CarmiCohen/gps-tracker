package com.jd.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 191 (ISSUE-NOV10.8-O84 part 2): the detailed Info's model and lines. The link statistics, the satellite
 * summary, and the wording and warning colors of each section, so the screens only draw them.
 */
class InfoSectionsTest {

    private val now = 1_794_000_000_000L

    private val info = SensorInfo(
        capturedAtMs = now,
        sensorId = "T",
        version = "Nov15.5",
        serviceStartedAtMs = now - 3_725_000L,
        motion = "Armed (accelerometer)",
        isMotionArmed = true,
        microphone = "Listening",
        behaviourRevision = 42L,
        link = LinkStats(isConnected = true, changedAtMs = now - 65_000L, latencyMs = 180L),
        isMonitorOnline = true,
        fixTimeMs = now - 2_000L,
        lat = 32.123456,
        lng = 34.654321,
        accuracyM = 6f,
        speedMps = 0f,
        homeDistanceM = 40.0,
        homeRadiusM = 100.0,
        satellites = listOf(
            SatelliteReading(1, 1, 40f, true), SatelliteReading(2, 1, 30f, true), SatelliteReading(4, 6, 40f, true),
            SatelliteReading(5, 6, 30f, true), SatelliteReading(3, 6, 20f, false)
        ),
        vibrationG = 0.01f,
        vibrationPeakG = 0.05f,
        vibrationLimitG = 0.13f,
        tiltDeg = 2f,
        tiltLimitDeg = 15f,
        noiseDbfs = -61f,
        noiseFloorDbfs = -65f,
        phone = PhoneState(batteryPercent = 80, temperatureC = 31f, network = "Mobile", standbyBucket = 10, isBatteryExempt = true, freeStorageMb = 20_000L)
    )

    private fun List<InfoSection>.line(section: String, label: String): InfoLine =
        single { it.title.endsWith(section) }.lines.single { it.label == label }

    @Test
    fun `a link drop counts at the disconnect and its length once the link is back`() {
        val first = LinkStats().afterLink(true, 1_000L)
        assertEquals("The first connect is no drop", LinkStats(isConnected = true, changedAtMs = 1_000L), first)
        assertEquals("A repeated state changes nothing", first, first.afterLink(true, 2_000L))

        val down = first.copy(latencyMs = 90L).afterLink(false, 10_000L)
        assertEquals(1, down.drops)
        assertNull("No latency while disconnected", down.latencyMs)
        val up = down.afterLink(true, 16_000L).afterLink(false, 20_000L).afterLink(true, 22_000L)
        assertEquals(2, up.drops)
        assertEquals(6_000L, up.longestDropMs)
        assertEquals(8_000L, up.totalDropMs)
        assertEquals(22_000L, up.changedAtMs)
    }

    @Test
    fun `the link lines say since when the dropouts are counted`() {
        // Chapter 204 (ISSUE-NOV15.5-O99 part 3): counts start with the app process, so Info shows that start.
        val since = now - 60_000L // A minute, so no time zone puts it on another day.
        val lines = linkLines(LinkStats(isConnected = true, changedAtMs = now - 5_000L, countingSinceMs = since), now)
        assertEquals(clockText(since, now), lines.single { it.label == "Counting since" }.value)
        assertTrue("From Sensors before Nov17.3: no line", linkLines(LinkStats(), now).none { it.label == "Counting since" })
        assertFalse("Today: time only", clockText(since, now).contains("-"))
        assertTrue("Another day: with the date", clockText(now - 3 * 86_400_000L, now).matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }

    @Test
    fun `the signal is the mean of the satellites used in the fix`() {
        assertEquals(GnssSummary(used = 4, inView = 5, meanCn0DbHz = 35f), gnssSummary(info.satellites))
        assertEquals(GnssSummary(0, 1, null), gnssSummary(listOf(SatelliteReading(5, 1, 30f, false))))
        assertEquals("Galileo", constellationName(6))
        assertEquals("Other", constellationName(0))
    }

    @Test
    fun `a healthy Sensor has its five sections and no warning`() {
        val sections = sensorInfoSections(info)
        assertEquals(listOf("SERVICE", "CONNECTION", "POSITION", "SENSORS", "PHONE"), sections.map { it.title })
        val warnings = sections.flatMap { it.lines }.filter { it.tone == InfoTone.WARN || it.tone == InfoTone.BAD }
        assertTrue("$warnings", warnings.isEmpty())
        assertEquals("1 h 02 min", sections.line("SERVICE", "Running for").value)
        assertEquals("1 min 5 s", sections.line("CONNECTION", "Connected for").value)
        assertEquals("180 ms", sections.line("CONNECTION", "Latency").value)
        assertEquals("32.123456, 34.654321", sections.line("POSITION", "Position").value)
        assertEquals("Inside: 40 m from the nearest point (radius 100 m)", sections.line("POSITION", "Home").value)
        assertEquals("0.01 g (1 min peak 0.05 g, limit 0.13 g)", sections.line("SENSORS", "Vibration").value)
        assertEquals("2° (limit 15°)", sections.line("SENSORS", "Tilt").value)
        assertEquals("-61 dBFS (background -65)", sections.line("SENSORS", "Noise").value)
        assertEquals("No barometer", sections.line("SENSORS", "Air pressure").value)
        assertEquals("19.5 GB", sections.line("PHONE", "Free storage").value)
    }

    @Test
    fun `the satellite line opens the list`() {
        val satellites = sensorInfoSections(info).line("POSITION", "Satellites")
        assertEquals("4 used / 5 in view", satellites.value)
        assertTrue(satellites.opensSatellites)
        assertEquals("35.0 dB-Hz", sensorInfoSections(info).line("POSITION", "Signal (mean)").value)
        val few = sensorInfoSections(info.copy(satellites = info.satellites.take(2)))
        assertEquals("Too few used", InfoTone.WARN, few.line("POSITION", "Satellites").tone)
        val none = sensorInfoSections(info.copy(fixTimeMs = 0L, satellites = emptyList()))
        assertTrue("Without a fix the satellites still show why", none.line("POSITION", "Satellites").opensSatellites)
        assertEquals("--", none.line("POSITION", "Signal (mean)").value)
    }

    @Test
    fun `problems are warned about`() {
        val sections = sensorInfoSections(
            info.copy(
                serviceStartedAtMs = 0L,
                isMicrophoneFaulty = true,
                link = LinkStats(isConnected = false, changedAtMs = now - 30_000L, drops = 1),
                queuedRows = 12,
                fixTimeMs = 0L,
                homeDistanceM = null,
                tiltDeg = null,
                phone = PhoneState(batteryPercent = 8, temperatureC = 47f, network = NO_NETWORK, isPowerSave = true, standbyBucket = 45, freeStorageMb = 300L)
            )
        )
        assertEquals(InfoTone.BAD, sections.line("SERVICE", "Service").tone)
        assertEquals(InfoTone.WARN, sections.line("SERVICE", "Microphone").tone)
        assertEquals(InfoTone.BAD, sections.line("CONNECTION", "Relay").tone)
        assertEquals("30 s", sections.line("CONNECTION", "Disconnected for").value)
        assertEquals("--", sections.line("CONNECTION", "Latency").value)
        assertEquals(InfoTone.WARN, sections.line("CONNECTION", "Rows held offline").tone)
        assertEquals(InfoTone.BAD, sections.line("POSITION", "Fix").tone)
        assertEquals("Not armed", sections.line("SENSORS", "Tilt").value)
        listOf("Battery" to InfoTone.BAD, "Temperature" to InfoTone.WARN, "Network" to InfoTone.BAD, "Power saving" to InfoTone.WARN,
            "Standby bucket" to InfoTone.WARN, "Battery optimization" to InfoTone.WARN, "Free storage" to InfoTone.WARN).forEach { (label, tone) ->
            assertEquals(label, tone, sections.line("PHONE", label).tone)
        }
        val away = sensorInfoSections(info.copy(homeDistanceM = 250.0)).line("POSITION", "Home")
        assertEquals("Outside: 250 m from the nearest point (radius 100 m)", away.value)
        assertEquals(InfoTone.WARN, away.tone)
        assertEquals("No home points", sensorInfoSections(info.copy(homeDistanceM = null, homeRadiusM = null)).line("POSITION", "Home").value)
    }

    @Test
    fun `the Monitor's copy is titled as the Sensor's and grays when old`() {
        val sections = sensorInfoSections(info, isStale = true, titlePrefix = "SENSOR - ")
        assertEquals("SENSOR - SERVICE", sections.first().title)
        assertTrue(sections.all { it.isStale })
        assertFalse(sensorInfoSections(info).any { it.isStale })
    }

    @Test
    fun `the Monitor's own section`() {
        val own = monitorPhoneSection(LinkStats(isConnected = true, changedAtMs = now - 5_000L), 8f, 3_000L, 1_234.0, PhoneState(batteryPercent = 50, network = "Wi-Fi"), "V", "Nov15.5", now)
        assertEquals("THIS PHONE (MONITOR)", own.title)
        assertEquals("± 8 m, 3 s old", own.lines.single { it.label == "GPS" }.value)
        assertEquals("1.23 km", own.lines.single { it.label == "Distance to Sensor" }.value)
        assertEquals("V", own.lines.single { it.label == "Monitor ID" }.value)
        val noFix = monitorPhoneSection(LinkStats(), null, null, null, PhoneState(), "V", "Nov15.5", now)
        assertEquals(InfoTone.WARN, noFix.lines.single { it.label == "GPS" }.tone)
        assertEquals("--", noFix.lines.single { it.label == "Distance to Sensor" }.value)
    }

    @Test
    fun `the newest row stands in while no Info has come`() {
        val row = LocationUpdate(lat = 32.0, lng = 34.0, accuracy = 7f, timestamp = now - 12_000L, batteryLevel = 60).apply {
            satsUsed = 9; satsInView = 20; meanCn0DbHz = 31f; noiseDbfs = -55f
        }
        val section = newestRowSection(row, now, isStale = false)
        assertEquals("12 s ago", section.lines.single { it.label == "Sent" }.value)
        assertEquals("9 used / 20 in view", section.lines.single { it.label == "Satellites" }.value)
        assertEquals("-55 dBFS", section.lines.single { it.label == "Noise" }.value)
        val older = newestRowSection(LocationUpdate(timestamp = now - 1_000L), now, isStale = true)
        assertTrue("A row of an older Sensor has no satellite line", older.lines.none { it.label == "Satellites" })
        assertEquals(InfoTone.BAD, older.lines.single { it.label == "Fix" }.tone)
        assertTrue(older.isStale)
    }

    @Test
    fun `durations, distances and standby buckets read naturally`() {
        assertEquals("0 s", durationText(-5_000L))
        assertEquals("59 s", durationText(59_999L))
        assertEquals("12 min 5 s", durationText(725_000L))
        assertEquals("3 h 07 min", durationText(11_220_000L))
        assertEquals("2 d 4 h", durationText(187_200_000L))
        assertEquals("999 m", distanceText(999.2))
        assertEquals("1.00 km", distanceText(1_000.0))
        assertEquals("Rare", standbyBucketName(40))
        assertEquals("--", standbyBucketName(null))
    }
}
