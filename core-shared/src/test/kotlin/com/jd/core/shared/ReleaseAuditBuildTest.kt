package com.jd.core.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Chapter 165 (ISSUE-NOV8.13-O55 / Version Nov9.3-MonitorStart): the device audit measures startup frames on the
 * release build.
 *
 * A debug build is debuggable, so ART runs it in its interpreter: the Monitor's first frame took 726 ms there and
 * about 0.45 s as a release build on the A15. The audit is only meaningful while the release build stays
 * non-debuggable and installs over the debug app (same application ID, signed with the debug key by
 * `TESTS/install_release.ps1`), and while Completion step 1 tells the next session to use it.
 */
class ReleaseAuditBuildTest {

    private fun read(path: String) = File("../$path").readText(Charsets.UTF_8)

    private val appBuildScripts = listOf("app-monitor/build.gradle.kts", "app-sensor/build.gradle.kts")

    @Test
    fun releaseBuildsAreNotDebuggableAndInstallOverTheDebugApp() {
        for (path in appBuildScripts) {
            val script = read(path)
            assertFalse(Regex("""isDebuggable\s*=\s*true""").containsMatchIn(script), "$path must not make a build debuggable: the audit would measure the interpreter")
            assertFalse("applicationIdSuffix" in script, "$path must not add an application ID suffix: the release build must install over the debug app")
        }
    }

    @Test
    fun installScriptSignsTheReleaseApkWithTheDebugKeyAndKeepsAppData() {
        val script = read("TESTS/install_release.ps1")
        assertTrue("-release-unsigned.apk" in script, "install_release.ps1 must take the assembleRelease output")
        assertTrue("debug.keystore" in script && "androiddebugkey" in script, "install_release.ps1 must sign with the debug key, the key of the installed debug app")
        assertTrue("install -r" in script, "install_release.ps1 must replace the app in place, keeping its data and pairing")
    }

    /**
     * Chapter 174 (ISSUE-NOV9.8-O70): adb's stderr reaches PowerShell as ErrorRecords that print as
     * `RemoteException`; the script must report their messages, so a failure names its reason
     * (for example INSTALL_FAILED_VERSION_DOWNGRADE).
     */
    @Test
    fun installScriptReportsTheRealFailureReason() {
        val script = read("TESTS/install_release.ps1")
        assertTrue("ErrorRecord" in script && "Exception.Message" in script, "install_release.ps1 must convert stderr ErrorRecords to their messages")
        assertTrue("INSTALL_FAILED" in script, "install_release.ps1 must pick the line naming the install failure")
        assertFalse(Regex("""install -r \S+ 2>&1 \| Select-Object -Last 1""").containsMatchIn(script), "install_release.ps1 must not print only the raw last record")
    }

    @Test
    fun completionSequenceAuditsTheReleaseBuild() {
        val guidelines = read("DEVELOPER_GUIDELINES.md")
        val step1 = guidelines.lines().first { it.startsWith("1.  **Verification**") }
        assertTrue("install_release.ps1" in step1, "Completion step 1 must install the release builds before device_audit.cmd")
        assertTrue("installDebug" in step1, "Completion step 1 must restore the debug builds after the audit")
    }
}
