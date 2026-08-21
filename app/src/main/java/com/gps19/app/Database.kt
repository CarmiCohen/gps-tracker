package com.gps19.app

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import com.gps19.core.engine.*

/**
 * Database: persistence configuration for GPS Tracker.
 * Aug.21.05:
 * - Issue #196 Hardening: Added getExistingForensicSignaturesInRange to LogDao 
 *   to support optimized range-based deduplication during 100Hz forensic 
 *   bursts on budget hardware (R197).
 * Aug.20.06:
 * - Issue #224 Forensic Audit: Bumped to v73. Added MIGRATION_72_73 to include 
 *   sitVzRt in pending_status_updates for vertical velocity parity (R224).
 * Aug.18.01:
 * - Issue #197: Forensic Storage-Aware Adaptive Pruning. Added LogDao methods for 
 *   forensic-specific chunked pruning to handle 100Hz trace accumulation (R197).
 */
@Entity(
    tableName = "logs", 
    indices = [
        Index(value = ["timestamp"]), 
        Index(value = ["localId"]),
        Index(value = ["isImportant"]),
        Index(value = ["isSpecial"]),
        Index(value = ["synced", "timestamp"]), 
        Index(value = ["type", "role", "deviceId", "timestamp"]), 
        Index(value = ["type", "timestamp", "spillIdx"]),
        Index(value = ["type", "timestamp"]), 
        Index(value = ["isImportant", "isSpecial", "timestamp"]) 
    ]
)
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
    val vibeSnapshot: Double? = null,
    @ColumnInfo(defaultValue = "-1") val spillIdx: Int = -1,
    @ColumnInfo(defaultValue = "0") val gpsHardwareLock: Boolean = false,
    val tempSnapshot: Double? = null,
    val battSnapshot: Int? = null,
    val chargingSnapshot: Boolean? = null
)

data class ForensicSignature(
    val timestamp: Long,
    val spillIdx: Int
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
    @ColumnInfo(defaultValue = "0") val rt: Long = 0L,
    val rtt: Int,
    val isConnected: Boolean,
    val isGap: Boolean,
    @ColumnInfo(defaultValue = "0") val isRecoveryEvent: Boolean = false,
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
    @ColumnInfo(defaultValue = "0") val sitVzRt: Long = 0L,
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
    @ColumnInfo(defaultValue = "0") val isAnchorLocked: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isBatteryLow: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isBatteryCritical: Boolean = false
)

@Entity(tableName = "violations", indices = [Index(value = ["ts"])])
data class ViolationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double, val lng: Double, val type: String, val ts: Long,
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
    @ColumnInfo(defaultValue = "0") val noiseIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val luxIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val vibeIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "1") val proxIdx: Double = 1.0,
    @ColumnInfo(defaultValue = "0") val liftIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val tiltIdx: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val baroIdx: Double = 0.0,
    @ColumnInfo(name = "isBatterySteepDischarge", defaultValue = "0") val isBatterySteepDischarge: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isCoolingModeActive: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isSitDetected: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isSitActive: Boolean = false,
    @ColumnInfo(defaultValue = "0") val sitVz: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sitVzTs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val sitVzRt: Long = 0L,
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
    @ColumnInfo(defaultValue = "VALID") val status: String = "VALID",
    @ColumnInfo(defaultValue = "0") val isBatteryLow: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isBatteryCritical: Boolean = false
)

