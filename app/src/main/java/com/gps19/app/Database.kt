package com.gps19.app

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Database: persistence configuration for GPS Tracker.
 * v8.9.5:
 * - Issue 192: Added currentMa to PendingStatusEntity and HistoryEntity for full forensic power parity.
 * v8.9.3:
 * - Issue 188: Added gpsTs to PendingStatusEntity to preserve historical fix accuracy.
 * v8.9.2:
 * - Issue 182: Synchronized source headers with v8.9.2 baseline.
 * v33 Migration Forensic Audit:
 * - Issue 135: Ensured 'verticalVelocity' is present in both connection_history and pending_status_updates.
 * - Standardized all forensic SIT fields across tables.
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
    val firstSeenTs: Long = 0L,
    val role: String = "tracker"
)

@Entity(tableName = "trail_points", indices = [Index(value = ["timestamp"])])
data class TrailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lng") val lng: Double,
    val timestamp: Long,
    val isViewerTrail: Boolean,
    val isJump: Boolean = false
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
    val ribbonKey: String, // "4M", "16M", "1H", "4H", "24H", "7D"
    val gpsIndex: Float = 0f,
    val noiseIdx: Float = 0f,
    val luxIdx: Float = 0f,
    val vibeIdx: Float = 0f,
    val proxIdx: Float = 1f, 
    val liftIdx: Float = 0f,
    val snrIdx: Float = 0f,
    val verticalVelocity: Float = 0f,
    val sitVz: Float = 0f, 
    val sitDz: Float = 0f,
    @ColumnInfo(name = "isBatterySteepDischarge")
    val isBatterySteepDischarge: Boolean = false,
    val remoteSig: Int = 10,
    val isCoolingModeActive: Boolean = false,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val sitBaro: Float = 0f,
    val sitTilt: Float = 0f,
    val sitShock: Float = 0f,
    val currentMa: Int = 0
)

@Entity(tableName = "violations", indices = [Index(value = ["ts"])])
data class ViolationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val type: String,
    val ts: Long
)

@Entity(tableName = "pending_status_updates", indices = [Index(value = ["timestamp"])])
data class PendingStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val speed: Float,
    val accuracy: Float,
    val bearing: Float,
    val battery: Int,
    val temp: Float,
    val isCharging: Boolean,
    val currentMa: Int = 0,
    val timestamp: Long,
    val gpsTs: Long = 0L,
    val satsView: Int,
    val satsUsed: Int,
    val maxAccuracy: Float,
    val distToTracker: Double? = null,
    val distToHome: Double? = null,
    val snrIdx: Float = 0f,
    @ColumnInfo(name = "isBatterySteepDischarge")
    val isBatterySteepDischarge: Boolean = false,
    val isCoolingModeActive: Boolean = false,
    val isSitDetected: Boolean = false,
    val isSitActive: Boolean = false,
    val sitVz: Float = 0f,
    val sitDz: Float = 0f,
    val verticalVelocity: Float = 0f,
    val sitBaro: Float = 0f,
    val sitTilt: Float = 0f,
    val sitShock: Float = 0f,
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val standbyBucket: Int = -1,
    val netInterface: String = "UNKNOWN"
)

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntity): Long
    @Update
    suspend fun update(log: LogEntity)
    @Query("SELECT * FROM logs WHERE localId = :localId")
    suspend fun getLogByLocalId(localId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLog(): LogEntity?
    @Query("SELECT * FROM logs WHERE type = :type AND role = :role AND deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLogByMetadata(type: String, role: String, deviceId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000")
    fun getAllLogs(): Flow<List<LogEntity>>
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000")
    suspend fun getAllLogsStatic(): List<LogEntity>
    @Query("DELETE FROM logs")
    suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getCount(): Int
    @Query("DELETE FROM logs WHERE timestamp < (SELECT timestamp FROM logs ORDER BY timestamp DESC LIMIT 1 OFFSET 999)")
    suspend fun pruneLogs()
}

