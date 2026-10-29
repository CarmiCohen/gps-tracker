package com.gps19.core.shared

import kotlin.math.*

/**
 * SpatialMath: Pure Kotlin implementation of geospatial and kinematic calculations.
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

    /**
     * Calculates the initial bearing between two points in degrees.
     */
    fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if (lat1 == lat2 && lng1 == lng2) return 0.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lng2 - lng1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /**
     * Exponential Moving Average (EMA) for coordinate smoothing.
     */
    fun smoothValue(last: Double, current: Double, alpha: Double): Double {
        if (last == 0.0) return current
        return last + alpha * (current - last)
    }

    /**
     * Angular smoothing for bearings to handle the 0/360 degree wrap-around.
     */
    fun smoothBearing(last: Double, current: Double, alpha: Double): Double {
        var delta = current - last
        while (delta < -180) delta += 360
        while (delta > 180) delta -= 360
        return (last + delta * alpha + 360) % 360
    }
}
