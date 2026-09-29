package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicBufferSchemaTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var spillBuffer: ForensicSpillBuffer

    @Before
    fun init() { hiltRule.inject() }

    @Test
    fun verifySchemaAlignment() {
        val entry = LogEntry(
            localId = "TEST", timestamp = 12345L, message = "Forensic Handover: TEST", type = "FORENSIC_TRACE",
            isImportant = false
        )
        
        while (spillBuffer.hasPending()) {
            spillBuffer.commitDrain(1000)
        }
        
        spillBuffer.writeTrace(entry)
        val traces = spillBuffer.peekToEntities(10)
        
        Timber.i("SCHEMA_TEST: Written message: ${entry.message}")
        if (traces.isNotEmpty()) {
            Timber.i("SCHEMA_TEST: Read message: ${traces.last().message}")
        }
        
        assertEquals(entry.message, traces.last().message)
    }
}
