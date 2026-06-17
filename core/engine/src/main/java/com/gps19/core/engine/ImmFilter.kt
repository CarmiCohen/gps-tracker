package com.gps19.core.engine

import kotlin.math.*

/**
 * ImmFilter: Interacting Multiple Model Filter.
 */
class ImmFilter {

    private class KalmanModel(
        var x: Double, var y: Double,
        var vx: Double = 0.0, var vy: Double = 0.0,
        var pPos: Double = 25.0,
        var pVel: Double = 1.0,
        val qPos: Double,
        val qVel: Double,
        var probability: Double
    )

    private val modelStationary = KalmanModel(
        x = 0.0, y = 0.0,
        qPos = IMM_STATIONARY_Q_POS, qVel = IMM_STATIONARY_Q_VEL,
        probability = IMM_STATIONARY_PROBABILITY
    )

    private val modelKinematic = KalmanModel(
        x = 0.0, y = 0.0,
        qPos = IMM_KINEMATIC_Q_POS, qVel = IMM_KINEMATIC_Q_VEL,
        probability = IMM_KINEMATIC_PROBABILITY
    )

    private var lastUpdateTs = 0L

    fun update(lat: Double, lng: Double, accuracy: Float, timestamp: Long, qScale: Double = 1.0): EngineGeoPoint {
        if (lastUpdateTs == 0L) {
            initModels(lat, lng, timestamp)
            return EngineGeoPoint(lat, lng)
        }

        val dt = (timestamp - lastUpdateTs) / 1000.0
        if (dt <= 0) return EngineGeoPoint(modelStationary.x, modelStationary.y)
        
        if (dt > IMM_STALL_RECOVERY_DT_SEC) {
            initModels(lat, lng, timestamp)
            return EngineGeoPoint(lat, lng)
        }

        // Apply Q-scaling (for suspicious state)
        val qSPos = modelStationary.qPos * qScale
        val qSVel = modelStationary.qVel * qScale
        val qKPos = modelKinematic.qPos * qScale
        val qKVel = modelKinematic.qVel * qScale

        val prevLat = (modelStationary.x * modelStationary.probability) + (modelKinematic.x * modelKinematic.probability)
        val lngDegToMeters = LAT_DEG_TO_METERS * cos(Math.toRadians(prevLat))

        predictModel(modelStationary, dt, lngDegToMeters, qSPos, qSVel)
        predictModel(modelKinematic, dt, lngDegToMeters, qKPos, qKVel)

        val rVar = max(IMM_MIN_MEASUREMENT_NOISE_METERS, accuracy.toDouble()).pow(2)
        val currentLngDegToMeters = LAT_DEG_TO_METERS * cos(Math.toRadians(lat))
        
        val weightS = updateModel(modelStationary, lat, lng, rVar, dt, currentLngDegToMeters)
        val weightK = updateModel(modelKinematic, lat, lng, rVar, dt, currentLngDegToMeters)

        val totalWeight = (weightS * modelStationary.probability) + (weightK * modelKinematic.probability)
        if (totalWeight > 1e-12) {
            modelStationary.probability = (weightS * modelStationary.probability) / totalWeight
            modelKinematic.probability = (weightK * modelKinematic.probability) / totalWeight
        }

        lastUpdateTs = timestamp

        val mixedLat = (modelStationary.x * modelStationary.probability) + (modelKinematic.x * modelKinematic.probability)
        val mixedLng = (modelStationary.y * modelStationary.probability) + (modelKinematic.y * modelKinematic.probability)

        if (!mixedLat.isFinite() || !mixedLng.isFinite()) {
            initModels(lat, lng, timestamp)
            return EngineGeoPoint(lat, lng)
        }

        return EngineGeoPoint(mixedLat, mixedLng)
    }