@Dao
abstract class LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insert(log: LogEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertAll(logs: List<LogEntity>)
    @Update abstract suspend fun update(log: LogEntity)
    @Query("SELECT * FROM logs WHERE localId = :localId") abstract suspend fun getLogByLocalId(localId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1") abstract suspend fun getLastLog(): LogEntity?
    @Query("SELECT * FROM logs WHERE type = :type AND role = :role AND deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1") abstract suspend fun getLastLogByMetadata(type: String, role: String, deviceId: String): LogEntity?
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit") abstract fun getAllLogs(limit: Int): Flow<List<LogEntity>>
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit") abstract suspend fun getAllLogsStatic(limit: Int): List<LogEntity>
    @Query("SELECT * FROM logs WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit") abstract suspend fun getUnsyncedLogs(limit: Int): List<LogEntity>
    @Query("UPDATE logs SET synced = 1 WHERE localId IN (:localIds)") abstract suspend fun markLogsAsSynced(localIds: List<String>)
    @Query("DELETE FROM logs") abstract suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM logs") abstract suspend fun getCount(): Int
    
    @Query("SELECT timestamp, spillIdx FROM logs WHERE type = 'FORENSIC_TRACE' AND timestamp >= :minTimestamp") 
    abstract suspend fun getExistingForensicSignatures(minTimestamp: Long): List<ForensicSignature>

    @Query("SELECT timestamp, spillIdx FROM logs WHERE type = 'FORENSIC_TRACE' AND timestamp >= :minTs AND timestamp <= :maxTs") 
    abstract suspend fun getExistingForensicSignaturesInRange(minTs: Long, maxTs: Long): List<ForensicSignature>

    // Optimized Pruning Support (R177, R197)
    @Query("SELECT timestamp FROM logs WHERE type IN ('watchdog_stats', 'viewer_pulse', 'tracker_pulse', 'pong_activity') ORDER BY timestamp DESC LIMIT 1 OFFSET :limit")
    abstract suspend fun getHeartbeatPruneThreshold(limit: Int): Long?

    @Query("SELECT timestamp FROM logs WHERE isImportant = 0 AND isSpecial = 0 AND type NOT IN ('watchdog_stats', 'viewer_pulse', 'tracker_pulse', 'pong_activity', 'FORENSIC_TRACE') ORDER BY timestamp DESC LIMIT 1 OFFSET :limit")
    abstract suspend fun getGeneralPruneThreshold(limit: Int): Long?

    @Query("SELECT timestamp FROM logs WHERE isImportant = 1 AND isSpecial = 0 AND type != 'FORENSIC_TRACE' ORDER BY timestamp DESC LIMIT 1 OFFSET :limit")
    abstract suspend fun getImportantPruneThreshold(limit: Int): Long?

    @Query("SELECT timestamp FROM logs WHERE isSpecial = 1 AND type != 'FORENSIC_TRACE' ORDER BY timestamp DESC LIMIT 1 OFFSET :limit")
    abstract suspend fun getSpecialPruneThreshold(limit: Int): Long?

    @Query("SELECT timestamp FROM logs WHERE type = 'FORENSIC_TRACE' ORDER BY timestamp DESC LIMIT 1 OFFSET :limit")
    abstract suspend fun getForensicPruneThreshold(limit: Int): Long?

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs WHERE type IN ('watchdog_stats', 'viewer_pulse', 'tracker_pulse', 'pong_activity') AND timestamp < :threshold LIMIT :chunkSize)")
    abstract suspend fun pruneHeartbeatsByThreshold(threshold: Long, chunkSize: Int): Int

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs WHERE isImportant = 0 AND isSpecial = 0 AND type NOT IN ('watchdog_stats', 'viewer_pulse', 'tracker_pulse', 'pong_activity', 'FORENSIC_TRACE') AND timestamp < :threshold LIMIT :chunkSize)")
    abstract suspend fun pruneGeneralByThreshold(threshold: Long, chunkSize: Int): Int

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs WHERE isImportant = 1 AND isSpecial = 0 AND type != 'FORENSIC_TRACE' AND timestamp < :threshold LIMIT :chunkSize)")
    abstract suspend fun pruneImportantByThreshold(threshold: Long, chunkSize: Int): Int

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs WHERE isSpecial = 1 AND type != 'FORENSIC_TRACE' AND timestamp < :threshold LIMIT :chunkSize)")
    abstract suspend fun pruneSpecialByThreshold(threshold: Long, chunkSize: Int): Int

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs WHERE type = 'FORENSIC_TRACE' AND timestamp < :threshold LIMIT :chunkSize)")
    abstract suspend fun pruneForensicByThreshold(threshold: Long, chunkSize: Int): Int
}