@Dao
interface TrailDao {
    @Insert
    suspend fun insert(point: TrailEntity)
    @Query("SELECT * FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp ASC LIMIT 1000")
    fun getTrail(isViewer: Boolean): Flow<List<TrailEntity>>
    @Query("SELECT * FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp ASC LIMIT 1000")
    suspend fun getTrailStatic(isViewer: Boolean): List<TrailEntity>
    @Query("DELETE FROM trail_points WHERE isViewerTrail = :isViewer")
    suspend fun clearTrail(isViewer: Boolean)
    @Query("DELETE FROM trail_points WHERE isViewerTrail = :isViewer AND timestamp < (SELECT timestamp FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp DESC LIMIT 1 OFFSET 999)")
    suspend fun pruneTrail(isViewer: Boolean): Int
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(point: HistoryEntity)
    @Insert
    suspend fun insertAll(points: List<HistoryEntity>)
    @Query("SELECT * FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts ASC LIMIT 1000")
    fun getHistoryFlow(ribbonKey: String): Flow<List<HistoryEntity>>
    @Query("SELECT * FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts ASC LIMIT 1000")
    suspend fun getHistory(ribbonKey: String): List<HistoryEntity>
    @Query("DELETE FROM connection_history WHERE ribbonKey = :ribbonKey")
    suspend fun clearHistory(ribbonKey: String)
    @Query("DELETE FROM connection_history")
    suspend fun clearAll()
    @Query("DELETE FROM connection_history WHERE ribbonKey = :ribbonKey AND ts < (SELECT ts FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts DESC LIMIT 1 OFFSET 999)")
    suspend fun pruneHistory(ribbonKey: String)
}

@Dao
interface ViolationDao {
    @Insert
    suspend fun insert(violation: ViolationEntity)
    @Query("SELECT * FROM violations ORDER BY ts ASC LIMIT 1000")
    fun getAllFlow(): Flow<List<ViolationEntity>>
    @Query("SELECT * FROM violations ORDER BY ts ASC LIMIT 1000")
    suspend fun getAll(): List<ViolationEntity>
    @Query("DELETE FROM violations")
    suspend fun clearAll()
    @Query("DELETE FROM violations WHERE ts < (SELECT ts FROM violations ORDER BY ts DESC LIMIT 1 OFFSET 999)")
    suspend fun prune()
}

