package com.gps19.app.monitor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * MonitorDatabase: Stores telemetry for both the local user and the remote tracker.
 * Hardened for Revol30.3: Includes sequenceId for forensic backfill gap detection (R-ID 768).
 */
@Entity(tableName = "trails", primaryKeys = ["trackerId", "timestamp"])
data class TrailEntity(
    val trackerId: String,
    val timestamp: Long,
    val sequenceId: Long, // Added for gap detection
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

    @Query("SELECT * FROM trails WHERE trackerId = :id ORDER BY timestamp DESC")
    fun getTrail(id: String): Flow<List<TrailEntity>>

    @Query("SELECT sequenceId FROM trails WHERE trackerId = :id AND isLocalUser = 0 ORDER BY sequenceId DESC LIMIT 1")
    suspend fun getLastSequenceId(id: String): Long?

    /**
     * Prunes local user trails faster than remote sensor trails.
     */
    @Query("DELETE FROM trails WHERE isLocalUser = 1 AND timestamp < :threshold")
    suspend fun pruneLocalTrail(threshold: Long)
}

@Database(entities = [TrailEntity::class], version = 2, exportSchema = false)
abstract class MonitorDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao
}
