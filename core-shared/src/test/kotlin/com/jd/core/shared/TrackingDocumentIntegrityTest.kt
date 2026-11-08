package com.jd.core.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Chapter 150 (ISSUE-NOV8.2-O46) and chapter 158 (Nov8.100-Documentation): the tracking documents keep their
 * history and stay lean.
 *
 * Whole-file rewrites once cut the tail off `issues.md` and `RESOLUTION_ARCHIVE.md` unnoticed (ISSUE-NOV8.2-O46),
 * so each resolved version must still be recorded in the archive, the SOT baseline, and a dashboard row, and the
 * oldest entries must survive. Since Nov8.100-Documentation the archive is the single home of resolved issues
 * (Documentation Rule D1), `issues.md` is a working file (D2), and `Handover.md` is short (D3); this test holds
 * those limits. Since Nov8.102-Documentation (SIMP-NOV8.100-S2, chapter 160) the SOT baseline is frozen at
 * Nov8.101-Tooling: it must hold every version up to then and nothing newer. The documents are declared as inputs of
 * `:core-shared:test`, so editing one re-runs it.
 */
class TrackingDocumentIntegrityTest {

    private val issues by lazy { document("DOCS/issues.md") }
    private val archive by lazy { document("STATUS/RESOLUTION_ARCHIVE.md") }
    private val sot by lazy { document("STATUS/SOT_MASTER_REQUIREMENTS.md") }
    private val handover by lazy { document("Handover.md") }

    @Test
    fun issuesIsALeanWorkingFileWithTheDashboardLast() {
        val headings = sectionHeadings(issues)
        listOf("Next Resumption Focus", "Work Queue", "Open Gaps", "Strategic Simplification Ideas", "Hardening Progress Dashboard")
            .forEach { required -> assertTrue(headings.any { required in it }, "issues.md lost its '$required' section") }
        assertTrue("Hardening Progress Dashboard" in headings.last { it.startsWith("## ") }, "the dashboard must stay the last section of issues.md")
        assertTrue(headings.none { "Resolved Gaps" in it }, "issues.md must not hold Resolved Gaps; the archive is their home (D1, D2)")
        assertTrue(dashboardRows(issues).size <= MAX_DASHBOARD_ROWS, "issues.md keeps at most $MAX_DASHBOARD_ROWS dashboard rows; move older ones to the archive (D2)")
        assertTrue(issues.length <= MAX_ISSUES_CHARS, "issues.md has ${issues.length} characters, more than $MAX_ISSUES_CHARS (D2)")
    }

    @Test
    fun handoverFollowsTheShortTemplate() {
        val lines = handover.lines()
        assertTrue(lines.size <= MAX_HANDOVER_LINES, "Handover.md has ${lines.size} lines, more than $MAX_HANDOVER_LINES (D3)")
        assertTrue(lines.first().startsWith("```"), "Handover.md must start with the Git block")
        listOf("Resumption State", "Status: PASSED", "**Audit line**", "Next Resumption Focus")
            .forEach { part -> assertTrue(part in handover, "Handover.md lost its '$part' part") }
    }

    @Test
    fun everyResolvedVersionIsRecordedInEveryTrackingDocument() {
        val archived = archivedVersions(archive)
        val sotBaseline = sotBaselineVersions(sot)
        assertEquals(emptyList(), archived.filter { it <= SOT_FROZEN_AT }.filterNot { it in sotBaseline }, "versions missing from the SOT Core Hardening Baseline")
        assertEquals(emptyList(), sotBaseline.filter { it > SOT_FROZEN_AT }, "the SOT Core Hardening Baseline is frozen at Nov8.101; record newer versions in the archive only (SIMP-NOV8.100-S2)")

        val dashboard = dashboardRows(issues) + dashboardRows(archive, ARCHIVE_DASHBOARD)
        assertEquals(emptyList(), archived.filterNot { v -> dashboard.any { v in it } }, "versions missing from the dashboard of issues.md and the archive's Dashboard History")
    }

    @Test
    fun historyStillReachesItsOldestEntries() {
        assertTrue(archivedVersions(archive).contains(Version(4, 32)), "RESOLUTION_ARCHIVE.md lost its Nov4.32 entry")
        assertTrue(dashboardRows(archive, ARCHIVE_DASHBOARD).any { Version(4, 26) in it }, "the Dashboard History lost its Nov4.26 row")
        assertTrue("Dashboard History" in sectionHeadings(archive).last { it.startsWith("## ") }, "the Dashboard History must stay the last section of the archive")
        assertTrue("**Chapter 5.52 (Nov4.30" in sot, "the SOT lost verification chapter 5.52")
        assertTrue("- **Nov4.22-addHUD**" in sot, "the SOT lost its oldest hardening baseline entry")
    }

