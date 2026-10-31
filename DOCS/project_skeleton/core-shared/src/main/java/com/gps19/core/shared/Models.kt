package com.gps19.core.shared

import kotlinx.serialization.Serializable

/**
 * Project Revolution: Master Telemetry & Command Models
 * Hardened for Zero-Allocation Pooling, E2EE, and Forensic Integrity.
 */

@Serializable
data class LocationUpdate(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var accuracy: Float = 0f,
    var speed: Float = 0f,
    var bearing: Float = 0f,
    var altitude: Double = 0.0,
    var timestamp: Long = 0L,
    var monotonicNanos: Long = 0L,
    var serverArrivalTs: Long = 0L, 
    var sequenceId: Long = 0L,
    var isCharging: Boolean = false,
    var batteryLevel: Int = 0,
    var gsmSignalDbm: Int = 0,
    var networkType: String = "--",
    var isBuffered: Boolean = false,
    var isHistoricalLkg: Boolean = false,
    var tempCelsius: Float = 0f,
    var isThermalThrottled: Boolean = false,
    var cumulativeBytesSent: Long = 0L,
    var sensorSnapshot: SensorSnapshot = SensorSnapshot()
) {
    fun reset() {
        lat = 0.0; lng = 0.0; accuracy = 0f; speed = 0f; bearing = 0f; altitude = 0.0
        timestamp = 0L; monotonicNanos = 0L; serverArrivalTs = 0L; sequenceId = 0L
        isCharging = false; batteryLevel = 0; gsmSignalDbm = 0; networkType = "--"
        isBuffered = false; isHistoricalLkg = false; tempCelsius = 0f
        isThermalThrottled = false; cumulativeBytesSent = 0L
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
