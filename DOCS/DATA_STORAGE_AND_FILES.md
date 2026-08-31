# Data Storage & Files Mechanism (vAug.31.04)

This document describes the multi-tier storage architecture used in the GPS Tracker project: reactive preferences for configuration, a relational database for high-frequency telemetry, and external JSON files for archival.

## 1. Configuration & Preferences (`DataStore`)
The app uses **Jetpack DataStore (Preferences)** for atomic, thread-safe configuration storage.
- **Managed by**: `SettingsRepository.kt` and `MainRepository.kt`.
- **Key Data**: Identity (IDs), Infrastructure (Relay URL), Security Toggles (`alertSettings`), and Geofencing (Home Points).
- **Draft Mechanism**: Supports uncommitted draft states for UI settings to ensure no configuration loss during rapid edits.

## 2. Relational Persistence (`Room / SQLite`)
For high-frequency forensic and historical data, the app uses a **Room Database** (`AppDatabase.kt`). The system currently operates on **Database Version 75**.

### A. Event Logs (`logs` table)
- **Forensic Aggregation**: Merges consecutive events with dynamic parts into a single entry.
- **Forensic Metrics**: Tracks `count` (occurrences) and `durationMs` (total active time).
- **Dual-Metric Spatial Anchor (v50)**: Every log entry includes `lat`, `lng`, `accuracy`, and `maxAccuracy` (Issue #325).
- **Hardened Sanitization (R779)**: **MANDATORY**. All persisted logs are scrubbed via `ForensicSanitizer` before being written to the database.

### B. Analytical History (`history` table)
- **Ribbon Streams**: Stores connection and sensor indices for 4M, 16M, 1H, 4H, 24H, and 7D views.
- **Forensic Parity (v75)**: Includes `violationUptimeMs` and `isUltraLongStationary` for authoritative historical replay (R782).
- **Power Forensic Parity**: Includes `currentMa` for end-to-end power visibility.
- **Environmental Indices**: Tracks `snrIdx`, `noiseIdx`, `vibeIdx`, `luxIdx`, `proxIdx`, `liftIdx`, `tiltIdx`, and `baroIdx`.

### C. Map Overlays
- **Trail Points (v50)**: Historical coordinates for Tracker and Viewer roles. Includes `accuracy` and `maxAccuracy`.
- **Violations (v48)**: Range, Jump, and Jammer violations with precise timestamps and spatial accuracy metrics.
- **SIT Markers**: Reconstructed from persistent telemetry to ensure zero-loss forensic parity.

### D. Offline Queuing (`pending_status_updates` table)
- **Persistence**: Queues status updates locally if the relay is disconnected.
- **Forensic Parity (v75)**: Includes `violationUptimeMs` and `isUltraLongStationary` to ensure state continuity after reconnection.

## 3. Storage Integrity Watchdog
The `IntegrityMonitor` implements a dual-tier protection system:
- **Tier 1: Critical (< 10MB)**: Absolute muzzle. All non-essential logging is halted.
- **Tier 2: Low (< 50MB)**: Throttled logging. Only "Important" forensic logs are allowed.

## 4. Maintenance & Archival
- **Sanitized Archiving (R779)**: **MANDATORY**. Daily archiving moving files to external storage ensures all exported files are rigorously scrubbed of internal paths and raw hardware IDs.
- **Daily Cleanup**: Scheduled task at 02:05 AM to optimize database performance.
- **Daily Archiving**: Scheduled task at 03:30 AM to move old forensic files to external storage.

## 5. Summary of Files Involved
- `MainRepository.kt`: Orchestrates data flow between UI, DataStore, and Room.
- `AppDatabase.kt`: Room database definition (v75).
- `ForensicSanitizer.kt`: Central authority for metadata scrubbing (R779).
