package com.jd.app.monitor

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.SignalingClient
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BackfillContentionTest: Audits the MonitorRepository under multi-peer backfill contention.
 * Verifies that redundant requests are minimized and data integrity is maintained.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackfillContentionTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: MonitorRepository

    @Inject
    lateinit var signalingClient: SignalingClient

    private val testDeviceId = "test-sensor-123"

    @Before
    fun init() {
        hiltRule.inject()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testBackfillDeduplication() = runTest {
        val mockClient = signalingClient as FakeSignalingClient
        
        // 1. Initial state: DB is empty.
        // 2. Simulate arrival of a live update with a large gap (seq 100).
        val liveUpdate = LocationUpdate(
            deviceId = testDeviceId,
            sequenceId = 100L,
            timestamp = System.currentTimeMillis(),
            lat = 1.0,
            lng = 1.0
        )

        val backfillRequests = mutableListOf<BackfillRequest>()
        
        // Use a flow to capture requests
        val requestCapture = MutableSharedFlow<BackfillRequest>(extraBufferCapacity = 10)
        mockClient.onRequestBackfill = { req ->
            backfillRequests.add(req)
            requestCapture.tryEmit(req)
            true
        }

        // Trigger processing
        mockClient.emitTelemetry(liveUpdate)
        
        // Wait for the first backfill request
        val firstReq = requestCapture.first()
        assertEquals(1L, firstReq.startSequenceId)
        assertEquals(99L, firstReq.endSequenceId)

        // 3. Simulate arrival of another live update (seq 101) BEFORE backfill completes.
        // Repository should NOT trigger another backfill request for the same range.
        val liveUpdate2 = LocationUpdate(
            deviceId = testDeviceId,
            sequenceId = 101L,
            timestamp = System.currentTimeMillis() + 1000,
            lat = 1.1,
            lng = 1.1
        )
        mockClient.emitTelemetry(liveUpdate2)

        // Give it some time for processing
        kotlinx.coroutines.delay(1000)

        // Verify only one request was sent for the gap
        val redundantRequests = backfillRequests.filter { it.startSequenceId == 1L && it.endSequenceId == 99L }
        assertEquals("Redundant backfill requests detected", 1, redundantRequests.size)
    }

    @Module
    @TestInstallIn(
        components = [SingletonComponent::class],
        replaces = [com.jd.app.monitor.MonitorModule::class]
    )
    object TestMonitorModule {
        @Provides
        @Singleton
        fun provideSignalingClient(): SignalingClient = FakeSignalingClient()

        @Provides
        @Singleton
        fun provideDatabase(): MonitorDatabase {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            return androidx.room.Room.inMemoryDatabaseBuilder(context, MonitorDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }

        @Provides
        @Singleton
        fun provideTrailDao(db: MonitorDatabase) = db.trailDao()
    }
}

class FakeSignalingClient : SignalingClient {
    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 64)
    override val telemetryFlow: Flow<LocationUpdate> = _telemetryFlow

    override val violationFlow: Flow<com.jd.core.shared.ViolationEvent> = MutableSharedFlow()
    
    private val _backfillRequestFlow = MutableSharedFlow<BackfillRequest>(extraBufferCapacity = 8)
    override val backfillRequestFlow: Flow<BackfillRequest> = _backfillRequestFlow

    private val _backfillResponseFlow = MutableSharedFlow<BackfillResponse>(extraBufferCapacity = 8)
    override val backfillResponseFlow: Flow<BackfillResponse> = _backfillResponseFlow

    var onRequestBackfill: ((BackfillRequest) -> Boolean)? = null

    override suspend fun connect(url: String, deviceId: String, secret: String, lastSequenceId: Long) {}
    override fun disconnect() {}
    override fun getMonthlyDataUsageBytes(): Long = 0
    override suspend fun sendTelemetry(update: LocationUpdate): Boolean = true
    override suspend fun sendViolation(event: com.jd.core.shared.ViolationEvent): Boolean = true

    override suspend fun requestBackfill(request: BackfillRequest): Boolean {
        return onRequestBackfill?.invoke(request) ?: true
    }

    override suspend fun sendBackfillResponse(response: BackfillResponse): Boolean = true

    fun emitTelemetry(update: LocationUpdate) {
        _telemetryFlow.tryEmit(update)
    }

    fun emitBackfillResponse(response: BackfillResponse) {
        _backfillResponseFlow.tryEmit(response)
    }
}
