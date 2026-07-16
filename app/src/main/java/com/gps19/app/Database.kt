package com.gps19.app

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import com.gps19.core.engine.*

/**
 * Database: persistence configuration for GPS Tracker.
 * July.1.16:
 * - Issue #510: Abandoned Chair Sit Detection. Removed sit-related fields from all entities.
 * - Issue #515: Removed isAnchorLocked from all entities.
 * - Issue #511: Simplified Ribbon Telemetry in HistoryEntity.
 * - Database Version: 57.
 */
@Entity(tableName = "logs", indices = [Index(value = ["timestamp"]), Index(value = ["localId"])])
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localId: String,
    val timestamp: Long,
    val message: String,
    val type: String,
    val isImportant: Boolean,
    val deviceId: String,
    val viewerId: String,
    val count: Int = 1,
    val extremeValue: Double? = null,
    val durationMs: Long = 0L,
    val isSpecial: Boolean = false,
    val specialColor: Int? = null,
    @ColumnInfo(defaultValue = "0") val firstSeenTs: Long = 0L,
    @ColumnInfo(defaultValue = "tracker") val role: String = "tracker",
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
    @ColumnInfo(defaultValue = "0") val lat: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val lng: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val accuracy: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val maxAccuracy: Double = 0.0,
    val snrSnapshot: Double? = null,
    val vibeSnapshot: Double? = null
)

@Entity(tableName = "trail_points", indices = [Index(value = ["timestamp"])])
data class TrailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lng") val lng: Double,
    val timestamp: Long,
    val isViewerTrail: Boolean,
    @ColumnInfo(defaultValue = "VALID") val status: String = "VALID",
    @ColumnInfo(defaultValue = "0") val accuracy: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val maxAccuracy: Double = 0.0
)

@Entity(tableName = "connection_history", indices = [Index(value = ["ts"])])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val rtt: Int,
    val isConnected: Boolean,
    val isGap: Boolean,
    val hasGps: Boolean,
    val isTick: Boolean,
    val ribbonKey: String,
    @ColumnInfo(name = "isBatterySteepDischarge", defaultValue = "0") val isBatterySteepDischarge: Boolean = false,
    @ColumnInfo(defaultValue = "10") val remoteSig: Int = 10,
    @ColumnInfo(defaultValue = "0") val isCoolingModeActive: Boolean = false,
    @ColumnInfo(defaultValue = "0.0") val speed: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val bearing: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val currentMa: Int = 0,
    @ColumnInfo(defaultValue = "NONE") val locationPendingReason: String = "NONE",
    @ColumnInfo(defaultValue = "0.0") val accuracy: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val maxAccuracy: Double = 0.0
)

@Entity(tableName = "violations", indices = [Index(value = ["ts"])])
data class ViolationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val type: String,
    val ts: Long,
    @ColumnInfo(defaultValue = "0") val accuracy: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val maxAccuracy: Double = 0.0
)

@Entity(tableName = "pending_status_updates", indices = [Index(value = ["timestamp"])])
data class PendingStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val speed: Double,
    val accuracy: Double,
    val bearing: Double,
    val battery: Int,
    val temp: Double,
    val isCharging: Boolean,
    @ColumnInfo(defaultValue = "0") val currentMa: Int = 0,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "0") val gpsTs: Long = 0L,
    val satsView: Int,
    val satsUsed: Int,
    val name: String? = null,
    val maxAccuracy: Double,
    val distToTracker: Double? = null,
    val distToHome: Double? = null,
    @ColumnInfo(name = "isBatterySteepDischarge", defaultValue = "0") val isBatterySteepDischarge: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isCoolingModeActive: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isStorageLow: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isStorageCritical: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPowerSaveMode: Boolean = false,
    @ColumnInfo(defaultValue = "-1") val standbyBucket: Int = -1,
    @ColumnInfo(defaultValue = "UNKNOWN") val netInterface: String = "UNKNOWN",
    @ColumnInfo(defaultValue = "0") val lastValidFixRealtime: Long = 0L,
    @ColumnInfo(defaultValue = "NONE") val locationPendingReason: String = "NONE",
    @ColumnInfo(defaultValue = "UNKNOWN") val trackerState: String = "UNKNOWN",
    @ColumnInfo(defaultValue = "VALID") val status: String = "VALID"
)

