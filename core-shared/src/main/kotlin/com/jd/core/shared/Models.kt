package com.jd.core.shared

import kotlinx.serialization.Serializable

/**
 * Project Revolution: Master Telemetry & Command Models
 * Hardened for Zero-Allocation Pooling, E2EE, and Forensic Integrity.
 */

@Serializable
data class LocationUpdate(
    var deviceId: String = "", // Added for Multi-Peer identification (R-ID 768)
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var accuracy: Float = 0f,
    var speed: Float = -1f,
    var bearing: Float = 0f,
    var altitude: Double = 0.0,
    var timestamp: Long = 0L,
    var monotonicNanos: Long = 0L,
    var serverArrivalTs: Long = 0L, 
    var sequenceId: Long = 0L,
    var isCharging: Boolean = false,
    /**
     * A charger is connected, also while a held battery is not charging; null when not known (rows from Sensors before
     * Nov17.15 and backfilled rows), and the Monitor then falls back to [isCharging] (ISSUE-NOV17.15-O114).
     */
    var isPluggedIn: Boolean? = null,
    var batteryLevel: Int = -1,
    var gsmSignalDbm: Int = 0,
    var networkType: String = "--",
    var isBuffered: Boolean = false,
    var isHistoricalLkg: Boolean = false,
    var tempCelsius: Float = -1f,
    var isThermalThrottled: Boolean = false,
    var cumulativeBytesSent: Long = 0L,
    var status: String = "VALID", // What the reading means (VALID, NO_FIX, JUMP, ...); delivery never depends on it
    /** The Sensor's noise detection is off: no microphone after a restart, denied, silenced, or failed (ISSUE-NOV8.103-O60). */
    var isNoiseDetectionOff: Boolean = false,
    /**
     * Sent at once, never dropped or deduplicated while queued (SIMP-NOV7.19-S1). On the wire it is `crit`, which
     * tells the relay to push the row to the Monitors as well (ISSUE-NOV10.8-O86); the receiver does not read it back.
     */
    var isCritical: Boolean = false,
    /** Satellites used in the fix and in view, -1 when unknown (ISSUE-NOV10.8-O84 part 2). */
    var satsUsed: Int = -1,
    var satsInView: Int = -1,
    /** Mean signal of the satellites used in the fix, null when none is (ISSUE-NOV10.8-O84 part 2). */
    var meanCn0DbHz: Float? = null,
    /** The microphone's newest sound level, null while it is not listening (ISSUE-NOV10.8-O84 part 2). */
    var noiseDbfs: Float? = null,
    var sensorSnapshot: SensorSnapshot = SensorSnapshot()
) {
    /** Deep copy for holders that outlive a pooled instance, which is zeroed on release. */
    fun snapshot(): LocationUpdate = copy(sensorSnapshot = sensorSnapshot.copy())

    fun reset() {
        deviceId = ""; lat = 0.0; lng = 0.0; accuracy = 0f; speed = -1f; bearing = 0f; altitude = 0.0
        timestamp = 0L; monotonicNanos = 0L; serverArrivalTs = 0L; sequenceId = 0L
        isCharging = false; isPluggedIn = null; batteryLevel = -1; gsmSignalDbm = 0; networkType = "--"
        isBuffered = false; isHistoricalLkg = false; tempCelsius = -1f
        isThermalThrottled = false; cumulativeBytesSent = 0L; status = "VALID"; isNoiseDetectionOff = false; isCritical = false
        satsUsed = -1; satsInView = -1; meanCn0DbHz = null; noiseDbfs = null
        sensorSnapshot.reset()
    }
}

@Serializable
data class SensorSnapshot(
    var vibration: Float = 0f,
    var vibrationFloor: Float = 0.12f,
    var lux: Float = 0f,
    var pressure: Float = 0f,
    var tiltDegrees: Float = 0f,
    var temperature: Float = 0f
) {
    fun reset() {
        vibration = 0f; vibrationFloor = 0.12f; lux = 0f
        pressure = 0f; tiltDegrees = 0f; temperature = 0f
    }
}