@Dao
interface TrailDao {
    @Insert suspend fun insert(point: TrailEntity)
    @Query("SELECT * FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp ASC LIMIT 2000") fun getTrail(isViewer: Boolean): Flow<List<TrailEntity>>
    @Query("SELECT * FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp ASC LIMIT 2000") suspend fun getTrailStatic(isViewer: Boolean): List<TrailEntity>
    @Query("DELETE FROM trail_points WHERE isViewerTrail = :isViewer") suspend fun clearTrail(isViewer: Boolean)
    @Query("DELETE FROM trail_points WHERE isViewerTrail = :isViewer AND timestamp < (SELECT timestamp FROM trail_points WHERE isViewerTrail = :isViewer ORDER BY timestamp DESC LIMIT 1 OFFSET 1999)") suspend fun pruneTrail(isViewer: Boolean): Int
}

@Dao
interface HistoryDao {
    @Insert suspend fun insert(point: HistoryEntity)
    @Insert abstract suspend fun insertAll(points: List<HistoryEntity>)
    @Query("SELECT * FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts ASC LIMIT 300") fun getHistoryFlow(ribbonKey: String): Flow<List<HistoryEntity>>
    @Query("SELECT * FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts ASC LIMIT 300") suspend fun getHistory(ribbonKey: String): List<HistoryEntity>
    @Query("DELETE FROM connection_history WHERE ribbonKey = :ribbonKey") suspend fun clearHistory(ribbonKey: String)
    @Query("DELETE FROM connection_history") suspend fun clearAll()
    @Query("DELETE FROM connection_history WHERE ribbonKey = :ribbonKey AND ts < (SELECT ts FROM connection_history WHERE ribbonKey = :ribbonKey ORDER BY ts DESC LIMIT 1 OFFSET 299)") suspend fun pruneHistory(ribbonKey: String)
}

@Dao
interface ViolationDao {
    @Insert suspend fun insert(violation: ViolationEntity)
    @Query("SELECT * FROM violations ORDER BY ts ASC LIMIT 2000") fun getAllFlow(): Flow<List<ViolationEntity>>
    @Query("SELECT * FROM violations ORDER BY ts ASC LIMIT 2000") suspend fun getAll(): List<ViolationEntity>
    @Query("DELETE FROM violations") suspend fun clearAll()
    @Query("DELETE FROM violations WHERE ts < (SELECT ts FROM violations ORDER BY ts DESC LIMIT 1 OFFSET 1999)") suspend fun prune()
}

@Dao
interface PendingStatusDao {
    @Insert suspend fun insert(status: PendingStatusEntity)
    @Query("SELECT * FROM pending_status_updates ORDER BY timestamp ASC LIMIT :limit") suspend fun getOldestPending(limit: Int): List<PendingStatusEntity>
    @Query("DELETE FROM pending_status_updates WHERE id IN (:ids)") suspend fun deletePending(ids: LongArray)
    @Query("SELECT COUNT(*) FROM pending_status_updates") suspend fun getCount(): Int
    @Query("DELETE FROM pending_status_updates WHERE timestamp < (SELECT timestamp FROM pending_status_updates ORDER BY timestamp DESC LIMIT 1 OFFSET 1999)") suspend fun prune()
    @Query("DELETE FROM pending_status_updates") suspend fun clearAll()
}

