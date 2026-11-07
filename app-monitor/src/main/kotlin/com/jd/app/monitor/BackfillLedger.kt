package com.jd.app.monitor

import com.jd.core.shared.BackfillRequest

/**
 * BackfillLedger: Decides which missing ranges to request (ISSUE-NOV6.3-O32).
 *
 * The trail table says what is missing; this class only remembers what was asked. Answered
 * ranges are not asked again, because a hole can be unfillable: the Sensor buffer overwrites
 * old rows, and a Sensor restart can skip sequence numbers. That memory ends with the process,
 * so each remaining hole costs one more request after a restart.
 *
 * Not thread safe: callers hold a lock.
 */
class BackfillLedger(
    private val maxActive: Int = MAX_ACTIVE_REQUESTS,
    private val timeoutMs: Long = REQUEST_TIMEOUT_MS
) {
    private class ActiveRequest(val request: BackfillRequest, var lastActivityMs: Long)

    private val active = mutableMapOf<String, ActiveRequest>()
    private val answered = mutableListOf<BackfillRequest>()

    /** Returns the gaps to request now, in the given order. The rest wait for a free slot. */
    fun gapsToRequest(deviceId: String, gaps: List<SequenceGap>, nowMs: Long): List<SequenceGap> {
        active.values.removeAll { nowMs - it.lastActivityMs >= timeoutMs }
        answered.removeAll { request -> request.deviceId == deviceId && gaps.none { it.overlaps(request) } }

        val open = gaps.filterNot { gap ->
            answered.any { gap.isInside(it, deviceId) } || active.values.any { gap.isInside(it.request, deviceId) }
        }
        return open.take(maxActive - active.size)
    }

    fun onRequestSent(request: BackfillRequest, nowMs: Long) {
        active[request.requestId] = ActiveRequest(request, nowMs)
    }

    /** A large range can outlast the timeout, so each chunk restarts it. */
    fun onChunkReceived(requestId: String, nowMs: Long) {
        active[requestId]?.lastActivityMs = nowMs
    }

    /** Returns the finished request, or null if it had already timed out. */
    fun onRequestComplete(requestId: String): BackfillRequest? =
        active.remove(requestId)?.request?.also { answered += it }

    private fun SequenceGap.isInside(request: BackfillRequest, deviceId: String) =
        request.deviceId == deviceId &&
            request.startSequenceId <= firstMissing && request.endSequenceId >= lastMissing

    private fun SequenceGap.overlaps(request: BackfillRequest) =
        firstMissing <= request.endSequenceId && lastMissing >= request.startSequenceId

    companion object {
        const val MAX_ACTIVE_REQUESTS = 10
        const val REQUEST_TIMEOUT_MS = 30_000L
    }
}
