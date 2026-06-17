package com.gps19.app

import com.gps19.core.engine.PhysicsUtils
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HomePointUseCase: Business logic for managing geofence center points.
 * v8.8.27: Extracted nearest point logic from ViewModel to UseCase.
 */
@Singleton
class HomePointUseCase @Inject constructor(
    private val repository: MainRepository
) {
    suspend fun addHomePoint(currentPoints: List<GeoPoint>, newPoint: GeoPoint, maxDistance: Double): List<GeoPoint> {
        val newList = currentPoints + newPoint
        repository.saveHomePoints(newList, maxDistance)
        return newList
    }

    suspend fun removeHomePoint(currentPoints: List<GeoPoint>, index: Int, maxDistance: Double): List<GeoPoint> {
        if (index < 0 || index >= currentPoints.size) return currentPoints
        val newList = currentPoints.toMutableList().apply { removeAt(index) }
        repository.saveHomePoints(newList, maxDistance)
        return newList
    }

    suspend fun clearHomePoints(maxDistance: Double): List<GeoPoint> {
        repository.saveHomePoints(emptyList(), maxDistance)
        return emptyList()
    }

    fun findNearestPointIndex(currentPoints: List<GeoPoint>, target: GeoPoint, thresholdMeters: Double = 100.0): Int {
        if (currentPoints.isEmpty()) return -1
        var nearestIdx = -1
        var minDist = Double.MAX_VALUE
        currentPoints.forEachIndexed { index, geoPoint ->
            val d = PhysicsUtils.calculateDistance(target.latitude, target.longitude, geoPoint.latitude, geoPoint.longitude)
            if (d < minDist && d < thresholdMeters) {
                minDist = d
                nearestIdx = index
            }
        }
        return nearestIdx
    }
}