@Database(entities = [LogEntity::class, TrailEntity::class, HistoryEntity::class, ViolationEntity::class, PendingStatusEntity::class], version = 73, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun trailDao(): TrailDao
    abstract fun historyDao(): HistoryDao
    abstract fun violationDao(): ViolationDao
    abstract fun pendingStatusDao(): PendingStatusDao

    fun checkIntegrity(): String {
        val cursor = query("PRAGMA integrity_check", null)
        return try {
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                "UNKNOWN"
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        } finally {
            cursor.close()
        }
    }

    companion object {
        val MIGRATION_72_73 = object : Migration(72, 73) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // R224: Synchronizing sitVzRt in pending_status_updates for forensic parity
                try {
                    db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN sitVzRt INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_71_72 = object : Migration(71, 72) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // R195: Hardened recovery for connection_history schema mismatch
                try {
                    db.execSQL("ALTER TABLE connection_history ADD COLUMN sitVzRt INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist if migration 70_71 partially succeeded
                }
                
                // Force non-unique indices to clear any persistent legacy UNIQUE constraints
                db.execSQL("DROP INDEX IF EXISTS index_logs_type_timestamp_spillIdx")
                db.execSQL("CREATE INDEX index_logs_type_timestamp_spillIdx ON logs (type, timestamp, spillIdx)")
                db.execSQL("DROP INDEX IF EXISTS index_logs_localId")
                db.execSQL("CREATE INDEX index_logs_localId ON logs (localId)")
            }
        }

        val MIGRATION_70_71 = object : Migration(70, 71) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // R190e: Correct schema mismatch in connection_history
                try {
                    db.execSQL("ALTER TABLE connection_history ADD COLUMN sitVzRt INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}

                // R190: Force non-unique indices to resolve legacy duplication conflicts
                db.execSQL("DROP INDEX IF EXISTS index_logs_type_timestamp_spillIdx")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_type_timestamp_spillIdx ON logs (type, timestamp, spillIdx)")
                db.execSQL("DROP INDEX IF EXISTS index_logs_localId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_localId ON logs (localId)")
            }
        }

        val MIGRATION_69_70 = object : Migration(69, 70) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // R190c: Purge duplicates to prevent UNIQUE localId constraint violation
                db.execSQL("DELETE FROM logs WHERE id NOT IN (SELECT MIN(id) FROM logs GROUP BY localId)")
                
                db.execSQL("DROP INDEX IF EXISTS index_logs_type_timestamp_spillIdx")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_type_timestamp_spillIdx ON logs (type, timestamp, spillIdx)")
                db.execSQL("DROP INDEX IF EXISTS index_logs_localId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_localId ON logs (localId)")
            }
        }

        val MIGRATION_68_69 = object : Migration(68, 69) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // R190c: Purge duplicates for high-frequency telemetry keys
                db.execSQL("DELETE FROM logs WHERE id NOT IN (SELECT MIN(id) FROM logs GROUP BY type, timestamp, spillIdx)")

                db.execSQL("DROP INDEX IF EXISTS index_logs_type_spillIdx_timestamp")
                db.execSQL("DROP INDEX IF EXISTS index_logs_type_timestamp_spillIdx")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_type_timestamp_spillIdx ON logs (type, timestamp, spillIdx)")
            }
        }

        val MIGRATION_67_68 = object : Migration(67, 68) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_type_timestamp ON logs (type, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_isImportant_isSpecial_timestamp ON logs (isImportant, isSpecial, timestamp)")
            }
        }

        val MIGRATION_66_67 = object : Migration(66, 67) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN tempSnapshot REAL")
                db.execSQL("ALTER TABLE logs ADD COLUMN battSnapshot INTEGER")
                db.execSQL("ALTER TABLE logs ADD COLUMN chargingSnapshot INTEGER")
            }
        }

        val MIGRATION_65_66 = object : Migration(65, 66) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN isBatteryLow INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE connection_history ADD COLUMN isBatteryCritical INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isBatteryLow INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN isBatteryCritical INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_64_65 = object : Migration(64, 65) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN gpsHardwareLock INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_63_64 = object : Migration(63, 64) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN spillIdx INTEGER NOT NULL DEFAULT -1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_type_spillIdx_timestamp ON logs (type, spillIdx, timestamp)")
            }
        }

        val MIGRATION_62_63 = object : Migration(62, 63) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN isRecoveryEvent INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_61_62 = object : Migration(61, 62) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_isImportant ON logs (isImportant)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_isSpecial ON logs (isSpecial)")
                db.execSQL("DROP INDEX IF EXISTS index_logs_synced")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_synced_timestamp ON logs (synced, timestamp)")
            }
        }

        val MIGRATION_60_61 = object : Migration(60, 61) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_synced ON logs (synced)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_type_role_deviceId_timestamp ON logs (type, role, deviceId, timestamp)")
            }
        }

        val MIGRATION_59_60 = object : Migration(59, 60) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE connection_history ADD COLUMN rt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_56_57 = object : Migration(56, 57) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE logs_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, localId TEXT NOT NULL, timestamp INTEGER NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, isImportant INTEGER NOT NULL, deviceId TEXT NOT NULL, viewerId TEXT NOT NULL, count INTEGER NOT NULL, extremeValue REAL, durationMs INTEGER NOT NULL, isSpecial INTEGER NOT NULL, specialColor INTEGER, firstSeenTs INTEGER NOT NULL DEFAULT 0, role TEXT NOT NULL DEFAULT 'tracker', synced INTEGER NOT NULL DEFAULT 0, lat REAL NOT NULL DEFAULT 0, lng REAL NOT NULL DEFAULT 0, accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0, snrSnapshot REAL, vibeSnapshot REAL)")
                db.execSQL("INSERT INTO logs_new SELECT id, localId, timestamp, message, type, isImportant, deviceId, viewerId, count, extremeValue, durationMs, isSpecial, specialColor, firstSeenTs, role, synced, lat, lng, accuracy, maxAccuracy, snrSnapshot, vibeSnapshot FROM logs")
                db.execSQL("DROP TABLE logs"); db.execSQL("ALTER TABLE logs_new RENAME TO logs")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_timestamp ON logs (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_localId ON logs (localId)")

                db.execSQL("CREATE TABLE trail_points_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, timestamp INTEGER NOT NULL, isViewerTrail INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'VALID', accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO trail_points_new (id, lat, lng, timestamp, isViewerTrail, status, accuracy, maxAccuracy) SELECT id, lat, lng, timestamp, isViewerTrail, (CASE WHEN isJump = 1 THEN 'JUMP' ELSE 'VALID' END), accuracy, maxAccuracy FROM trail_points")
                db.execSQL("DROP TABLE trail_points"); db.execSQL("ALTER TABLE trail_points_new RENAME TO trail_points")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trail_points_timestamp ON trail_points (timestamp)")

                db.execSQL("CREATE TABLE violations_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, type TEXT NOT NULL, ts INTEGER NOT NULL, accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO violations_new SELECT id, lat, lng, type, ts, accuracy, maxAccuracy FROM violations")
                db.execSQL("DROP TABLE violations"); db.execSQL("ALTER TABLE violations_new RENAME TO violations")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_violations_ts ON violations (ts)")

                db.execSQL("CREATE TABLE connection_history_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ts INTEGER NOT NULL, rtt INTEGER NOT NULL, isConnected INTEGER NOT NULL, isGap INTEGER NOT NULL, hasGps INTEGER NOT NULL, isTick INTEGER NOT NULL, ribbonKey TEXT NOT NULL, gpsIndex REAL NOT NULL DEFAULT 0, noiseIdx REAL NOT NULL DEFAULT 0, luxIdx REAL NOT NULL DEFAULT 0, vibeIdx REAL NOT NULL DEFAULT 0, proxIdx REAL NOT NULL DEFAULT 1, liftIdx REAL NOT NULL DEFAULT 0, snrIdx REAL NOT NULL DEFAULT 0, tiltIdx REAL NOT NULL DEFAULT 0, baroIdx REAL NOT NULL DEFAULT 0, verticalVelocity REAL NOT NULL DEFAULT 0, sitVz REAL NOT NULL DEFAULT 0, sitVzTs INTEGER NOT NULL DEFAULT 0, sitDz REAL NOT NULL DEFAULT 0, isBatterySteepDischarge INTEGER NOT NULL DEFAULT 0, remoteSig INTEGER NOT NULL DEFAULT 10, isCoolingModeActive INTEGER NOT NULL DEFAULT 0, speed REAL NOT NULL DEFAULT 0, bearing REAL NOT NULL DEFAULT 0, isSitDetected INTEGER NOT NULL DEFAULT 0, isSitActive INTEGER NOT NULL DEFAULT 0, sitBaro REAL NOT NULL DEFAULT 0, sitTilt REAL NOT NULL DEFAULT 0, sitShock REAL NOT NULL DEFAULT 0, currentMa INTEGER NOT NULL DEFAULT 0, locationPendingReason TEXT NOT NULL DEFAULT 'NONE', accuracy REAL NOT NULL DEFAULT 0, maxAccuracy REAL NOT NULL DEFAULT 0, isAnchorLocked INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO connection_history_new SELECT id, ts, rtt, isConnected, isGap, hasGps, isTick, ribbonKey, gpsIndex, noiseIdx, luxIdx, vibeIdx, proxIdx, liftIdx, snrIdx, tiltIdx, baroIdx, verticalVelocity, sitVz, sitVzTs, sitDz, isBatterySteepDischarge, remoteSig, isCoolingModeActive, speed, bearing, isSitDetected, isSitActive, sitBaro, sitTilt, sitShock, currentMa, locationPendingReason, accuracy, maxAccuracy, isAnchorLocked FROM connection_history")
                db.execSQL("DROP TABLE connection_history"); db.execSQL("ALTER TABLE connection_history_new RENAME TO connection_history")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_connection_history_ts ON connection_history (ts)")

                db.execSQL("CREATE TABLE pending_status_updates_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, speed REAL NOT NULL, accuracy REAL NOT NULL, bearing REAL NOT NULL, battery INTEGER NOT NULL, temp REAL NOT NULL, isCharging INTEGER NOT NULL, currentMa INTEGER NOT NULL DEFAULT 0, timestamp INTEGER NOT NULL, gpsTs INTEGER NOT NULL DEFAULT 0, satsView INTEGER NOT NULL, satsUsed INTEGER NOT NULL, name TEXT, maxAccuracy REAL NOT NULL, distToTracker REAL, distToHome REAL, snrIdx REAL NOT NULL DEFAULT 0, tiltIdx REAL NOT NULL DEFAULT 0, baroIdx REAL NOT NULL DEFAULT 0, isBatterySteepDischarge INTEGER NOT NULL DEFAULT 0, isCoolingModeActive INTEGER NOT NULL DEFAULT 0, isXiaomiAutostartGranted INTEGER NOT NULL DEFAULT 0, isSitDetected INTEGER NOT NULL DEFAULT 0, isSitActive INTEGER NOT NULL DEFAULT 0, sitVz REAL NOT NULL DEFAULT 0, sitVzTs INTEGER NOT NULL DEFAULT 0, sitDz REAL NOT NULL DEFAULT 0, verticalVelocity REAL NOT NULL DEFAULT 0, sitBaro REAL NOT NULL DEFAULT 0, sitTilt REAL NOT NULL DEFAULT 0, sitShock REAL NOT NULL DEFAULT 0, isStorageLow INTEGER NOT NULL DEFAULT 0, isStorageCritical INTEGER NOT NULL DEFAULT 0, isPowerSaveMode INTEGER NOT NULL DEFAULT 0, standbyBucket INTEGER NOT NULL DEFAULT -1, netInterface TEXT NOT NULL DEFAULT 'UNKNOWN', lastValidFixRt INTEGER NOT NULL DEFAULT 0, locationPendingReason TEXT NOT NULL DEFAULT 'NONE', isAnchorLocked INTEGER NOT NULL DEFAULT 0, trackerState TEXT NOT NULL DEFAULT 'UNKNOWN')")
                db.execSQL("INSERT INTO pending_status_updates_new SELECT id, lat, lng, speed, accuracy, bearing, battery, temp, isCharging, currentMa, timestamp, gpsTs, satsView, satsUsed, name, maxAccuracy, distToTracker, distToHome, snrIdx, tiltIdx, baroIdx, isBatterySteepDischarge, isCoolingModeActive, isXiaomiAutostartGranted, isSitDetected, isSitActive, sitVz, sitVzTs, sitDz, verticalVelocity, sitBaro, sitTilt, sitShock, isStorageLow, isStorageCritical, isPowerSaveMode, standbyBucket, netInterface, lastValidFixRealtime, locationPendingReason, isAnchorLocked, trackerState FROM pending_status_updates")
                db.execSQL("DROP TABLE pending_status_updates"); db.execSQL("ALTER TABLE pending_status_updates_new RENAME TO pending_status_updates")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_status_updates_timestamp ON pending_status_updates (timestamp)")
            }
        }
        val MIGRATION_57_58 = object : Migration(57, 58) {
            override fun migrate(db: SupportSQLiteDatabase) { }
        }
        val MIGRATION_58_59 = object : Migration(58, 59) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN noiseIdx REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN luxIdx REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN vibeIdx REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN liftIdx REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_status_updates ADD COLUMN proxIdx REAL NOT NULL DEFAULT 1")
            }
        }
    }
}
