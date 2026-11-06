package com.jd.app.monitor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * MonitorDatabase: Stores telemetry for both the local user and the remote tracker.
 * Hardened for Revol30.6: Enforces unique sequenceId to prevent backfill duplicates (R-ID 770).
 */
const val MAX_TRAIL_POINTS = 1000

/** Rows stored per tracker. About 1.4 hours at the 10Hz charging rate, or 5.8 days at the 10s rate. */
const val MAX_STORED_TRAIL_POINTS = 50_000

@Entity(
    tableName = "trails",
    primaryKeys = ["trackerId", "timestamp"],
    indices = [Index(value = ["trackerId", "sequenceId"], unique = true)]
)
data class TrailEntity(
    val trackerId: String,
    val timestamp: Long,
    val sequenceId: Long, // Forensic monotonic counter
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val status: String,
    val isLocalUser: Boolean = false
)

@Dao
interface TrailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trail: TrailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trails: List<TrailEntity>)

    @Query("SELECT * FROM trails WHERE trackerId = :id ORDER BY timestamp DESC LIMIT $MAX_TRAIL_POINTS")
    fun getTrail(id: String): Flow<List<TrailEntity>>

    @Query("SELECT sequenceId FROM trails WHERE trackerId = :id AND isLocalUser = 0 ORDER BY sequenceId DESC LIMIT 1")
    suspend fun getLastSequenceId(id: String): Long?

    /**
     * Prunes local user trails faster than remote sensor trails.
     */
    @Query("DELETE FROM trails WHERE isLocalUser = 1 AND timestamp < :threshold")
    suspend fun pruneLocalTrail(threshold: Long)

    @Query("SELECT DISTINCT trackerId FROM trails")
    suspend fun getTrackerIds(): List<String>

    /**
     * Keeps the newest [keep] rows of one tracker and returns the number deleted.
     * Limited by count, not age, so a tracker always keeps its newest rows and
     * [getLastSequenceId] cannot fall to 0 and request a full backfill.
     * The primary key makes timestamps unique per tracker and serves this lookup.
     * With [keep] rows or fewer the lookup is NULL and nothing is deleted.
     */
    @Query(
        """
        DELETE FROM trails WHERE trackerId = :id AND timestamp <= (
            SELECT timestamp FROM trails WHERE trackerId = :id
            ORDER BY timestamp DESC LIMIT 1 OFFSET :keep
        )
        """
    )
    suspend fun pruneOldestRows(id: String, keep: Int): Int
}

@Database(entities = [TrailEntity::class], version = 3, exportSchema = false)
abstract class MonitorDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao
}
