package com.gps19.app

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import com.gps19.core.engine.*

/**
 * Database: persistence configuration for GPS Tracker.
 * July.21.00:
 * - Issue #105: Incremented version to 58 to resolve Room identity hash mismatch.
 * - Restored Sit Detection and Anchor Lock forensic fields across all entities.
 * - Issue #102: Temporal Forensic Integrity. Standardized 'lastValidFixRt'.
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
    @ColumnInfo(defaultValue = "0") val gpsIndex: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val noiseIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val luxIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val vibeIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "1") val proxIdx: Double = 1.0, 
    @ColumnInfo(defaultValue = "0") val liftIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val snrIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val tiltIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val baroIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val verticalVelocity: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitVz: Double = 0.0, 
    @ColumnInfo(defaultValue = "0") val sitVzTs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val sitDz: Double = 0.0,
    @ColumnInfo(name = "isBatterySteepDischarge", defaultValue = "0") val isBatterySteepDischarge: Boolean = false,
    @ColumnInfo(defaultValue = "10") val remoteSig: Int = 10,
    @ColumnInfo(defaultValue = "0") val isCoolingModeActive: Boolean = false,
    @ColumnInfo(defaultValue = "0") val speed: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val bearing: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val isSitDetected: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isSitActive: Boolean = false,
    @ColumnInfo(defaultValue = "0") val sitBaro: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitTilt: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitShock: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val currentMa: Int = 0,
    @ColumnInfo(defaultValue = "NONE") val locationPendingReason: String = "NONE",
    @ColumnInfo(defaultValue = "0") val accuracy: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val maxAccuracy: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val isAnchorLocked: Boolean = false
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
    val lat: Double, val lng: Double, val speed: Double, val accuracy: Double, val bearing: Double,
    val battery: Int, val temp: Double, val isCharging: Boolean,
    @ColumnInfo(defaultValue = "0") val currentMa: Int = 0,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "0") val gpsTs: Long = 0L,
    val satsView: Int, val satsUsed: Int, val name: String? = null, val maxAccuracy: Double,
    val distToTracker: Double? = null, val distToHome: Double? = null,
    @ColumnInfo(defaultValue = "0") val snrIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val tiltIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val baroIdx: Double = 0.0,
    @ColumnInfo(name = "isBatterySteepDischarge", defaultValue = "0") val isBatterySteepDischarge: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isCoolingModeActive: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isSitDetected: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isSitActive: Boolean = false,
    @ColumnInfo(defaultValue = "0") val sitVz: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitVzTs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val sitDz: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val verticalVelocity: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitBaro: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitTilt: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitShock: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val isStorageLow: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isStorageCritical: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPowerSaveMode: Boolean = false,
    @ColumnInfo(defaultValue = "-1") val standbyBucket: Int = -1,
    @ColumnInfo(defaultValue = "UNKNOWN") val netInterface: String = "UNKNOWN",
    @ColumnInfo(defaultValue = "0") val lastValidFixRt: Long = 0L,
    @ColumnInfo(defaultValue = "NONE") val locationPendingReason: String = "NONE",
    @ColumnInfo(defaultValue = "0") val isAnchorLocked: Boolean = false,
    @ColumnInfo(defaultValue = "UNKNOWN") val trackerState: String = "UNKNOWN",
    @ColumnInfo(defaultValue = "VALID") val status: String = "VALID"
)

@Dao
abstract class LogDao {
    @Insert abstract suspend fun insert(log: LogEntity): Long
    @Update abstract suspend fun update(log: LogEntity)
    @Query("SELECT * FROM logs WHERE localId = :localId") abstract suspend fun getLogByLocalId(localId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1") abstract suspend fun getLastLog(): LogEntity?
    @Query("SELECT * FROM logs WHERE type = :type AND role = :role AND deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1") abstract suspend fun getLastLogByMetadata(type: String, role: String, deviceId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000") abstract fun getAllLogs(): Flow<List<LogEntity>>
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000") abstract suspend fun getAllLogsStatic(): List<LogEntity>
    @Query("SELECT * FROM logs WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit") abstract suspend fun getUnsyncedLogs(limit: Int): List<LogEntity>
    @Query("UPDATE logs SET synced = 1 WHERE localId IN (:localIds)") abstract suspend fun markLogsAsSynced(localIds: List<String>)
    @Query("DELETE FROM logs") abstract suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM logs") abstract suspend fun getCount(): Int

    @Transaction
    open suspend fun deepPruneLogs() {
        pruneRoutineHeartbeats(100)
        pruneNonForensicLogs(500)
    }

    @Query("DELETE FROM logs WHERE type IN ('watchdog_stats', 'viewer_pulse', 'tracker_pulse', 'pong_activity') AND id NOT IN (SELECT id FROM logs WHERE type IN ('watchdog_stats', 'viewer_pulse', 'tracker_pulse', 'pong_activity') ORDER BY timestamp DESC LIMIT :limit)")
    abstract suspend fun pruneRoutineHeartbeats(limit: Int)

    @Query("DELETE FROM logs WHERE isImportant = 0 AND isSpecial = 0 AND id NOT IN (SELECT id FROM logs ORDER BY timestamp DESC LIMIT :limit)")
    abstract suspend fun pruneNonForensicLogs(limit: Int)
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

@Database(entities = [LogEntity::class, TrailEntity::class, HistoryEntity::class, ViolationEntity::class, PendingStatusEntity::class], version = 58, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun trailDao(): TrailDao
    abstract fun historyDao(): HistoryDao
    abstract fun violationDao(): ViolationDao
    abstract fun pendingStatusDao(): PendingStatusDao

    companion object {
        val MIGRATION_56_57 = object : Migration(56, 57) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Issue #097: Re-harmonize all tables to resolve Identity Hash mismatch.
                // logs
                db.execSQL("CREATE TABLE logs_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, localId TEXT NOT NULL, timestamp INTEGER NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, isImportant INTEGER NOT NULL, deviceId TEXT NOT NULL, viewerId TEXT NOT NULL, count INTEGER NOT NULL, extremeValue REAL, durationMs INTEGER NOT NULL, isSpecial INTEGER NOT NULL, specialColor INTEGER, firstSeenTs INTEGER NOT NULL DEFAULT 0, role TEXT NOT NULL DEFAULT 'tracker', synced INTEGER NOT NULL DEFAULT 0, lat REAL NOT NULL DEFAULT 0, lng REAL NOT NULL DEFAULT 0, accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0, snrSnapshot REAL, vibeSnapshot REAL)")
                db.execSQL("INSERT INTO logs_new SELECT id, localId, timestamp, message, type, isImportant, deviceId, viewerId, count, extremeValue, durationMs, isSpecial, specialColor, firstSeenTs, role, synced, lat, lng, accuracy, maxAccuracy, snrSnapshot, vibeSnapshot FROM logs")
                db.execSQL("DROP TABLE logs"); db.execSQL("ALTER TABLE logs_new RENAME TO logs")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_timestamp ON logs (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_localId ON logs (localId)")

                // trail_points
                db.execSQL("CREATE TABLE trail_points_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, timestamp INTEGER NOT NULL, isViewerTrail INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'VALID', accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO trail_points_new (id, lat, lng, timestamp, isViewerTrail, status, accuracy, maxAccuracy) SELECT id, lat, lng, timestamp, isViewerTrail, (CASE WHEN isJump = 1 THEN 'JUMP' ELSE 'VALID' END), accuracy, maxAccuracy FROM trail_points")
                db.execSQL("DROP TABLE trail_points"); db.execSQL("ALTER TABLE trail_points_new RENAME TO trail_points")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trail_points_timestamp ON trail_points (timestamp)")

                // violations
                db.execSQL("CREATE TABLE violations_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, type TEXT NOT NULL, ts INTEGER NOT NULL, accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO violations_new SELECT id, lat, lng, type, ts, accuracy, maxAccuracy FROM violations")
                db.execSQL("DROP TABLE violations"); db.execSQL("ALTER TABLE violations_new RENAME TO violations")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_violations_ts ON violations (ts)")

                // connection_history
                db.execSQL("CREATE TABLE connection_history_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ts INTEGER NOT NULL, rtt INTEGER NOT NULL, isConnected INTEGER NOT NULL, isGap INTEGER NOT NULL, hasGps INTEGER NOT NULL, isTick INTEGER NOT NULL, ribbonKey TEXT NOT NULL, gpsIndex REAL NOT NULL DEFAULT 0, noiseIdx REAL NOT NULL DEFAULT 0, luxIdx REAL NOT NULL DEFAULT 0, vibeIdx REAL NOT NULL DEFAULT 0, proxIdx REAL NOT NULL DEFAULT 1, liftIdx REAL NOT NULL DEFAULT 0, snrIdx REAL NOT NULL DEFAULT 0, tiltIdx REAL NOT NULL DEFAULT 0, baroIdx REAL NOT NULL DEFAULT 0, verticalVelocity REAL NOT NULL DEFAULT 0, sitVz REAL NOT NULL DEFAULT 0, sitVzTs INTEGER NOT NULL DEFAULT 0, sitDz REAL NOT NULL DEFAULT 0, isBatterySteepDischarge INTEGER NOT NULL DEFAULT 0, remoteSig INTEGER NOT NULL DEFAULT 10, isCoolingModeActive INTEGER NOT NULL DEFAULT 0, speed REAL NOT NULL DEFAULT 0, bearing REAL NOT NULL DEFAULT 0, isSitDetected INTEGER NOT NULL DEFAULT 0, isSitActive INTEGER NOT NULL DEFAULT 0, sitBaro REAL NOT NULL DEFAULT 0, sitTilt REAL NOT NULL DEFAULT 0, sitShock REAL NOT NULL DEFAULT 0, currentMa INTEGER NOT NULL DEFAULT 0, locationPendingReason TEXT NOT NULL DEFAULT 'NONE', accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0, isAnchorLocked INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO connection_history_new SELECT id, ts, rtt, isConnected, isGap, hasGps, isTick, ribbonKey, gpsIndex, noiseIdx, luxIdx, vibeIdx, proxIdx, liftIdx, snrIdx, tiltIdx, baroIdx, verticalVelocity, sitVz, sitVzTs, sitDz, isBatterySteepDischarge, remoteSig, isCoolingModeActive, speed, bearing, isSitDetected, isSitActive, sitBaro, sitTilt, sitShock, currentMa, locationPendingReason, accuracy, maxAccuracy, isAnchorLocked FROM connection_history")
                db.execSQL("DROP TABLE connection_history"); db.execSQL("ALTER TABLE connection_history_new RENAME TO connection_history")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_connection_history_ts ON connection_history (ts)")

                // pending_status_updates (Renamed lastValidFixRealtime -> lastValidFixRt)
                db.execSQL("CREATE TABLE pending_status_updates_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, speed REAL NOT NULL, accuracy REAL NOT NULL, bearing REAL NOT NULL, battery INTEGER NOT NULL, temp REAL NOT NULL, isCharging INTEGER NOT NULL, currentMa INTEGER NOT NULL DEFAULT 0, timestamp INTEGER NOT NULL, gpsTs INTEGER NOT NULL DEFAULT 0, satsView INTEGER NOT NULL, satsUsed INTEGER NOT NULL, name TEXT, maxAccuracy REAL NOT NULL, distToTracker REAL, distToHome REAL, snrIdx REAL NOT NULL DEFAULT 0, tiltIdx REAL NOT NULL DEFAULT 0, baroIdx REAL NOT NULL DEFAULT 0, isBatterySteepDischarge INTEGER NOT NULL DEFAULT 0, isCoolingModeActive INTEGER NOT NULL DEFAULT 0, isSitDetected INTEGER NOT NULL DEFAULT 0, isSitActive INTEGER NOT NULL DEFAULT 0, sitVz REAL NOT NULL DEFAULT 0, sitVzTs INTEGER NOT NULL DEFAULT 0, sitDz REAL NOT NULL DEFAULT 0, verticalVelocity REAL NOT NULL DEFAULT 0, sitBaro REAL NOT NULL DEFAULT 0, sitTilt REAL NOT NULL DEFAULT 0, sitShock REAL NOT NULL DEFAULT 0, isStorageLow INTEGER NOT NULL DEFAULT 0, isStorageCritical INTEGER NOT NULL DEFAULT 0, isPowerSaveMode INTEGER NOT NULL DEFAULT 0, standbyBucket INTEGER NOT NULL DEFAULT -1, netInterface TEXT NOT NULL DEFAULT 'UNKNOWN', lastValidFixRt INTEGER NOT NULL DEFAULT 0, locationPendingReason TEXT NOT NULL DEFAULT 'NONE', isAnchorLocked INTEGER NOT NULL DEFAULT 0, trackerState TEXT NOT NULL DEFAULT 'UNKNOWN')")
                db.execSQL("INSERT INTO pending_status_updates_new SELECT id, lat, lng, speed, accuracy, bearing, battery, temp, isCharging, currentMa, timestamp, gpsTs, satsView, satsUsed, name, maxAccuracy, distToTracker, distToHome, snrIdx, tiltIdx, baroIdx, isBatterySteepDischarge, isCoolingModeActive, isSitDetected, isSitActive, sitVz, sitVzTs, sitDz, verticalVelocity, sitBaro, sitTilt, sitShock, isStorageLow, isStorageCritical, isPowerSaveMode, standbyBucket, netInterface, lastValidFixRealtime, locationPendingReason, isAnchorLocked, trackerState FROM pending_status_updates")
                db.execSQL("DROP TABLE pending_status_updates"); db.execSQL("ALTER TABLE pending_status_updates_new RENAME TO pending_status_updates")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_status_updates_timestamp ON pending_status_updates (timestamp)")
            }
        }
        val MIGRATION_57_58 = object : Migration(57, 58) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Issue #105: Room identity hash mismatch resolution. 
                // All schema changes already handled in harmonized MIGRATION_56_57.
            }
        }
    }
}
