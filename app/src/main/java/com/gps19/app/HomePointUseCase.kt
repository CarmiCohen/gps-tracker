package com.gps19.app

import com.gps19.core.engine.PhysicsUtils
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * HomePointUseCase: Business logic for managing geofence center points.
 * Sep.22.03:
 * - Issue #1179: Atomic Hydration. Refactored add/remove to use atomic 
 *   repository methods, preventing list corruption during rapid interactive 
 *   updates. (R-ID 400).
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor.
 */
class HomePointUseCase @Inject constructor(
    private val repository: MainRepository
) {
    suspend fun addHomePoint(newPoint: GeoPoint): List<GeoPoint> {
        repository.addHomePoint(newPoint.latitude, newPoint.longitude)
        return repository.loadHomePoints()
    }

    suspend fun removeHomePoint(index: Int): List<GeoPoint> {
        repository.removeHomePoint(index)
        return repository.loadHomePoints()
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
