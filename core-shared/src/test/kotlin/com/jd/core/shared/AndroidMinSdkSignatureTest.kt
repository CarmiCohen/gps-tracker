package com.jd.core.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SIMP-NOV6.0-S1: `core-shared` is checked by animal-sniffer against one Android API level
 * (`core-shared/build.gradle.kts`). That check only protects the apps if the level is the lowest minSdk
 * of the modules that use `core-shared`; a lowered minSdk with an unchanged signature would let calls
 * missing from the older Android through again (ISSUE-NOV6.0-O25).
 */
class AndroidMinSdkSignatureTest {

    private val consumers = listOf("app-sensor", "app-monitor", "core-network", "core-ui")

    private fun buildScript(module: String) = File("../$module/build.gradle.kts").readText(Charsets.UTF_8)

    @Test
    fun signatureLevelEqualsTheMinSdkOfEveryConsumer() {
        val sharedScript = buildScript("core-shared")
        val signatureLevel = assertNotNull(
            Regex("""signature\("net\.sf\.androidscents\.signature:android-api-level-(\d+):[^"]+@signature"\)""")
                .find(sharedScript)?.groupValues?.get(1)?.toInt(),
            "core-shared/build.gradle.kts must declare an android-api-level signature"
        )

        consumers.forEach { module ->
            val script = buildScript(module)
            assertTrue("project(\":core-shared\")" in script, "$module is expected to use core-shared")
            val minSdk = assertNotNull(
                Regex("""minSdk\s*=\s*(\d+)""").find(script)?.groupValues?.get(1)?.toInt(),
                "$module/build.gradle.kts must set minSdk"
            )
            assertEquals(minSdk, signatureLevel, "The core-shared API signature must match $module's minSdk")
        }
    }

    @Test
    fun everyModuleUsingCoreSharedIsCovered() {
        val users = File("..").listFiles().orEmpty()
            .filter { File(it, "build.gradle.kts").isFile && it.name != "core-shared" }
            .filter { "project(\":core-shared\")" in File(it, "build.gradle.kts").readText(Charsets.UTF_8) }
            .map { it.name }
            .sorted()
        assertEquals(consumers.sorted(), users, "A new module using core-shared must be added to this test")
    }

    @Test
    fun theApiCheckRunsWithTheJarAndTheUnitTests() {
        val sharedScript = buildScript("core-shared")
        assertTrue("id(\"ru.vyarus.animalsniffer\")" in sharedScript, "core-shared must apply the animal-sniffer plugin")
        assertTrue("tasks.jar { dependsOn(apiCheck) }" in sharedScript, "Every app build must run the API check")
        assertTrue("dependsOn(apiCheck)\n" in sharedScript.replace("\r\n", "\n"), "The unit tests must run the API check")
    }
}
