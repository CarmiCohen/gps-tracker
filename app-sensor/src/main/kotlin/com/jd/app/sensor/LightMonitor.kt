package com.jd.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.jd.core.shared.LIGHT_RISE_RATIO
import com.jd.core.shared.LIGHT_TICK_MS
import com.jd.core.shared.LightDetector
import com.jd.core.shared.PeakTracker
import com.jd.core.shared.SystemLogger

/**
 * Reads the Sensor's ambient light sensor on its own thread and reports each sudden rise found by [LightDetector]
 * (ISSUE-NOV22.4-O130), like [AcousticMonitor] for noise. It runs only while light detection is switched on, so a
 * switched-off detection keeps the sensor closed. The sensor reports only on a change, so the held reading is fed
 * again every [LIGHT_TICK_MS]: a light that stays on still completes its confirmation, and each minute has a peak.
 */
class LightMonitor(
    private val sensorManager: SensorManager,
    /** Called with the light and the background the rise was compared against. */
    private val onAlert: (lux: Double, referenceLux: Double) -> Unit
) {
    private val lock = Any()
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    @Volatile private var run: Run? = null

    /** False on a phone without a light sensor, which the Monitor is told (ISSUE-NOV22.4-O130). */
    val hasSensor: Boolean get() = sensor != null

    /**
     * True while the phone itself rings or notifies ([OwnVibrationWatch]); its screen lights the sensor in a dark room,
     * which is no intruder, so no alert is raised. The detector still sees the readings.
     */
    @Volatile var isOwnLightActive = false

    /** The rise over the background that alerts, from the Monitor's `Light Sensitivity Threshold` slider. */
    @Volatile var riseRatio: Double = LIGHT_RISE_RATIO

    /** The newest reading and the learned background in lux, null while not measuring. */
    @Volatile var lux: Double? = null
        private set
    @Volatile var floorLux: Double? = null
        private set

    /** Every reading and tick, so the Graphs keep the brightest light of each minute. */
    val luxPeaks = PeakTracker()

    /** Starts measuring; does nothing while already measuring or without a light sensor. */
    fun start() {
        synchronized(lock) {
            val light = sensor ?: return
            if (run != null) return
            val thread = HandlerThread("LightMonitor").also { it.start() }
            val started = Run(thread, Handler(thread.looper), LightDetector(riseRatio = riseRatio))
            run = started
            sensorManager.registerListener(started, light, SensorManager.SENSOR_DELAY_NORMAL, started.handler)
            started.handler.postDelayed(started, LIGHT_TICK_MS)
            SystemLogger.i("Light", "Measuring (alert at ${riseRatio}x the background)")
        }
    }

    fun stop() {
        synchronized(lock) {
            val started = run ?: return
            run = null
            sensorManager.unregisterListener(started)
            started.handler.removeCallbacksAndMessages(null)
            started.thread.quitSafely()
            lux = null
            floorLux = null
            SystemLogger.i("Light", "Stopped")
        }
    }

    /** One start to stop: its own thread, listener, tick and detector, so a late event of a stopped run counts nowhere. */
    private inner class Run(val thread: HandlerThread, val handler: Handler, private val detector: LightDetector) :
        SensorEventListener, Runnable {
        private var heldLux: Double? = null // The newest reading; this run's thread only.

        override fun onSensorChanged(event: SensorEvent) {
            val reading = event.values[0].toDouble()
            heldLux = reading
            offer(reading)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        /** The tick: the held reading again. */
        override fun run() {
            heldLux?.let(::offer)
            handler.postDelayed(this, LIGHT_TICK_MS)
        }

        private fun offer(reading: Double) {
            if (run !== this) return // Stopped.
            lux = reading
            luxPeaks.offer(reading)
            detector.riseRatio = riseRatio // A slider change applies at once, without relearning the background.
            val isAlert = detector.onLux(reading, SystemClock.elapsedRealtime())
            floorLux = detector.floorLux
            if (isAlert && !isOwnLightActive) detector.alertReferenceLux?.let { onAlert(reading, it) }
        }
    }
}
