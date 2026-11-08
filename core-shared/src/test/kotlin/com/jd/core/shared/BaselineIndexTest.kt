package com.jd.core.shared

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Chapter 157 (ISSUE-NOV8.5-O47, SIMP-NOV8.5-S1, Nov8.14): the git tag is the baseline of a version, and
 * `STATUS/BASELINE/INDEX.md` lists each version with its commit. This test keeps the index complete and true:
 * a version that is recorded but not indexed, or a tag that points elsewhere than the index says (as the tags
 * Nov8.11 and Nov8.12 did before Nov8.13, ISSUE-NOV8.13-O53), fails. The version being closed has no commit yet, so its
 * row says `pending`; that is accepted only on the first row and only until its tag exists.
 */
class BaselineIndexTest {

    private data class Row(val version: String, val commit: String, val notes: String) {
        val isUntagged get() = "No tag" in notes
        val isPending get() = commit == PENDING
    }

    private val rows by lazy { parseRows(document("STATUS/BASELINE/INDEX.md")) }

    @Test
    fun everyRecordedVersionHasAnIndexLine() {
        val name = """Nov\d+\.\d+(?:-[A-Za-z]\w*)?"""
        val recorded = Regex("""Resolved Issues \(($name)\)""").findAll(document("STATUS/RESOLUTION_ARCHIVE.md")).map { it.groupValues[1] } +
            Regex("""^- \*\*($name):""", RegexOption.MULTILINE).findAll(document("DOCS/issues.md")).map { it.groupValues[1] }
        val indexed = rows.map { it.version }.toSet()
        assertEquals(emptyList(), recorded.distinct().filterNot { it in indexed }.toList(), "versions recorded but missing from STATUS/BASELINE/INDEX.md")
    }

    @Test
    fun versionsAreUniqueAndTheNewestComesFirst() {
        assertTrue(rows.size >= 90, "the index lost rows: ${rows.size}")
        assertEquals(rows.map { it.version }.distinct(), rows.map { it.version }, "a version is listed twice")
        val current = document("gradle.properties").lines().first { it.startsWith("appVersionName=") }.substringAfter("=")
        assertTrue(rows.first().version == current || rows.none { it.version == current }, "the current version $current, once indexed, must be the first row")
    }

    @Test
    fun everyTagPointsAtTheIndexedCommit() {
        // A source copy without git history cannot be checked; the index is then read as written.
        if (!File("../.git").exists() || git("--version") == null) return
        val wrong = mismatches(
            rows,
            tagOf = { version -> git("rev-parse", "--short=7", "$version^{commit}") },
            isCommit = { hash -> git("cat-file", "-t", hash) == "commit" }
        )
        assertEquals(emptyList(), wrong, "STATUS/BASELINE/INDEX.md disagrees with the git tags")
    }

    @Test
    fun onlyTheNewestRowMayBePendingAndOnlyUntilItIsTagged() {
        val index = "| Nov8.14 | pending | |\n| Nov8.13 | 981668c | |\n| Nov8.12 | pending | |"
        val tags = mapOf("Nov8.13" to "981668c", "Nov8.12" to "6021425")
        assertEquals(listOf("Nov8.12: only the newest row may be pending"), mismatches(parseRows(index), tagOf = tags::get, isCommit = { true }))
        assertEquals(
            listOf("Nov8.14: tagged on aaaaaaa, replace 'pending' with it"),
            mismatches(parseRows(index).take(1), tagOf = { "aaaaaaa" }, isCommit = { true })
        )
    }

    @Test
    fun aMovedOrMissingTagIsDetected() {
        val index = """
            | Nov8.12 | 6021425 | |
            | Nov8.11 | dd4be20 | |
            | Nov8.7 | d237755 | No tag: committed inside Nov8.8 |
        """.trimIndent()
        // The tags as they were before Nov8.13 repaired them (ISSUE-NOV8.13-O53), plus a tag added to an untagged version.
        val before = mapOf("Nov8.12" to "dd4be20", "Nov8.11" to "e45d8fd", "Nov8.7" to "d237755")
        assertEquals(
            listOf("Nov8.12: index 6021425, tag dd4be20", "Nov8.11: index dd4be20, tag e45d8fd", "Nov8.7 is marked 'No tag' but has a tag on d237755"),
            mismatches(parseRows(index), tagOf = before::get, isCommit = { true })
        )
        assertEquals(listOf("Nov8.12: index 6021425, tag missing"), mismatches(parseRows(index).take(1), tagOf = { null }, isCommit = { true }))
        val repaired = mapOf("Nov8.12" to "6021425", "Nov8.11" to "dd4be20")
        assertEquals(emptyList(), mismatches(parseRows(index), tagOf = repaired::get, isCommit = { true }))
    }

    private fun parseRows(text: String) = text.lines().mapNotNull { line ->
        Regex("""^\| ([A-Za-z]+[\d.]+(?:-\w+)?) \| ([0-9a-f]{7}|$PENDING) \|(.*)\|$""").find(line)?.destructured
            ?.let { (version, commit, notes) -> Row(version, commit, notes.trim()) }
    }

    private fun mismatches(rows: List<Row>, tagOf: (String) -> String?, isCommit: (String) -> Boolean) = rows.mapIndexedNotNull { i, row ->
        val tagged = tagOf(row.version)
        when {
            row.isPending && i > 0 -> "${row.version}: only the newest row may be pending"
            row.isPending && tagged != null -> "${row.version}: tagged on $tagged, replace '$PENDING' with it"
            row.isPending -> null
            row.isUntagged && tagged != null -> "${row.version} is marked 'No tag' but has a tag on $tagged"
            row.isUntagged && !isCommit(row.commit) -> "${row.version}: ${row.commit} is not a commit"
            !row.isUntagged && tagged != row.commit -> "${row.version}: index ${row.commit}, tag ${tagged ?: "missing"}"
            else -> null
        }
    }

    private fun document(path: String) = File("../$path").readText(Charsets.UTF_8)

    private companion object {
        const val PENDING = "pending"
    }

    /** Runs git in the project root; null when git fails or is missing. */
    private fun git(vararg args: String): String? = runCatching {
        val process = ProcessBuilder(listOf("git") + args).directory(File("..")).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) out else null
    }.getOrNull()
}