@Dao
interface PendingStatusDao {
    @Insert
    suspend fun insert(status: PendingStatusEntity)
    @Query("SELECT * FROM pending_status_updates ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getOldestPending(limit: Int): List<PendingStatusEntity>
    @Query("DELETE FROM pending_status_updates WHERE id IN (:ids)")
    suspend fun deletePending(ids: List<Long>)
    @Query("SELECT COUNT(*) FROM pending_status_updates")
    suspend fun getCount(): Int
    @Query("DELETE FROM pending_status_updates WHERE timestamp < (SELECT timestamp FROM pending_status_updates ORDER BY timestamp DESC LIMIT 1 OFFSET 999)")
    suspend fun prune()
    @Query("DELETE FROM pending_status_updates")
    suspend fun clearAll()
}

@Database(entities = [LogEntity::class, TrailEntity::class, HistoryEntity::class, ViolationEntity::class, PendingStatusEntity::class], version = 35, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun trailDao(): TrailDao
    abstract fun historyDao(): HistoryDao
    abstract fun violationDao(): ViolationDao
    abstract fun pendingStatusDao(): PendingStatusDao

    companion object {
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN firstSeenTs INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN snrIdx REAL NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN snrIdx REAL NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trail_points ADD COLUMN isJump INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN sitVz REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN sitDz REAL NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN isBatterySteepDrop INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN remoteSig INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isBatterySteepDrop INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN isCoolingModeActive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isCoolingModeActive INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history RENAME COLUMN isBatterySteepDrop TO isBatterySteepDischarge")
                db.execSQL("ALTER TABLE pending_status_updates RENAME COLUMN isBatterySteepDrop TO isBatterySteepDischarge")
            }
        }
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN speed REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN bearing REAL NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN isSitDetected INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN isSitActive INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isSitDetected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isSitActive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN sitVz REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN sitDz REAL NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN role TEXT NOT NULL DEFAULT 'tracker'")
            }
        }
        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // vid column added to entities in v30
                db.execSQL("ALTER TABLE connection_history ADD COLUMN vid TEXT")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN vid TEXT")
                db.execSQL("ALTER TABLE logs ADD COLUMN vid TEXT")
            }
        }
        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Issue 139/142: Added forensic fields, vid is now legacy/unused but remains in schema
                db.execSQL("ALTER TABLE connection_history ADD COLUMN sitBaro REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN sitTilt REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN sitShock REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN ver TEXT")

                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN sitBaro REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN sitTilt REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN sitShock REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isStorageLow INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isStorageCritical INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isPowerSaveMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN standbyBucket INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN netInterface TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN ver TEXT")
            }
        }
        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trail_points ADD COLUMN ver TEXT")
                db.execSQL("ALTER TABLE violations ADD COLUMN ver TEXT")
            }
        }
        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // logs: remove 'vid'
                db.execSQL("CREATE TABLE logs_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, localId TEXT NOT NULL, timestamp INTEGER NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, isImportant INTEGER NOT NULL, deviceId TEXT NOT NULL, viewerId TEXT NOT NULL, count INTEGER NOT NULL, extremeValue REAL, durationMs INTEGER NOT NULL, isSpecial INTEGER NOT NULL, specialColor INTEGER, firstSeenTs INTEGER NOT NULL, role TEXT NOT NULL)")
                db.execSQL("INSERT INTO logs_new (id, localId, timestamp, message, type, isImportant, deviceId, viewerId, count, extremeValue, durationMs, isSpecial, specialColor, firstSeenTs, role) SELECT id, localId, timestamp, message, type, isImportant, deviceId, viewerId, count, extremeValue, durationMs, isSpecial, specialColor, firstSeenTs, role FROM logs")
                db.execSQL("DROP TABLE logs")
                db.execSQL("ALTER TABLE logs_new RENAME TO logs")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_timestamp ON logs (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_localId ON logs (localId)")

                // trail_points: remove 'ver'
                db.execSQL("CREATE TABLE trail_points_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, timestamp INTEGER NOT NULL, isViewerTrail INTEGER NOT NULL, isJump INTEGER NOT NULL)")
                db.execSQL("INSERT INTO trail_points_new (id, lat, lng, timestamp, isViewerTrail, isJump) SELECT id, lat, lng, timestamp, isViewerTrail, isJump FROM trail_points")
                db.execSQL("DROP TABLE trail_points")
                db.execSQL("ALTER TABLE trail_points_new RENAME TO trail_points")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trail_points_timestamp ON trail_points (timestamp)")

                // connection_history: remove 'vid', 'ver', add 'verticalVelocity'
                db.execSQL("CREATE TABLE connection_history_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ts INTEGER NOT NULL, rtt INTEGER NOT NULL, isConnected INTEGER NOT NULL, isGap INTEGER NOT NULL, hasGps INTEGER NOT NULL, isTick INTEGER NOT NULL, ribbonKey TEXT NOT NULL, gpsIndex REAL NOT NULL, noiseIdx REAL NOT NULL, luxIdx REAL NOT NULL, vibeIdx REAL NOT NULL, proxIdx REAL NOT NULL, liftIdx REAL NOT NULL, snrIdx REAL NOT NULL, verticalVelocity REAL NOT NULL, sitVz REAL NOT NULL, sitDz REAL NOT NULL, isBatterySteepDischarge INTEGER NOT NULL, remoteSig INTEGER NOT NULL, isCoolingModeActive INTEGER NOT NULL, speed REAL NOT NULL, bearing REAL NOT NULL, isSitDetected INTEGER NOT NULL, isSitActive INTEGER NOT NULL, sitBaro REAL NOT NULL, sitTilt REAL NOT NULL, sitShock REAL NOT NULL)")
                db.execSQL("INSERT INTO connection_history_new (id, ts, rtt, isConnected, isGap, hasGps, isTick, ribbonKey, gpsIndex, noiseIdx, luxIdx, vibeIdx, proxIdx, liftIdx, snrIdx, verticalVelocity, sitVz, sitDz, isBatterySteepDischarge, remoteSig, isCoolingModeActive, speed, bearing, isSitDetected, isSitActive, sitBaro, sitTilt, sitShock) SELECT id, ts, rtt, isConnected, isGap, hasGps, isTick, ribbonKey, gpsIndex, noiseIdx, luxIdx, vibeIdx, proxIdx, liftIdx, snrIdx, 0 as verticalVelocity, sitVz, sitDz, isBatterySteepDischarge, remoteSig, isCoolingModeActive, speed, bearing, isSitDetected, isSitActive, sitBaro, sitTilt, sitShock FROM connection_history")
                db.execSQL("DROP TABLE connection_history")
                db.execSQL("ALTER TABLE connection_history_new RENAME TO connection_history")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_connection_history_ts ON connection_history (ts)")

                // violations: remove 'ver'
                db.execSQL("CREATE TABLE violations_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, type TEXT NOT NULL, ts INTEGER NOT NULL)")
                db.execSQL("INSERT INTO violations_new (id, lat, lng, type, ts) SELECT id, lat, lng, type, ts FROM violations")
                db.execSQL("DROP TABLE violations")
                db.execSQL("ALTER TABLE violations_new RENAME TO violations")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_violations_ts ON violations (ts)")

                // pending_status_updates: remove 'vid', 'ver'
                db.execSQL("CREATE TABLE pending_status_updates_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, speed REAL NOT NULL, accuracy REAL NOT NULL, bearing REAL NOT NULL, battery INTEGER NOT NULL, temp REAL NOT NULL, isCharging INTEGER NOT NULL, timestamp INTEGER NOT NULL, satsView INTEGER NOT NULL, satsUsed INTEGER NOT NULL, maxAccuracy REAL NOT NULL, distToTracker REAL, distToHome REAL, snrIdx REAL NOT NULL, isBatterySteepDischarge INTEGER NOT NULL, isCoolingModeActive INTEGER NOT NULL, isSitDetected INTEGER NOT NULL, isSitActive INTEGER NOT NULL, sitVz REAL NOT NULL, sitDz REAL NOT NULL, verticalVelocity REAL NOT NULL, sitBaro REAL NOT NULL, sitTilt REAL NOT NULL, sitShock REAL NOT NULL, isStorageLow INTEGER NOT NULL, isStorageCritical INTEGER NOT NULL, isPowerSaveMode INTEGER NOT NULL, standbyBucket INTEGER NOT NULL, netInterface TEXT NOT NULL)")
                db.execSQL("INSERT INTO pending_status_updates_new (id, lat, lng, speed, accuracy, bearing, battery, temp, isCharging, timestamp, satsView, satsUsed, maxAccuracy, distToTracker, distToHome, snrIdx, isBatterySteepDischarge, isCoolingModeActive, isSitDetected, isSitActive, sitVz, sitDz, verticalVelocity, sitBaro, sitTilt, sitShock, isStorageLow, isStorageCritical, isPowerSaveMode, standbyBucket, netInterface) SELECT id, lat, lng, speed, accuracy, bearing, battery, temp, isCharging, timestamp, satsView, satsUsed, maxAccuracy, distToTracker, distToHome, snrIdx, isBatterySteepDischarge, isCoolingModeActive, isSitDetected, isSitActive, sitVz, sitDz, 0 as verticalVelocity, sitBaro, sitTilt, sitShock, isStorageLow, isStorageCritical, isPowerSaveMode, standbyBucket, netInterface FROM pending_status_updates")
                db.execSQL("DROP TABLE pending_status_updates")
                db.execSQL("ALTER TABLE pending_status_updates_new RENAME TO pending_status_updates")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_status_updates_timestamp ON pending_status_updates (timestamp)")
            }
        }
        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN gpsTs INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN currentMa INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN currentMa INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
