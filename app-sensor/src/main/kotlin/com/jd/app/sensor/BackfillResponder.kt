package com.jd.app.sensor

import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate

/** Rows per backfill message. */
internal const val BACKFILL_CHUNK_SIZE = 50

/**
 * Answers one backfill request in chunks, and stops at the first chunk that cannot be sent
 * (ISSUE-NOV6.4-O33).
 *
 * The last chunk tells the Monitor the range is answered. Sending it after a lost chunk would
 * make the Monitor treat the missing rows as unfillable. Without it, the Monitor's request
 * times out and its next gap scan asks again for the rows that are still missing.
 *
 * Returns true if every chunk was sent.
 */
internal suspend fun answerBackfill(
    requestId: String,
    updates: List<LocationUpdate>,
    send: suspend (BackfillResponse) -> Boolean
): Boolean {
    if (updates.isEmpty()) return send(BackfillResponse(requestId, emptyList(), isComplete = true))

    val chunks = updates.chunked(BACKFILL_CHUNK_SIZE)
    chunks.forEachIndexed { index, chunk ->
        val response = BackfillResponse(requestId, chunk, isComplete = index == chunks.lastIndex)
        if (!send(response)) return false
    }
    return true
}
