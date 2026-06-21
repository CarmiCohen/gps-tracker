# Data Storage & Files Mechanism (v8.9.10)

This document describes the multi-tier storage architecture used in the GPS Tracker project: reactive preferences for configuration, a relational database for high-frequency telemetry, and external JSON files for archival.

## 1. Configuration & Preferences (`DataStore`)
The app uses **Jetpack DataStore (Preferences)** for atomic, thread-safe configuration storage.
- **Managed by**: `SettingsRepository.kt` and `MainRepository.kt`.
- **Key Data**: Identity (IDs), Infrastructure (Relay URL), Security Toggles (`alertSettings`), and Geofencing (Home Points serialized as JSON).
- **Draft Mechanism**: Supports uncommitted draft states for UI settings to ensure no configuration loss during rapid edits.

## 2. Relational Persistence (`Room / SQLite`)
For high-frequency forensic and historical data, the app uses a **Room Database** (`AppDatabase.kt`).

### A. Event Logs (`logs` table)
- **Forensic Aggregation**: Merges consecutive events with dynamic parts into a single entry.
- **Forensic Metrics**: Tracks `count` (occurrences) and `durationMs` (total active time).
- **Role Identity**: Every log entry includes a mandatory `role` field.
- **Log Spatial Anchor (v8.9.10)**: Every log entry includes `lat` and `lng` fields to support historical marker reconstruction on the map.
- **Capacity**: Sliding window of the last 1000 entries. Threshold-based pruning.
- **Migration (v39)**: Added `lat`/`lng` columns to the `logs` table (Issue 208).

### B. Analytical History (`history` table)
- **Ribbon Streams**: Stores connection and sensor indices for 4M, 16M, 1H, 4H, 24H, and 7D views.
- **Batch Processing**: History points are buffered and flushed in batches every 5,000ms (`HISTORY_BATCH_WRITE_INTERVAL_MS`) or upon reaching 100 entries (`HISTORY_BUFFER_MAX_SIZE`).
- **Power Forensic Parity (Issue 192)**: Includes `currentMa` (battery current) for end-to-end power visibility (DB v35).
- **SIT Metrics (Issue 197)**: Includes `sitVzTs` for improved chair event reconstruction (DB v38).
- **Pruning (PERF_IO)**: Pruning is triggered globally by `MainRepository` after every 50 writes (`DB_PRUNE_THRESHOLD`).

### C. Map Overlays
- **Trail Points**: Historical coordinates for Tracker and Viewer roles. Limited to 1000 points per role.
- **Violations**: Range, Jump, and Jammer violations with precise timestamps. Limited to 1000 entries.
- **SIT Markers (Issue 194)**: Reconstructed from acknowledged logs to ensure zero-loss forensic parity. Now geographically anchored (v8.9.10).

### D. Offline Queuing (`pending_status` table)
- **Persistence**: If the relay is disconnected, the system queues status updates locally. Includes `gpsTs` preservation (DB v34) and `currentMa` parity (DB v35).

## 3. Storage Integrity Watchdog
The `IntegrityMonitor` implements a dual-tier protection system based on `StatFs` metrics:
- **Tier 1: Critical (< 10MB `SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB`)**: Absolute muzzle. All non-essential logging is halted.
- **Tier 2: Low (< 50MB `SYSTEM_STORAGE_LOW_THRESHOLD_MB`)**: Throttled logging. Only "Important" forensic logs are allowed.

## 4. Maintenance & Archival
- **Daily Cleanup**: Scheduled task at 02:05 AM to optimize database performance.
- **Daily Archiving**: Scheduled task at 03:30 AM to move old forensic files to external storage.
- **Auto-Export**: Hourly (`HEARTBEAT_INTERVAL_MS` 3.6Ms) background tasks to export telemetry snapshots. Standardized to include the mandatory `role` field.

## 5. Summary of Files Involved
- `MainRepository.kt`: Orchestrates data flow between UI, DataStore, and Room.
- `AppDatabase.kt`: Room database definition. Includes v33-v39 migrations.
- `IntegrityMonitor.kt`: Implements the Storage Watchdog gates.
- `HistoryManager.kt`: Manages maintenance and ribbon aggregation.
