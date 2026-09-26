package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AnchorEvaluatorTest: Verifies stationary anchor logic (R990c, R990d, R990e).
 * Sep.26.3:
 * - Issue #1334: Adapted to stateless AnchorEvaluator and LocationProcessingState.
 */
class AnchorEvaluatorTest {

    private val state = LocationProcessingState()
    private val logs = mutableListOf<String>()
    private val logCapture: (String, Double, Double, Double, Double?) -> Unit = { msg, _, _, _, _ ->
        logs.add(msg)
    }

    @Before
    fun setup() {
        logs.clear()
        state.accuracyWindowSize = 0
        AnchorEvaluator.reset(state)
    }

    private fun createPoint(lat: Double, lng: Double, accuracy: Double) = EngineGeoPoint().apply {
        update(lat = lat, lng = lng, accuracy = accuracy, maxAccuracy = accuracy)
    }

    @Test
    fun `test engagement and coordinate averaging`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        
        // 1. Engage anchor
        val res1 = AnchorEvaluator.evaluate(
            state = state,
            point = basePoint,
            isPhysicallyStationary = true,
            stationaryProb = 0.95,
            estimatedSpeed = 0.0,
            maxAccuracy = 10.0,
            isSuspicious = false,
            isAdaptationMuzzled = false,
            isAccuracySnap = false,
            snr = 30.0,
            vibeIndex = 0.1,
            onLog = logCapture
        )

        assertTrue("Should be locked", res1.isLocked)

        // 2. Add slightly shifted points
        val shiftedPoint = createPoint(32.794018, 34.9896, 10.0)
        
        repeat(5) {
            AnchorEvaluator.evaluate(
                state = state,
                point = shiftedPoint,
                isPhysicallyStationary = true,
                stationaryProb = 0.95,
                estimatedSpeed = 0.0,
                maxAccuracy = 10.0,
                isSuspicious = false,
                isAdaptationMuzzled = false,
                isAccuracySnap = false,
                snr = 30.0,
                vibeIndex = 0.1,
                onLog = logCapture
            )
        }

        val anchor = state.parkingAnchorPoint
        // Average should be between 32.7940 and 32.794018
        assertTrue("Lat should be averaged: ${anchor.lat}", anchor.lat > 32.7940 && anchor.lat < 32.794018)
    }

    @Test
    fun `test breakout by physical motion`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        AnchorEvaluator.evaluate(state, basePoint, true, 0.95, 0.0, 10.0, false, false, false, 30.0, 0.1, logCapture)
        assertTrue(state.isAnchorLockedState)

        val movingPoint = createPoint(32.7945, 34.9896, 10.0) // ~55m away
        val res = AnchorEvaluator.evaluate(
            state = state,
            point = movingPoint,
            isPhysicallyStationary = false, // Sensor motion
            stationaryProb = 0.95,
            estimatedSpeed = 5.0,
            maxAccuracy = 10.0,
            isSuspicious = false,
            isAdaptationMuzzled = false,
            isAccuracySnap = false,
            snr = 30.0,
            vibeIndex = 0.5,
            onLog = logCapture
        )

        assertFalse("Should breakout immediately on physical motion", res.isLocked)
    }

    @Test
    fun `test safety valve breakout`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        // Engagement
        AnchorEvaluator.evaluate(state, basePoint, true, 0.95, 0.0, 10.0, false, false, false, 30.0, 0.1, logCapture)
        
        // Large displacement (25m). Safety valve should accelerate breakout despite IMU damping.
        val farPoint = createPoint(32.7940 + 0.000225, 34.9896, 10.0)
        
        var brokeOut = false
        repeat(20) {
            val res = AnchorEvaluator.evaluate(state, farPoint, true, 0.95, 0.0, 10.0, false, false, false, 30.0, 0.1, logCapture)
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
        AnchorEvaluator.evaluate(state, basePoint, true, 0.95, 0.0, 10.0, false, false, false, 30.0, 0.1, logCapture)
        
        val snapPoint = createPoint(32.7940 + 0.00008, 34.9896, 10.0) // ~9m shift
        
        // Simulate Accuracy Snap
        repeat(5) {
            AnchorEvaluator.evaluate(state, snapPoint, true, 0.95, 0.0, 10.0, false, false, true, 30.0, 0.1, logCapture)
        }
        
        assertTrue("Accuracy Snap should delay breakout", state.isAnchorLockedState)
    }

    @Test
    fun `test snr based damping for urban canyon`() {
        val basePoint = createPoint(32.7940, 34.9896, 10.0)
        
        // Thresholds: accuracy 10m -> threshold 8m. Zone starts at 4m.
        // Pick a point ~6m away (0.000054 lat). This is IN the scoring zone but BELOW the hard breakout threshold.
        val driftPoint = createPoint(32.794054, 34.9896, 10.0)
        
        // Scenario A: High SNR (Clear Sky) - Should breakout eventually
        val highSnrState = LocationProcessingState()
        AnchorEvaluator.reset(highSnrState)
        AnchorEvaluator.evaluate(highSnrState, basePoint, true, 0.95, 0.0, 10.0, false, false, false, 30.0, 0.1) { _, _, _, _, _ -> }
        
        var highSnrBreakoutIteration = -1
        for (i in 1..100) {
            val res = AnchorEvaluator.evaluate(highSnrState, driftPoint, true, 0.95, 0.0, 10.0, false, false, false, 30.0, 0.1) { _, _, _, _, _ -> }
            if (!res.isLocked) {
                highSnrBreakoutIteration = i
                break
            }
        }
        
        // Scenario B: Low SNR (Urban Canyon) - Damping should delay breakout significantly
        val lowSnrState = LocationProcessingState()
        AnchorEvaluator.reset(lowSnrState)
        AnchorEvaluator.evaluate(lowSnrState, basePoint, true, 0.95, 0.0, 10.0, false, false, false, 30.0, 0.1) { _, _, _, _, _ -> }
        
        var lowSnrBreakoutIteration = -1
        for (i in 1..100) {
            val res = AnchorEvaluator.evaluate(lowSnrState, driftPoint, true, 0.95, 0.0, 10.0, false, false, false, 15.0, 0.1) { _, _, _, _, _ -> } // Low SNR
            if (!res.isLocked) {
                lowSnrBreakoutIteration = i
                break
            }
        }
        
        assertTrue("High SNR should breakout eventually", highSnrBreakoutIteration > 0)
        assertTrue("Low SNR ($lowSnrBreakoutIteration) should take longer than High SNR ($highSnrBreakoutIteration) due to damping",
            lowSnrBreakoutIteration == -1 || lowSnrBreakoutIteration > highSnrBreakoutIteration)
    }
}