@Dao
interface LogDao {
    @Insert suspend fun insert(log: LogEntity): Long
    @Update suspend fun update(log: LogEntity)
    @Query("SELECT * FROM logs WHERE localId = :localId") suspend fun getLogByLocalId(localId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1") suspend fun getLastLog(): LogEntity?
    @Query("SELECT * FROM logs WHERE type = :type AND role = :role AND deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1") suspend fun getLastLogByMetadata(type: String, role: String, deviceId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000") fun getAllLogs(): Flow<List<LogEntity>>
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000") suspend fun getAllLogsStatic(): List<LogEntity>
    @Query("SELECT * FROM logs WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit") suspend fun getUnsyncedLogs(limit: Int): List<LogEntity>
    @Query("UPDATE logs SET synced = 1 WHERE localId IN (:localIds)") suspend fun markLogsAsSynced(localIds: List<String>)
    @Query("DELETE FROM logs") suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM logs") suspend fun getCount(): Int
    @Query("DELETE FROM logs WHERE timestamp < (SELECT timestamp FROM logs ORDER BY timestamp DESC LIMIT 1 OFFSET 999)") suspend fun pruneLogs()
}

@Dao
interface TrailDao {
    @Insert suspend fun insert(point: TrailEntity)
    @Query("SELECT * FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp ASC LIMIT 1000") fun getTrail(isViewer: Boolean): Flow<List<TrailEntity>>
    @Query("SELECT * FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp ASC LIMIT 1000") suspend fun getTrailStatic(isViewer: Boolean): List<TrailEntity>
    @Query("DELETE FROM trail_points WHERE isViewerTrail = :isViewer") suspend fun clearTrail(isViewer: Boolean)
    @Query("DELETE FROM trail_points WHERE isViewerTrail = :isViewer AND timestamp < (SELECT timestamp FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp DESC LIMIT 1 OFFSET 999)") suspend fun pruneTrail(isViewer: Boolean): Int
}

@Dao
interface HistoryDao {
    @Insert suspend fun insert(point: HistoryEntity)
    @Insert suspend fun insertAll(points: List<HistoryEntity>)
    @Query("SELECT * FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts ASC LIMIT 1000") fun getHistoryFlow(ribbonKey: String): Flow<List<HistoryEntity>>
    @Query("SELECT * FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts ASC LIMIT 1000") suspend fun getHistory(ribbonKey: String): List<HistoryEntity>
    @Query("DELETE FROM connection_history WHERE ribbonKey = :ribbonKey") suspend fun clearHistory(ribbonKey: String)
    @Query("DELETE FROM connection_history") suspend fun clearAll()
    @Query("DELETE FROM connection_history WHERE ribbonKey = :ribbonKey AND ts < (SELECT ts FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts DESC LIMIT 1 OFFSET 999)") suspend fun pruneHistory(ribbonKey: String)
}

@Dao
interface ViolationDao {
    @Insert suspend fun insert(violation: ViolationEntity)
    @Query("SELECT * FROM violations ORDER BY ts ASC LIMIT 1000") fun getAllFlow(): Flow<List<ViolationEntity>>
    @Query("SELECT * FROM violations ORDER BY ts ASC LIMIT 1000") suspend fun getAll(): List<ViolationEntity>
    @Query("DELETE FROM violations") suspend fun clearAll()
    @Query("DELETE FROM violations WHERE ts < (SELECT ts FROM violations ORDER BY ts DESC LIMIT 1 OFFSET 999)") suspend fun prune()
}

@Dao
interface PendingStatusDao {
    @Insert suspend fun insert(status: PendingStatusEntity)
    @Query("SELECT * FROM pending_status_updates ORDER BY timestamp ASC LIMIT :limit") suspend fun getOldestPending(limit: Int): List<PendingStatusEntity>
    @Query("DELETE FROM pending_status_updates WHERE id IN (:ids)") suspend fun deletePending(ids: LongArray)
    @Query("SELECT COUNT(*) FROM pending_status_updates") suspend fun getCount(): Int
    @Query("DELETE FROM pending_status_updates WHERE timestamp < (SELECT timestamp FROM pending_status_updates ORDER BY timestamp DESC LIMIT 1 OFFSET 999)") suspend fun prune()
    @Query("DELETE FROM pending_status_updates") suspend fun clearAll()
}

@Database(entities = [LogEntity::class, TrailEntity::class, HistoryEntity::class, ViolationEntity::class, PendingStatusEntity::class], version = 57, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun trailDao(): TrailDao
    abstract fun historyDao(): HistoryDao
    abstract fun violationDao(): ViolationDao
    abstract fun pendingStatusDao(): PendingStatusDao

    companion object {
        val MIGRATION_56_57 = object : Migration(56, 57) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // connection_history
                db.execSQL("CREATE TABLE connection_history_v57 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ts INTEGER NOT NULL, rtt INTEGER NOT NULL, isConnected INTEGER NOT NULL, isGap INTEGER NOT NULL, hasGps INTEGER NOT NULL, isTick INTEGER NOT NULL, ribbonKey TEXT NOT NULL, isBatterySteepDischarge INTEGER NOT NULL DEFAULT 0, remoteSig INTEGER NOT NULL DEFAULT 10, isCoolingModeActive INTEGER NOT NULL DEFAULT 0, speed REAL NOT NULL DEFAULT 0.0, bearing REAL NOT NULL DEFAULT 0.0, currentMa INTEGER NOT NULL DEFAULT 0, locationPendingReason TEXT NOT NULL DEFAULT 'NONE', accuracy REAL NOT NULL DEFAULT 0.0, maxAccuracy REAL NOT NULL DEFAULT 0.0)")
                db.execSQL("INSERT INTO connection_history_v57 (id, ts, rtt, isConnected, isGap, hasGps, isTick, ribbonKey, isBatterySteepDischarge, remoteSig, isCoolingModeActive, speed, bearing, currentMa, locationPendingReason, accuracy, maxAccuracy) SELECT id, ts, rtt, isConnected, isGap, hasGps, isTick, ribbonKey, isBatterySteepDischarge, remoteSig, isCoolingModeActive, speed, bearing, currentMa, locationPendingReason, accuracy, maxAccuracy FROM connection_history")
                db.execSQL("DROP TABLE connection_history")
                db.execSQL("ALTER TABLE connection_history_v57 RENAME TO connection_history")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_connection_history_ts ON connection_history (ts)")

                // pending_status_updates
                db.execSQL("CREATE TABLE pending_status_updates_v57 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, speed REAL NOT NULL, accuracy REAL NOT NULL, bearing REAL NOT NULL, battery INTEGER NOT NULL, temp REAL NOT NULL, isCharging INTEGER NOT NULL, currentMa INTEGER NOT NULL DEFAULT 0, timestamp INTEGER NOT NULL, gpsTs INTEGER NOT NULL DEFAULT 0, satsView INTEGER NOT NULL, satsUsed INTEGER NOT NULL, name TEXT, maxAccuracy REAL NOT NULL, distToTracker REAL, distToHome REAL, isBatterySteepDischarge INTEGER NOT NULL DEFAULT 0, isCoolingModeActive INTEGER NOT NULL DEFAULT 0, isStorageLow INTEGER NOT NULL DEFAULT 0, isStorageCritical INTEGER NOT NULL DEFAULT 0, isPowerSaveMode INTEGER NOT NULL DEFAULT 0, standbyBucket INTEGER NOT NULL DEFAULT -1, netInterface TEXT NOT NULL DEFAULT 'UNKNOWN', lastValidFixRealtime INTEGER NOT NULL DEFAULT 0, locationPendingReason TEXT NOT NULL DEFAULT 'NONE', trackerState TEXT NOT NULL DEFAULT 'UNKNOWN', status TEXT NOT NULL DEFAULT 'VALID')")
                db.execSQL("INSERT INTO pending_status_updates_v57 (id, lat, lng, speed, accuracy, bearing, battery, temp, isCharging, currentMa, timestamp, gpsTs, satsView, satsUsed, name, maxAccuracy, distToTracker, distToHome, isBatterySteepDischarge, isCoolingModeActive, isStorageLow, isStorageCritical, isPowerSaveMode, standbyBucket, netInterface, lastValidFixRealtime, locationPendingReason, trackerState, status) SELECT id, lat, lng, speed, accuracy, bearing, battery, temp, isCharging, currentMa, timestamp, gpsTs, satsView, satsUsed, name, maxAccuracy, distToTracker, distToHome, isBatterySteepDischarge, isCoolingModeActive, isStorageLow, isStorageCritical, isPowerSaveMode, standbyBucket, netInterface, lastValidFixRealtime, locationPendingReason, trackerState, status FROM pending_status_updates")
                db.execSQL("DROP TABLE pending_status_updates")
                db.execSQL("ALTER TABLE pending_status_updates_v57 RENAME TO pending_status_updates")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_status_updates_timestamp ON pending_status_updates (timestamp)")

                // trail_points
                db.execSQL("CREATE TABLE trail_points_v57 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, timestamp INTEGER NOT NULL, isViewerTrail INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'VALID', accuracy REAL NOT NULL DEFAULT 0.0, maxAccuracy REAL NOT NULL DEFAULT 0.0)")
                db.execSQL("INSERT INTO trail_points_v57 (id, lat, lng, timestamp, isViewerTrail, status, accuracy, maxAccuracy) SELECT id, lat, lng, timestamp, isViewerTrail, (CASE WHEN isJump = 1 THEN 'JUMP' ELSE 'VALID' END), accuracy, maxAccuracy FROM trail_points")
                db.execSQL("DROP TABLE trail_points")
                db.execSQL("ALTER TABLE trail_points_v57 RENAME TO trail_points")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trail_points_timestamp ON trail_points (timestamp)")
            }
        }
    }
}
