package com.jd.core.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Chapter 150 (ISSUE-NOV8.2-O46): the forensic tracking documents may only grow.
 *
 * Whole-file rewrites cut the tail off `DOCS/issues.md` (Nov7.38) and `STATUS/RESOLUTION_ARCHIVE.md`
 * (Nov7.30, Nov8.0) and dropped SOT verification chapters (Nov7.38) without any test noticing. Every
 * loss either removed the oldest entries at the end of a file or left one record out of step with the
 * others, so this test checks that each resolved version is recorded in all four places, that the
 * oldest entries are still there, and that the dashboard is still the last section of `issues.md`.
 * The documents are declared as inputs of `:core-shared:test`, so editing one re-runs this test.
 */
class TrackingDocumentIntegrityTest {

    private val issues by lazy { document("DOCS/issues.md") }
    private val archive by lazy { document("STATUS/RESOLUTION_ARCHIVE.md") }
    private val sot by lazy { document("STATUS/SOT_MASTER_REQUIREMENTS.md") }

    @Test
    fun issuesKeepsEveryRequiredSectionWithTheDashboardLast() {
        val headings = sectionHeadings(issues)
        listOf("Next Resumption Focus", "Work Queue", "Open Gaps", "Resolved Gaps", "Strategic Simplification Ideas", "Hardening Progress Dashboard")
            .forEach { required -> assertTrue(headings.any { required in it }, "issues.md lost its '$required' section") }
        assertTrue("Hardening Progress Dashboard" in headings.last { it.startsWith("## ") }, "the dashboard must stay the last section of issues.md")
    }

    @Test
    fun everyResolvedVersionIsRecordedInEveryTrackingDocument() {
        val archived = archivedVersions(archive)
        assertEquals(archived, resolvedGapVersions(issues), "RESOLUTION_ARCHIVE.md and the Resolved Gaps of issues.md disagree")

        val sotBaseline = sotBaselineVersions(sot)
        assertEquals(emptyList(), archived.filterNot { it in sotBaseline }, "versions missing from the SOT Core Hardening Baseline")

        val dashboard = dashboardRows(issues)
        assertEquals(emptyList(), archived.filterNot { v -> dashboard.any { v in it } }, "versions missing from the Hardening Progress Dashboard")
    }

    @Test
    fun historyStillReachesItsOldestEntries() {
        assertTrue(archivedVersions(archive).contains(Version(4, 32)), "RESOLUTION_ARCHIVE.md lost its Nov4.32 entry")
        assertTrue(resolvedGapVersions(issues).contains(Version(4, 32)), "issues.md lost its Nov4.32 Resolved Gaps")
        assertTrue(dashboardRows(issues).any { Version(4, 26) in it }, "the dashboard lost its Nov4.26 row")
        assertTrue("**Chapter 5.52 (Nov4.30" in sot, "the SOT lost verification chapter 5.52")
        assertTrue("- **Nov4.22-addHUD**" in sot, "the SOT lost its oldest hardening baseline entry")
    }

    @Test
    fun versionsAreUniqueAndNewestFirst() {
        listOf("RESOLUTION_ARCHIVE.md" to archivedVersions(archive), "issues.md Resolved Gaps" to resolvedGapVersions(issues))
            .forEach { (name, versions) ->
                assertEquals(versions.sortedDescending(), versions, "$name is not newest first, or repeats a version")
                assertEquals(versions.distinct(), versions, "$name repeats a version")
            }
    }

    @Test
    fun aTruncatedArchiveIsDetected() {
        val full = "# Resolution Archive\n\n## ✅ Resolved Issues (Nov8.0)\n- x\n\n## ✅ Resolved Issues (Nov7.38)\n- y\n"
        val truncated = full.substringBefore("## ✅ Resolved Issues (Nov7.38)")
        val gaps = "## 🟢 Resolved Gaps (Nov8.0)\n\n## 🟢 Resolved Gaps (Nov7.38)\n"

        assertEquals(resolvedGapVersions(gaps), archivedVersions(full))
        assertTrue(archivedVersions(truncated) != resolvedGapVersions(gaps))
    }

    @Test
    fun aDashboardRangeRowCoversEachVersionInIt() {
        val rows = dashboardRows("## 📊 Hardening Progress Dashboard\n- **Nov8.0: [SOT Count: 1]**\n- **Nov7.7 to Nov7.12: no audit figures were recorded.**\n")

        assertTrue(rows.any { Version(8, 0) in it })
        assertTrue(rows.any { Version(7, 9) in it })
        assertTrue(rows.none { Version(7, 6) in it })
    }

    data class Version(val major: Int, val minor: Int) : Comparable<Version> {
        override fun compareTo(other: Version) = compareValuesBy(this, other, Version::major, Version::minor)
    }

    private fun document(path: String) = File("../$path").readText(Charsets.UTF_8)

    private fun sectionHeadings(text: String) = text.lines().filter { it.startsWith("#") }

    private fun section(text: String, title: String) =
        text.substringAfter(title, missingDelimiterValue = "").substringBefore("\n## ")

    private fun versionsOf(text: String, pattern: Regex) =
        text.lines().mapNotNull { line -> pattern.find(line)?.let { Version(it.groupValues[1].toInt(), it.groupValues[2].toInt()) } }

    private fun archivedVersions(text: String) = versionsOf(text, Regex("""^## .*Resolved Issues \(Nov(\d+)\.(\d+)\)"""))

    private fun resolvedGapVersions(text: String) = versionsOf(text, Regex("""^## .*Resolved Gaps \(Nov(\d+)\.(\d+)\)"""))

    private fun sotBaselineVersions(text: String) =
        versionsOf(section(text, "Core Hardening Baseline"), Regex("""^- \*\*Nov(\d+)\.(\d+)\*\*"""))

    private fun dashboardRows(text: String): List<ClosedRange<Version>> =
        section(text, "Hardening Progress Dashboard").lines().mapNotNull { line ->
            Regex("""^- \*\*Nov(\d+)\.(\d+)(?: to Nov(\d+)\.(\d+))?:""").find(line)?.groupValues?.let { g ->
                val from = Version(g[1].toInt(), g[2].toInt())
                val to = if (g[3].isEmpty()) from else Version(g[3].toInt(), g[4].toInt())
                from..to
            }
        }
}
