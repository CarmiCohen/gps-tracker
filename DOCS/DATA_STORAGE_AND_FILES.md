# Data Storage & Files Mechanism (v8.8.35)

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
- **Capacity**: Sliding window of the last 1000 entries. Threshold-based pruning.

### B. Analytical History (`history` table)
- **Ribbon Streams**: Stores connection and sensor indices for 4M, 16M, 1H, 4H, 24H, and 7D views.
- **Batch Processing**: History points are buffered and flushed in batches every 5,000ms (`HISTORY_BATCH_WRITE_INTERVAL_MS`) or upon reaching 100 entries (`HISTORY_BUFFER_MAX_SIZE`) to optimize I/O.
- **Pruning (PERF_IO)**: Pruning is triggered globally by `MainRepository` after every 50 writes (`DB_PRUNE_THRESHOLD`) to minimize SQLite WAL pressure.
- **Forensic SNR Monitoring**: Includes `snrIdx` for long-term GNSS health tracking.
- **Aggregation Strategy**: Uses "Worst-Case" logic for long-term buckets to ensure momentary health drops are never masked.

### C. Map Overlays
- **Trail Points**: Historical coordinates for Tracker and Viewer roles. Limited to 1000 points per role.
- **Violations**: Range, Jump, and Jammer violations with precise timestamps. Limited to 1000 entries.

### D. Offline Queuing (`pending_status` table)
- **Persistence**: If the relay is disconnected, the system queues status updates locally. These are flushed automatically with `snrIdx` preservation once the connection is restored. Limited to 1000 entries.

## 3. Storage Integrity Watchdog
The `IntegrityMonitor` implements a dual-tier protection system based on `StatFs` metrics:
- **Tier 1: Critical (< 10MB `SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB`)**: Absolute muzzle. All non-essential logging is halted to prevent database corruption.
- **Tier 2: Low (< 50MB `SYSTEM_STORAGE_LOW_THRESHOLD_MB`)**: Throttled logging. Only "Important" or "Special" forensic logs are allowed.

## 4. Maintenance & Archival
- **Daily Cleanup**: Scheduled task at 02:05 AM to clear historical trail points and optimize database performance.
- **Daily Archiving**: Scheduled task at 03:30 AM to move old forensic files to external storage.
- **Auto-Export**: Hourly (`HEARTBEAT_INTERVAL_MS` 3.6Ms) background tasks to export telemetry snapshots for forensic continuity. Standardized to include the mandatory `role` field. Legacy `ver` field has been removed in v8.8.35.

## 5. Summary of Files Involved
- `MainRepository.kt`: Orchestrates data flow between UI, DataStore, and Room.
- `AppDatabase.kt`: Room database definition.
- `IntegrityMonitor.kt`: Implements the Storage Watchdog gates.
- `HistoryManager.kt`: Manages daily maintenance and ribbon aggregation.