@Serializable
data class ViolationEvent(
    val type: String,
    val location: LocationUpdate,
    val nonce: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: Int = 1,
    val isBuffered: Boolean = false,
    val criticalAckRequired: Boolean = true,
    val requiresBiometricAuth: Boolean = false
)

@Serializable
data class HandshakeRequest(
    val deviceId: String,
    val nonce: String,
    val signature: String,
    val protocolVersion: Int = 1,
    val minMonitorVersion: Int = 1,
    val lastReceivedSequenceId: Long = 0L,
    val isOwner: Boolean = false
)

@Serializable
data class Acknowledgement(
    val upToSequenceId: Long,
    val nonce: String? = null
)

/**
 * BackfillRequest: Sent by Monitor to request missing data (R-ID 768).
 */
@Serializable
data class BackfillRequest(
    val deviceId: String,
    val startSequenceId: Long,
    val endSequenceId: Long,
    val requestId: String
)

/**
 * BackfillResponse: Sent by Sensor in response to BackfillRequest.
 */
@Serializable
data class BackfillResponse(
    val requestId: String,
    val updates: List<LocationUpdate>,
    val isComplete: Boolean
)

/** Telemetry status and violation type the Sensor sends when the tracker leaves all Home Points (ISSUE-NOV8.13-O51). */
const val GEOFENCE_BREACH_STATUS = "GEOFENCE_BREACH"

/** Telemetry status and violation type the Sensor sends when its microphone hears a sudden loud noise (ISSUE-NOV5.2-O11 Group 4). */
const val ACOUSTIC_ALERT_STATUS = "ACOUSTIC_ALERT"

/** Telemetry status and violation type the Sensor sends when the resting tracker is tilted (ISSUE-NOV5.2-O11 Group 5). */
const val TILT_ALERT_STATUS = "TILT_ALERT"

/** Telemetry status and violation type the Sensor sends when the resting tracker is lifted (ISSUE-NOV5.2-O11 Group 5). */
const val LIFT_ALERT_STATUS = "LIFT_ALERT"

/** Telemetry status and violation type the Sensor sends when the resting tracker is knocked or shaken (ISSUE-NOV5.2-O11 Group 7). */
const val VIBRATION_ALERT_STATUS = "VIBRATION_ALERT"

/** A user-defined Home Point; the tracker is home while it is within the geofence radius of any one of them. */
@Serializable
data class HomePoint(val lat: Double, val lng: Double)

/**
 * The geofence the Monitor user defines and the Sensor enforces (ISSUE-NOV8.13-O51).
 * The Monitor edits it and sends it; the Sensor stores it and checks every fix, so the Monitor does no per-fix work.
 * [revision] is the Monitor's edit time: a peer keeps only the newest revision, so a late copy never overwrites a newer one.
 */
@Serializable
data class GeofenceConfig(
    val points: List<HomePoint> = emptyList(),
    val radiusMeters: Double = DEFAULT_RADIUS_METERS,
    val revision: Long = 0L
) {
    val isActive: Boolean get() = points.isNotEmpty()

    fun withPoint(point: HomePoint): GeofenceConfig = copy(points = points + point)

    fun withoutPoint(index: Int): GeofenceConfig =
        if (index in points.indices) copy(points = points.filterIndexed { i, _ -> i != index }) else this

    fun withRadius(meters: Double): GeofenceConfig = copy(radiusMeters = meters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS))

    fun cleared(): GeofenceConfig = copy(points = emptyList())

    /** This config with a revision above [previous], at least [nowMs], so it wins even if the clock went back. */
    fun revisedAfter(previous: GeofenceConfig, nowMs: Long): GeofenceConfig =
        copy(revision = maxOf(nowMs, previous.revision + 1))

    companion object {
        const val DEFAULT_RADIUS_METERS = 100.0
        const val MIN_RADIUS_METERS = 50.0
        const val MAX_RADIUS_METERS = 500.0
    }
}

