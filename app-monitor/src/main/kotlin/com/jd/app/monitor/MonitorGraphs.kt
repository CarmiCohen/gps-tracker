package com.jd.app.monitor

import com.jd.core.shared.GraphField
import com.jd.core.shared.GraphHistory
import com.jd.core.shared.GraphSample

/**
 * The Monitor's two sample stores for the Graphs (ISSUE-NOV10.8-O84 part 3): [sensor] holds the paired Sensor's samples
 * as received, [own] this phone's own connection. Unpairing clears [sensor], so another Sensor's samples never mix in.
 */
class MonitorGraphs(val sensor: GraphHistory, val own: GraphHistory)

/** The paired Sensor's rows as this Monitor received them in one minute, and how many are known to be missing. */
data class RowCounts(val live: Int, val backfilled: Int, val missing: Long)

/** The Monitor's own sample of one minute (ISSUE-NOV10.8-O84 part 3). */
fun monitorGraphSample(timeMs: Long, linkUpPercent: Float, latencyMs: Long?, rows: RowCounts): GraphSample = GraphSample.of(
    timeMs,
    mapOf(
        GraphField.LINK_UP_PERCENT to linkUpPercent,
        GraphField.LATENCY_MS to latencyMs,
        GraphField.ROWS_LIVE to rows.live,
        GraphField.ROWS_BACKFILLED to rows.backfilled,
        GraphField.ROWS_MISSING to rows.missing
    )
)
