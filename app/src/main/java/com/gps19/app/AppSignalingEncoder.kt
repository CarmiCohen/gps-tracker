package com.gps19.app

import com.google.protobuf.CodedOutputStream
import com.gps19.core.engine.*
import timber.log.Timber
import java.util.Arrays

/**
 * AppSignalingEncoder: App-side implementation of the signaling encoder.
 * Oct.6.20:
 * - Issue #SIGN-1006-12: Centralized Protobuf serialization and compression logic.
 *   Uses a thread-local buffer to minimize allocations during high-frequency bursts (Rule 1.130).
 */
class AppSignalingEncoder : SignalingEncoder {

    private val statusBuilder = RealtimeStatus.newBuilder()
    private var serializationBuffer = ByteArray(4096)
    private val MAX_SERIALIZATION_BUFFER_SIZE = 65536

    override fun encodeObject(
        update: LocationUpdate, 
        deltaState: SignalingDeltaState, 
        fromViewer: Boolean
    ): ByteArray {
        synchronized(statusBuilder) {
            statusBuilder.clear()
            TelemetryProtobufMapper.mapToRealtime(update, statusBuilder, fromViewer, deltaState)
            val message = statusBuilder.buildPartial()
            val size = message.serializedSize

            // Dynamic buffer scaling (Rule 1.130)
            if (size > serializationBuffer.size && size <= MAX_SERIALIZATION_BUFFER_SIZE) {
                serializationBuffer = ByteArray((serializationBuffer.size * 2).coerceAtLeast(size).coerceAtMost(MAX_SERIALIZATION_BUFFER_SIZE))
            }

            val rawData = if (size <= serializationBuffer.size) {
                try {
                    val cos = CodedOutputStream.newInstance(serializationBuffer, 0, size)
                    message.writeTo(cos)
                    cos.checkNoSpaceLeft()
                    Arrays.copyOf(serializationBuffer, size)
                } catch (e: Exception) {
                    Timber.e(e, "Pre-allocated serialization failed, falling back to toByteArray()")
                    message.toByteArray()
                }
            } else {
                message.toByteArray()
            }

            // Apply Gzip compression if needed (Issue #AUDIT-1006-11)
            return CompressionUtils.compressIfNeeded(rawData)
        }
    }
}
