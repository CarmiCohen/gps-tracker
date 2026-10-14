package com.gps19.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*
import org.osmdroid.util.GeoPoint

/**
 * SpatialLogicUseCaseTest: Validating geofence management and map logic.
 * Targets Chapters 2.4 and 2.6 of TEST_PROCEDURE.md.
 */
class SpatialLogicUseCaseTest {

    private val repository: MainRepository = mock()
    private val useCase = SpatialLogicUseCase(repository)

    @Test
    fun `Verify Geofence Batch Operations - Adding point calls repository`() = runBlocking {
        val point = GeoPoint(10.0, 10.0)
        whenever(repository.loadHomePoints()).thenReturn(listOf(point))

        val result = useCase.addHomePoint(point)

        verify(repository).addHomePoint(10.0, 10.0)
        assertEquals(1, result.size)
    }

    @Test
    fun `Verify Geofence Mode Persistence - handleMapEvent preserves mode correctly (Issue 1179)`() {
        val initialState = MainUiState(spatial = SpatialUiState(geofenceMode = GeofenceMode.ADD))
        
        // Simulating a map event that might occur during batching
        val event = UiEvent.SetFenceVisible(true)
        val newState = useCase.handleMapEvent(event, initialState)

        assertEquals("geofenceMode must persist during batch operations", GeofenceMode.ADD, newState.spatial.geofenceMode)
        assertEquals(true, newState.spatial.isFenceVisible)
    }

    @Test
    fun `Verify Nearest Point Indexing - Precise thresholding`() {
        val points = listOf(
            GeoPoint(10.0, 10.0),
            GeoPoint(20.0, 20.0)
        )
        val target = GeoPoint(10.0001, 10.0001) // Very close to first point
        
        val index = useCase.findNearestPointIndex(points, target, thresholdMeters = 50.0)
        assertEquals(0, index)

        val farTarget = GeoPoint(15.0, 15.0)
        val farIndex = useCase.findNearestPointIndex(points, farTarget, thresholdMeters = 50.0)
        assertEquals(-1, farIndex) // Outside threshold
    }
}