/**
 * Master Alarming & Alert Setup Configuration (R747 Locality Authority & R872 Stealth)
 */
@Serializable
data class AlertSettings(
    val localInternet: Boolean = true,
    val serverConnection: Boolean = true,
    val relayConnection: Boolean = true,
    val jammerDetection: Boolean = true,
    val signalLoss: Boolean = true,
    val gpsStalling: Boolean = true,
    val distance: Boolean = true,
    val power: Boolean = true,
    val lowBattery: Boolean = true,
    val batteryHealth: Boolean = true,
    val longTimeGap: Boolean = true,
    val highTemperature: Boolean = true,
    val overrideSilence: Boolean = true,
    val useMaxVolume: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val alarmVolume: Float = 0.8f,
    val useCustomVolume: Boolean = false,
    val tiltAlert: Boolean = true,
    val acousticAlert: Boolean = true,
    val liftAlert: Boolean = true,
    val tamperAlert: Boolean = true,
    val globalMute: Boolean = false,
    val systemStorageLow: Boolean = true,
    val vibrationSensitivity: Float = DEFAULT_VIBRATION_SENSITIVITY,
    val tiltSensitivity: Float = DEFAULT_TILT_SENSITIVITY,
    val noiseSensitivity: Float = DEFAULT_NOISE_SENSITIVITY,
    /** The revision of the Sensor's part ([sensorBehaviour]), stamped by the Monitor's store on each change (ISSUE-NOV10.8-O83). */
    val sensorRevision: Long = 0L,
    val sirenType: String = "Siren"
) {
    /** What the Sensor must do, from the Monitor's switches and sliders (ISSUE-NOV10.8-O83). */
    fun sensorBehaviour(): SensorBehaviour = SensorBehaviour(
        tiltAlert = tiltAlert,
        vibrationAlert = tamperAlert,
        noiseAlert = acousticAlert,
        liftAlert = liftAlert,
        tiltSensitivity = tiltSensitivity,
        vibrationSensitivity = vibrationSensitivity,
        noiseSensitivity = noiseSensitivity,
        revision = sensorRevision
    )

    /** These settings with a new [sensorRevision] when their Sensor part differs from [previous]'s, else with [previous]'s. */
    fun revisedAfter(previous: AlertSettings, nowMs: Long): AlertSettings {
        val changed = sensorBehaviour().copy(revision = 0L) != previous.sensorBehaviour().copy(revision = 0L)
        return copy(sensorRevision = if (changed) maxOf(nowMs, previous.sensorRevision + 1) else previous.sensorRevision)
    }
}

/**
 * The Sensor's detection settings, set on the Monitor and sent by relay like the geofence (ISSUE-NOV10.8-O83, user
 * decisions 2026-11-15): a switched-off kind is not detected at all (noise off closes the microphone), and the three
 * sliders set the limits. [revision] is the Monitor's edit time: the Sensor keeps only the newest.
 */
@Serializable
data class SensorBehaviour(
    val tiltAlert: Boolean = true,
    val vibrationAlert: Boolean = true,
    val noiseAlert: Boolean = true,
    val liftAlert: Boolean = true,
    val tiltSensitivity: Float = DEFAULT_TILT_SENSITIVITY,
    val vibrationSensitivity: Float = DEFAULT_VIBRATION_SENSITIVITY,
    val noiseSensitivity: Float = DEFAULT_NOISE_SENSITIVITY,
    val revision: Long = 0L
) {
    /** False for an alert [status] whose kind is switched off. */
    fun allows(status: String): Boolean = when (status) {
        TILT_ALERT_STATUS -> tiltAlert
        VIBRATION_ALERT_STATUS -> vibrationAlert
        ACOUSTIC_ALERT_STATUS -> noiseAlert
        LIFT_ALERT_STATUS -> liftAlert
        else -> true
    }
}

