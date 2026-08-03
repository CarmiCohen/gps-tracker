package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AnchorEvaluatorTest: Verifies stationary anchor logic (R990c, R990d, R990e).
 * Aug.03.37:
 * - Issue #669: Refactored to eliminate .copy() usage on EngineGeoPoint 
 *   to resolve build errors following zero-churn transition.
 */
class AnchorEvaluatorTest {

    private lateinit var evaluator: AnchorEvaluator
    private val logs = mutableListOf<String>()

    @Before
    fun setup() {
        logs.clear()
        evaluator = AnchorEvaluator { msg, _, _, _, _ ->
            logs.add(msg)
        }
    }

    private fun createPoint(lat: Double, lng: Double, accuracy: Double) = EngineGeoPoint().apply {
        update(lat = lat, lng = lng, accuracy = accuracy, maxAccuracy = accuracy)
    }

    @Test
    fun `test engagement and coordinate averaging`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        
        // 1. Engage anchor (accuracy 10m -> threshold 8m)
        val res1 = evaluator.evaluate(
            point = basePoint,
            isPhysicallyStationary = true,
            stationaryProb = 0.95,
            estimatedSpeed = 0.0,
            maxAccuracy = 10.0,
            isSuspicious = false,
            isAdaptationMuzzled = false,
            isAccuracySnap = false,
            vibeIndex = 0.1
        )

        assertTrue("Should be locked", res1.isLocked)

        // 2. Add slightly shifted points (R990c)
        // Shift ~4.4m (0.00004 lat) - well within 8m threshold
        val shiftedPoint = createPoint(32.79404, 34.9896, 10.0)
        
        repeat(5) {
            evaluator.evaluate(
                point = shiftedPoint,
                isPhysicallyStationary = true,
                stationaryProb = 0.95,
                estimatedSpeed = 0.0,
                maxAccuracy = 10.0,
                isSuspicious = false,
                isAdaptationMuzzled = false,
                isAccuracySnap = false,
                vibeIndex = 0.1
            )
        }

        val anchor = evaluator.getAnchorPoint()
        assertNotNull(anchor)
        // Average should be between 32.7940 and 32.79404
        assertTrue("Lat should be averaged: ${anchor!!.lat}", anchor.lat > 32.7940 && anchor.lat < 32.79404)
    }

    @Test
    fun `test breakout by physical motion`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        evaluator.evaluate(basePoint, true, 0.95, 0.0, 10.0, false, false, false, 0.1)
        assertTrue(evaluator.isLocked())

        val movingPoint = createPoint(32.7945, 34.9896, 10.0) // ~55m away
        val res = evaluator.evaluate(
            point = movingPoint,
            isPhysicallyStationary = false, // Sensor motion
            stationaryProb = 0.95,
            estimatedSpeed = 5.0,
            maxAccuracy = 10.0,
            isSuspicious = false,
            isAdaptationMuzzled = false,
            isAccuracySnap = false,
            vibeIndex = 0.5
        )

        assertFalse("Should breakout immediately on physical motion", res.isLocked)
    }

    @Test
    fun `test safety valve breakout`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        // Engagement
        evaluator.evaluate(basePoint, true, 0.95, 0.0, 10.0, false, false, false, 0.1)
        
        // Large displacement (25m). Safety valve should accelerate breakout despite IMU damping.
        val farPoint = createPoint(32.7940 + 0.000225, 34.9896, 10.0)
        
        var brokeOut = false
        repeat(20) {
            val res = evaluator.evaluate(farPoint, true, 0.95, 0.0, 10.0, false, false, false, 0.1)
            if (!res.isLocked) {
                brokeOut = true
            }
        }

        assertTrue("Safety Valve should force breakout from old anchor", brokeOut)
        assertTrue("Log should mention breakout reason", logs.any { it.contains("breakout") })
    }

    @Test
    fun `test accuracy snap suppression`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        evaluator.evaluate(basePoint, true, 0.95, 0.0, 10.0, false, false, false, 0.1)
        
        val snapPoint = createPoint(32.7940 + 0.00008, 34.9896, 10.0) // ~9m shift
        
        // Simulate Accuracy Snap
        repeat(5) {
            evaluator.evaluate(snapPoint, true, 0.95, 0.0, 10.0, false, false, true, 0.1)
        }
        
        assertTrue("Accuracy Snap should delay breakout", evaluator.isLocked())
    }
}
