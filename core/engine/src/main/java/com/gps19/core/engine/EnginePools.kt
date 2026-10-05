package com.gps19.core.engine

/**
 * EnginePools: Centralized ring-buffered pools for high-frequency engine entities.
 * Oct.4.6:
 * - Issue #1160: Flyweight & Pooling Expansion. Expanded with TRAJECTORY_NODE 
 *   and standardized capacities for high-load bursts (R1160).
 */
object EnginePools {
    /**
     * LOCATION_UPDATE: Pool for LocationUpdate snapshots used in DomainEvents.
     * Capacity 256 covers the DomainEventBus buffer (128) plus active processing headroom.
     */
    val LOCATION_UPDATE = RingBufferPool(
        capacity = 256,
        factory = { LocationUpdate() },
        resetter = { it.reset() }
    )

    /**
     * PROCESSED_LOCATION: Pool for processed location results.
     */
    val PROCESSED_LOCATION = RingBufferPool(
        capacity = 128,
        factory = { ProcessedLocation() },
        resetter = { it.reset() }
    )

    /**
     * SYSTEM_HEALTH: Pool for system health snapshots.
     */
    val SYSTEM_HEALTH = RingBufferPool(
        capacity = 128,
        factory = { SystemHealthState() },
        resetter = { it.reset() }
    )

    /**
     * CONNECTION_POINT: Pool for ribbon aggregation and persistence.
     */
    val CONNECTION_POINT = RingBufferPool(
        capacity = 128,
        factory = { EngineConnectionPoint() },
        resetter = { /* reset logic if needed, currently copyFrom handles all fields */ }
    )

    /**
     * GEO_POINT: Pool for intermediate spatial calculations.
     */
    val GEO_POINT = RingBufferPool(
        capacity = 256,
        factory = { EngineGeoPoint() },
        resetter = { it.update(0.0, 0.0) }
    )

    /**
     * SENTINEL_RESULT: Pool for location validation results.
     */
    val SENTINEL_RESULT = RingBufferPool(
        capacity = 64,
        factory = { 
            SentinelResult().apply { 
                jumpConfidence = JumpConfidence() 
            } 
        },
        resetter = { it.reset() }
    )

    /**
     * TRAJECTORY_NODE: Pool for trajectory optimization nodes.
     */
    val TRAJECTORY_NODE = RingBufferPool(
        capacity = 128,
        factory = { TrajectoryNode() },
        resetter = { it.reset() }
    )
}
