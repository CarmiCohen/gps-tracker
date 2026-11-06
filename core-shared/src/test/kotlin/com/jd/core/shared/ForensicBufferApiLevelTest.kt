package com.jd.core.shared

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Verifies Chapter 121.6: ForensicBuffer stays runnable on the Sensor's minSdk (ISSUE-NOV6.0-O25).
 *
 * This module compiles against the JDK and Gradle builds do not run lint on it, so a call Android
 * lacks would build cleanly, and no device below API 35 is available. These signatures are
 * checked in the compiled class instead:
 * force(int, int), position(int), and the other JDK 9+ forms that would link to a missing method.
 */
class ForensicBufferApiLevelTest {

    private val forbiddenSignatures = listOf(
        "(II)Ljava/nio/MappedByteBuffer;",
        "(I)Ljava/nio/MappedByteBuffer;",
        "(I)Ljava/nio/ByteBuffer;",
        "(I)Ljava/nio/Buffer;",
        "(I[B)Ljava/nio/ByteBuffer;",
        "(I[BII)Ljava/nio/ByteBuffer;"
    )

    @Test
    fun compiledClassUsesNoSignatureMissingFromOlderAndroid() {
        val stream = assertNotNull(ForensicBuffer::class.java.getResourceAsStream("ForensicBuffer.class"))
        val classText = stream.use { String(it.readBytes(), Charsets.ISO_8859_1) }

        forbiddenSignatures.forEach { signature ->
            assertFalse(signature in classText, "ForensicBuffer must not call a method with $signature")
        }
    }
}
