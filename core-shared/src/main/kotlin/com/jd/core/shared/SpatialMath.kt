package com.jd.core.shared

import kotlin.math.*

/**
 * SpatialMath: Pure Kotlin geospatial calculations.
 * Decoupled from Android for high-speed execution in the Sensor engine.
 */
object SpatialMath {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the distance between two points in meters using the Haversine formula.
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if (lat1.isNaN() || lng1.isNaN() || lat2.isNaN() || lng2.isNaN()) return 0.0
        if (lat1 == lat2 && lng1 == lng2) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt(1.0 - a.coerceIn(0.0, 1.0)))
    }
}