    fun getEstimatedSpeedKph(): Double {
        val mixedVx = (modelStationary.vx * modelStationary.probability) + (modelKinematic.vx * modelKinematic.probability)
        val mixedVy = (modelStationary.vy * modelStationary.probability) + (modelKinematic.vy * modelKinematic.probability)
        
        if (!mixedVx.isFinite() || !mixedVy.isFinite()) return 0.0
        
        val speedMps = sqrt(mixedVx * mixedVx + mixedVy * mixedVy)
        return min(speedMps * 3.6, OUTLIER_SPEED_CAP_MPS * 3.6)
    }

    fun getEstimatedBearing(): Float {
        val mixedVx = (modelStationary.vx * modelStationary.probability) + (modelKinematic.vx * modelKinematic.probability)
        val mixedVy = (modelStationary.vy * modelStationary.probability) + (modelKinematic.vy * modelKinematic.probability)
        if (abs(mixedVx) < 0.1 && abs(mixedVy) < 0.1) return 0f
        var deg = Math.toDegrees(atan2(mixedVy, mixedVx)).toFloat()
        return (deg + 360f) % 360f
    }

    private fun initModels(lat: Double, lng: Double, ts: Long) {
        modelStationary.x = lat; modelStationary.y = lng
        modelStationary.vx = 0.0; modelStationary.vy = 0.0
        modelStationary.pPos = 25.0; modelStationary.pVel = 1.0
        modelStationary.probability = IMM_STATIONARY_PROBABILITY

        modelKinematic.x = lat; modelKinematic.y = lng
        modelKinematic.vx = 0.0; modelKinematic.vy = 0.0
        modelKinematic.pPos = 25.0; modelKinematic.pVel = 1.0
        modelKinematic.probability = IMM_KINEMATIC_PROBABILITY
        lastUpdateTs = ts
    }

    private fun predictModel(m: KalmanModel, dt: Double, lngDegToMeters: Double, qPos: Double, qVel: Double) {
        m.x += (m.vx * dt) / LAT_DEG_TO_METERS
        m.y += (m.vy * dt) / lngDegToMeters 
        m.pPos += qPos * dt; m.pVel += qVel * dt
        if (m.pVel > 1000.0) m.pVel = 1000.0
    }

    private fun updateModel(m: KalmanModel, zLat: Double, zLng: Double, rVar: Double, dt: Double, lngDegToMeters: Double): Double {
        val innLatMeters = (zLat - m.x) * LAT_DEG_TO_METERS
        val innLngMeters = (zLng - m.y) * lngDegToMeters
        val sVar = m.pPos + rVar
        val kPos = m.pPos / sVar
        val kVel = (dt * m.pVel) / sVar
        m.x += (kPos * innLatMeters) / LAT_DEG_TO_METERS
        m.y += (kPos * innLngMeters) / lngDegToMeters
        m.vx += kVel * innLatMeters
        m.vy += kVel * innLngMeters
        val modelSpeedMps = sqrt(m.vx.pow(2) + m.vy.pow(2))
        if (modelSpeedMps > OUTLIER_SPEED_CAP_MPS || !m.vx.isFinite() || !m.vy.isFinite()) {
            m.vx = 0.0; m.vy = 0.0; m.pVel = 1.0
        }
        m.pPos *= (1.0 - kPos); m.pVel *= 0.99
        val distSq = innLatMeters.pow(2) + innLngMeters.pow(2)
        val weight = exp(-0.5 * distSq / sVar) / (2.0 * PI * sVar)
        return if (weight.isFinite()) weight else 1e-12
    }

    fun getStationaryProbability(): Double = modelStationary.probability
    
    fun reset() {
        lastUpdateTs = 0L
        modelStationary.probability = IMM_STATIONARY_PROBABILITY; modelStationary.vx = 0.0; modelStationary.vy = 0.0
        modelKinematic.probability = IMM_KINEMATIC_PROBABILITY; modelKinematic.vx = 0.0; modelKinematic.vy = 0.0
    }
}
