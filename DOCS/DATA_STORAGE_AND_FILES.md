# Data Storage & Files Mechanism (v8.9.52)

This document describes the multi-tier storage architecture used in the GPS Tracker project: reactive preferences for configuration, a relational database for high-frequency telemetry, and external JSON files for archival.

## 1. Configuration & Preferences (`DataStore`)
The app uses **Jetpack DataStore (Preferences)** for atomic, thread-safe configuration storage.
- **Managed by**: `SettingsRepository.kt` and `MainRepository.kt`.
- **Key Data**: Identity (IDs), Infrastructure (Relay URL), Security Toggles (`alertSettings`), and Geofencing (Home Points serialized as JSON).
- **Draft Mechanism**: Supports uncommitted draft states for UI settings to ensure no configuration loss during rapid edits.

## 2. Relational Persistence (`Room / SQLite`)
For high-frequency forensic and historical data, the app uses a **Room Database** (`AppDatabase.kt`). The system currently operates on **Database Version 50**.

### A. Event Logs (`logs` table)
- **Forensic Aggregation**: Merges consecutive events with dynamic parts into a single entry.
- **Forensic Metrics**: Tracks `count` (occurrences) and `durationMs` (total active time).
- **Role Identity**: Every log entry includes a mandatory `role` field.
- **Dual-Metric Spatial Anchor (v50)**: Every log entry includes `lat`, `lng`, `accuracy`, and `maxAccuracy` (Issue #325). This allows forensic parity between raw GPS accuracy and engine uncertainty.
- **Signal & Vibration Snapshots (v43)**: Logs now include `snrSnapshot` and `vibeSnapshot` for point-in-time forensic audit of environmental conditions.
- **Capacity**: Sliding window of the last 1000 entries. Threshold-based pruning.

### B. Analytical History (`history` table)
- **Ribbon Streams**: Stores connection and sensor indices for 4M, 16M, 1H, 4H, 24H, and 7D views.
- **Forensic Parity (v49)**: Includes `accuracy` and `maxAccuracy` for authoritative ribbon rendering (Issue #325).
- **Power Forensic Parity (Issue #337)**: Includes `currentMa` (battery current) for end-to-end power visibility.
- **SIT Metrics (Issue #329)**: Includes `sitVzTs` and `sitVz/sitDz` for improved chair event reconstruction.
- **Environmental Indices**: Tracks `snrIdx`, `tiltIdx`, and `baroIdx` (v44) for comprehensive forensic context.
- **Pruning (PERF_IO)**: Pruning is triggered globally by `MainRepository` after every 50 writes (`DB_PRUNE_THRESHOLD`).

### C. Map Overlays
- **Trail Points (v50)**: Historical coordinates for Tracker and Viewer roles. Includes `accuracy`, `maxAccuracy`, and `isHindsightCorrected` (v42) for forensic trajectory reconstruction (Issue #325).
- **Violations (v48)**: Range, Jump, and Jammer violations with precise timestamps and spatial accuracy metrics.
- **SIT Markers (Issue #194)**: Reconstructed from acknowledged logs to ensure zero-loss forensic parity.

### D. Offline Queuing (`pending_status` table)
- **Persistence**: If the relay is disconnected, the system queues status updates locally.
- **Location Pending Reason (v45)**: Explicitly tracks why a location was not sent (e.g., "ACCURACY_GATE", "STALENESS").
- **Extended Telemetry**: Includes `lastValidFixRealtime` (v41), `currentMa`, and full sensor indices.

## 3. Storage Integrity Watchdog
The `IntegrityMonitor` implements a dual-tier protection system based on `StatFs` metrics:
- **Tier 1: Critical (< 10MB `SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB`)**: Absolute muzzle. All non-essential logging is halted (Issue #316).
- **Tier 2: Low (< 50MB `SYSTEM_STORAGE_LOW_THRESHOLD_MB`)**: Throttled logging. Only "Important" forensic logs are allowed.

## 4. Maintenance & Archival
- **Safety Flush (Issue #308)**: Implemented mandatory safety-flush in service `onDestroy` and monotonic interval checks to ensure no data loss during app termination.
- **Daily Cleanup**: Scheduled task at 02:05 AM to optimize database performance.
- **Daily Archiving**: Scheduled task at 03:30 AM to move old forensic files to external storage.

## 5. Summary of Files Involved
- `MainRepository.kt`: Orchestrates data flow between UI, DataStore, and Room.
- `AppDatabase.kt`: Room database definition. Includes migrations up to **v50**.
- `IntegrityMonitor.kt`: Implements the Storage Watchdog gates.
