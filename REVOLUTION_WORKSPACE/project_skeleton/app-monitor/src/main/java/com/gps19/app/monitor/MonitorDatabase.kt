package com.gps19.app.monitor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * MonitorDatabase: Corrected for Local vs Remote pruning (Point 31) 
 * and schema cleanup (Point 68).
 */
@Entity(tableName = "trails", primaryKeys = ["trackerId", "timestamp"])
data class TrailEntity(
    val trackerId: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val status: String,
    val isLocalUser: Boolean = false // Point 31: Distinguishes "You" vs "Them"
)

@Dao
interface TrailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trail: TrailEntity)

    @Query("SELECT * FROM trails WHERE trackerId = :id ORDER BY timestamp DESC")
    fun getTrail(id: String): Flow<List<TrailEntity>>

    /**
     * Corrected: Prunes local user trails faster than remote sensor trails.
     */
    @Query("DELETE FROM trails WHERE isLocalUser = 1 AND timestamp < :threshold")
    suspend fun pruneLocalTrail(threshold: Long)
}

@Database(entities = [TrailEntity::class], version = 1, exportSchema = false)
abstract class MonitorDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao
}