    @Test
    fun versionsAreUniqueAndNewestFirst() {
        val archived = archivedVersions(archive)
        assertEquals(archived.sortedDescending(), archived, "RESOLUTION_ARCHIVE.md is not newest first, or repeats a version")
        assertEquals(archived.distinct(), archived, "RESOLUTION_ARCHIVE.md repeats a version")

        val rows = (dashboardRows(issues) + dashboardRows(archive, ARCHIVE_DASHBOARD)).map { it.endInclusive }
        assertEquals(rows.sortedDescending(), rows, "the dashboard rows, then the Dashboard History, are not newest first")
        assertEquals(rows.distinct(), rows, "a dashboard row appears twice")
    }

    @Test
    fun aTruncatedArchiveIsDetected() {
        val full = "# Resolution Archive\n\n## ✅ Resolved Issues (Nov8.0)\n- x\n\n## ✅ Resolved Issues (Nov4.32)\n- y\n"
        val truncated = full.substringBefore("## ✅ Resolved Issues (Nov4.32)")
        assertTrue(archivedVersions(full).contains(Version(4, 32)))
        assertTrue(!archivedVersions(truncated).contains(Version(4, 32)))
    }

    @Test
    fun theFrozenSotBaselineRejectsANewerEntryAndIgnoresRuleText() {
        val sotText = "*   **1.398 x**: the Core Hardening Baseline is frozen.\n- **Nov9.0**: in a rule, not the list\n" +
            "## 🛡️ Core Hardening Baseline\n- **Nov8.102-Documentation**: x\n- **Nov8.101-Tooling**: x\n"
        val baseline = sotBaselineVersions(sotText)
        assertEquals(listOf(Version(8, 102), Version(8, 101)), baseline)
        assertEquals(listOf(Version(8, 102)), baseline.filter { it > SOT_FROZEN_AT })
    }

    @Test
    fun versionNamesWithASuffixAreRead() {
        assertEquals(listOf(Version(8, 100)), archivedVersions("## ✅ Resolved Issues (Nov8.100-Documentation)\n"))
        assertEquals(listOf(Version(8, 100)), sotBaselineVersions("## 🛡️ Core Hardening Baseline\n- **Nov8.100-Documentation**: x\n"))
        val rows = dashboardRows("## 📊 Hardening Progress Dashboard\n- **Nov8.100-Documentation: [SOT Count: 1]**\n- **Nov8.14: [SOT Count: 1]**\n")
        assertEquals(listOf(Version(8, 100), Version(8, 14)), rows.map { it.start })
        assertTrue(Version(8, 100) > Version(8, 14), "Nov8.100 must sort after Nov8.14")
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

    private companion object {
        const val MAX_DASHBOARD_ROWS = 6
        const val MAX_ISSUES_CHARS = 40_000
        const val MAX_HANDOVER_LINES = 70
        val SOT_FROZEN_AT = Version(8, 101)
        const val NAME = """Nov(\d+)\.(\d+)(?:-[A-Za-z]\w*)?"""
        const val ISSUES_DASHBOARD = "Hardening Progress Dashboard"
        const val ARCHIVE_DASHBOARD = "Dashboard History"
    }

    private fun document(path: String) = File("../$path").readText(Charsets.UTF_8)

    private fun sectionHeadings(text: String) = text.lines().filter { it.startsWith("#") }


    private fun versionsOf(text: String, pattern: Regex) =
        text.lines().mapNotNull { line -> pattern.find(line)?.let { Version(it.groupValues[1].toInt(), it.groupValues[2].toInt()) } }

    private fun archivedVersions(text: String) = versionsOf(text, Regex("""^## .*Resolved Issues \($NAME\)"""))

    private fun sotBaselineVersions(text: String) =
        // Match the heading line itself: SOT rules mention these words in their text (rule 1.398).
        versionsOf(text.substringAfter("## 🛡️ Core Hardening Baseline", missingDelimiterValue = "").substringBefore("\n## "), Regex("""^- \*\*$NAME\*\*"""))

    /** Rows under the heading [title]: "Hardening Progress Dashboard" in issues.md, "Dashboard History" in the archive. */
    private fun dashboardRows(text: String, title: String = ISSUES_DASHBOARD): List<ClosedRange<Version>> {
        // Match the heading line itself: archive entries mention these words in their text.
        val rows = text.substringAfter("## 📊 $title", missingDelimiterValue = "").substringBefore("\n## ")
        return rows.lines().mapNotNull { line ->
            Regex("""^- \*\*$NAME(?: to Nov(\d+)\.(\d+))?:""").find(line)?.groupValues?.let { g ->
                val from = Version(g[1].toInt(), g[2].toInt())
                val to = if (g[3].isEmpty()) from else Version(g[3].toInt(), g[4].toInt())
                minOf(from, to)..maxOf(from, to)
            }
        }
    }
}
