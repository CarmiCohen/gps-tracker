package com.gps19.app

import com.gps19.core.engine.MockTimeProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*
import androidx.datastore.core.DataStore
import android.content.Context

/**
 * DataStoreMutationStressTest: Verifies atomic integrity of settings mutations.
 * Targets Chapter 2.5 of TEST_PROCEDURE.md (Issue #1180).
 */
class DataStoreMutationStressTest {

    @Test
    fun `Verify Atomic Mutations - Concurrent saves do not cause data loss`() = runTest {
        val mockContext: Context = mock()
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        
        val mockDataStore: DataStore<AppSettings> = mock()
        val repository = SettingsRepository(mockContext, MockTimeProvider())
        
        // Use reflection to set the private dataStore field for testing
        val field = repository.javaClass.getDeclaredField("dataStore")
        field.isAccessible = true
        field.set(repository, mockDataStore)

        var currentSettings = AppSettings.getDefaultInstance()
        
        // Mock updateData to simulate atomic updates on a local variable
        whenever(mockDataStore.updateData(any())).thenAnswer { invocation ->
            val transform = invocation.arguments[0] as suspend (AppSettings) -> AppSettings
            runBlocking {
                val next = transform(currentSettings)
                currentSettings = next
                next
            }
        }

        // Act: Perform 100 concurrent increments to recoveryCount
        val jobs = List(100) {
            launch(Dispatchers.Default) {
                repository.saveInt(RECOVERY_COUNT_KEY, currentSettings.recoveryCount + 1)
            }
        }
        jobs.joinAll()

        // Verify: The final count should be 100
        verify(mockDataStore, times(100)).updateData(any())
        assertEquals(100, currentSettings.recoveryCount)
    }
}
