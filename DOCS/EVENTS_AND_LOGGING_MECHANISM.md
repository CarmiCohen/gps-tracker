# Events & Logging Mechanism (v8.9.37)

This document describes the event tracking, persistence, and synchronized logging architecture of the GPS Tracker.

## 1. Event Architecture
The project uses a unified JSON-based event structure. Every significant occurrence is encapsulated as an "Event".

### A. Core Event Structure
An event typically contains:
- `type`: Category (`system`, `user`, `alarm`, `network`, `error`).
- `message`: Descriptive text.
- `isImportant`: Boolean flag used for highlighting.
- `isSpecial` & `specialColor`: Forensic flags for critical security events (Pink/F472B6).
- `durationMs` & `count`: Forensic metrics for combined events.
- `firstSeenTs`: Tracks the beginning of a sustained event group.
- `role`: Mandatory field indicating which device generated the event.
- **Log Spatial Anchor (Issue #208)**: All events are automatically anchored with `lat`/`lng` coordinates to enable historical map reconstruction.
- **SIT Latch (Issue #194)**: SIT events are synchronized via a 10s acknowledged loop to ensure forensic persistence.

## 2. UI Rendering & Semantic Colors
The log overlay uses a keyword-based priority system for visual identification:

1.  **Special Security (Pink / F472B6)**: Hardcoded override for Jammer, Tamper, and Geofence alerts.
2.  **Critical (Red / Rose500)**: Triggered by `CRITICAL`, `ERROR`, or `VIOLATION`.
3.  **Success/Restored (Green / Emerald500)**: Triggered by `CONNECTED`, `RESTORED`, or the `isImportant` flag.
4.  **User Actions (Orange / ViewerOrange)**: Triggered by the `USER ACTION` prefix.
5.  **Forensic Details (Cyan / Teal500)**: Default color for detail logs.
6.  **Ghost Mode UX (Issue #193)**: Visual staleness indicators are applied to log entries when telemetry is older than `TELEMETRY_UI_STALE_THRESHOLD_MS` (10s).

## 3. Storage Integrity Watchdog
To prevent database corruption, the `IntegrityMonitor` enforces strict suppression based on available internal storage:
- **Tier 1: Critical (< 10MB `SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB`)**: Absolute muzzle. ALL non-essential logging is halted.
- **Tier 2: Low (< 50MB `SYSTEM_STORAGE_LOW_THRESHOLD_MB`)**: Throttled logging. Only "Important" or "Special" logs pass (Issue #403).

## 4. Navigation & Context
- **Intelligent Return**: Closing the log overlay returns the user to the previous primary screen (Map or Dashboard).
- **Header Integration**: The Log button in the `HeaderBar` reflects the current view state.

## 5. Forensic Logic & Batching
- **Fuzzy Matching**: Merges consecutive events with dynamic parts into a single entry using regex-based variable stripping (Issue #294).
- **Accumulated Metrics**: Combined entries display occurrence counts `(xN)` and total active duration.
- **Monotonic Timing (Issue #413)**: Uses `TimeProvider.elapsedRealtime()` for all forensic duration calculations.

## 6. Forensic Unification
Legacy version tags have been removed from data models. Traceability is maintained by injecting the build version at the emission layer and is enhanced by **acknowledged SIT synchronization** (Issue #194), **Power Forensic Parity** (Issue #192), and **Spatial Anchoring** (Issue #208).
