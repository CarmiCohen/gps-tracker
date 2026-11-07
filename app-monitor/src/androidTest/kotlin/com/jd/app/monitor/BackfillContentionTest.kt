package com.jd.app.monitor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.ForensicIdentityManager
import com.jd.core.network.IoDispatcher
import com.jd.core.network.SignalingClient
import com.jd.core.network.SocketFactory
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BackfillContentionTest: Audits the MonitorRepository under multi-peer backfill contention.
 * Verifies that redundant requests are minimized and data integrity is maintained.
 * Updated Nov2.25: Added high-latency simulation (>2000ms) to validate deduplication under pressure (GAP-REVOL38.2-G2).
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
    fun testBackfillDeduplicationWithLatency() = runTest {
        val mockClient = signalingClient as FakeSignalingClient
        
        // Use a list to capture all backfill requests sent by the repository
        val backfillRequests = mutableListOf<BackfillRequest>()
        val requestCapture = MutableSharedFlow<BackfillRequest>(extraBufferCapacity = 10)
        
        mockClient.onRequestBackfill = { req ->
            backfillRequests.add(req)
            requestCapture.tryEmit(req)
            true
        }

        // 1. Trigger a gap: Seq 0 exists (implicit), Seq 100 arrives.
        val update100 = LocationUpdate(
            deviceId = testDeviceId,
            sequenceId = 100L,
            timestamp = System.currentTimeMillis(),
            lat = 1.0,
            lng = 1.0
        )
        mockClient.emitTelemetry(update100)
        
        // Wait for the first request (1..99)
        val firstReq = requestCapture.first()
        assertEquals(1L, firstReq.startSequenceId)
        assertEquals(99L, firstReq.endSequenceId)

        // 2. Simulate High Network Latency: Delay the response by 2500ms
        // While waiting, multiple telemetry pulses arrive, each detecting the same gap.
        repeat(5) { i ->
            delay(500) // Total 2500ms
            val fillerUpdate = LocationUpdate(
                deviceId = testDeviceId,
                sequenceId = 101L + i,
                timestamp = System.currentTimeMillis(),
                lat = 1.1,
                lng = 1.1
            )
            mockClient.emitTelemetry(fillerUpdate)
        }

        // 3. Verify Deduplication
        // Repository must NOT have sent more than 1 request for the (1..99) range.
        val redundantRequests = backfillRequests.filter { it.startSequenceId == 1L && it.endSequenceId == 99L }
        assertEquals("Redundant backfill requests detected during latency period", 1, redundantRequests.size)

        // 4. Complete the backfill
        mockClient.emitBackfillResponse(BackfillResponse(firstReq.requestId, emptyList(), true))
        delay(100)

        // 5. Verify the gap is closed and future updates don't trigger backfill
        val update110 = LocationUpdate(
            deviceId = testDeviceId,
            sequenceId = 110L,
            timestamp = System.currentTimeMillis(),
            lat = 1.2,
            lng = 1.2
        )
        mockClient.emitTelemetry(update110)
        
        // This should trigger a backfill for 106..109 (since 105 was the last from the repeat loop)
        val secondReq = requestCapture.first { it.startSequenceId == 106L }
        assertEquals(109L, secondReq.endSequenceId)
    }
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [
        com.jd.app.monitor.MonitorModule::class,
        com.jd.core.network.NetworkModule::class
    ]
)
object TestMonitorModule {
    @Provides
    @Singleton
    fun provideSignalingClient(): SignalingClient = FakeSignalingClient()

    @Provides
    @Singleton
    fun provideForensicIdentityManager(): ForensicIdentityManager {
        return ForensicIdentityManager(ApplicationProvider.getApplicationContext())
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideDatabase(): MonitorDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, MonitorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    @Singleton
    fun provideTrailDao(db: MonitorDatabase) = db.trailDao()

    @Provides
    @Singleton
    fun provideMonitorDataStore(): MonitorDataStore {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return MonitorDataStore(context)
    }

    @Provides
    @Singleton
    fun provideMonitorRepository(
        trailDao: TrailDao,
        signalingClient: SignalingClient
    ): MonitorRepository {
        return MonitorRepository(trailDao, signalingClient)
    }
}

class FakeSignalingClient : SignalingClient {
    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 64)
    override val telemetryFlow: Flow<LocationUpdate> = _telemetryFlow

    override val viewerStatusFlow: Flow<Boolean> = MutableStateFlow(false)

    override val violationFlow: Flow<com.jd.core.shared.ViolationEvent> = MutableSharedFlow()
    
    private val _backfillRequestFlow = MutableSharedFlow<BackfillRequest>(extraBufferCapacity = 8)
    override val backfillRequestFlow: Flow<BackfillRequest> = _backfillRequestFlow

    private val _backfillResponseFlow = MutableSharedFlow<BackfillResponse>(extraBufferCapacity = 5000)
    override val backfillResponseFlow: Flow<BackfillResponse> = _backfillResponseFlow

    var onRequestBackfill: ((BackfillRequest) -> Boolean)? = null

    override suspend fun connect(url: String, deviceId: String, secret: String, lastSequenceId: Long) {}
    override fun disconnect() {}
    override fun getMonthlyDataUsageBytes(): Long = 0
    override suspend fun sendTelemetry(update: LocationUpdate): Boolean = true
    override suspend fun sendTelemetryBatch(updates: List<LocationUpdate>): Boolean = true
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
