# Events & Logging Mechanism (v8.8.35)

This document describes the event tracking, persistence, and synchronized logging architecture of the GPS Tracker.

## 1. Event Architecture
The project uses a unified JSON-based event structure. Every significant occurrence is encapsulated as an "Event".

### A. Core Event Structure
An event typically contains:
- `type`: Category (`system`, `user`, `alarm`, `network`, `error`).
- `message`: Descriptive text.
- `isImportant`: Boolean flag used for highlighting.
- `isSpecial` & `specialColor`: Forensic flags for hardcoded high-contrast rendering of critical security events (Pink/F472B6).
- `durationMs` & `count`: Forensic metrics for combined events.
- `firstSeenTs`: Tracks the beginning of a sustained event group.
- `role`: Mandatory field indicating which device generated the event.

## 2. UI Rendering & Semantic Colors
The log overlay uses a keyword-based priority system for visual identification:

1.  **Special Security (Pink / F472B6)**: Hardcoded override for Jammer, Tamper, and Geofence alerts.
2.  **Critical (Red / Rose500)**: Triggered by `CRITICAL`, `ERROR`, or `VIOLATION`.
3.  **Success/Restored (Green / Emerald500)**: Triggered by `CONNECTED`, `RESTORED`, or the `isImportant` flag.
4.  **User Actions (Orange / ViewerOrange)**: Triggered by the `USER ACTION` prefix.
5.  **Forensic Details (Cyan / Teal500)**: Default color for detail logs.

## 3. Storage Integrity Watchdog
To prevent database corruption, the `IntegrityMonitor` enforces strict suppression based on available internal storage:
- **Tier 1: Critical (< 10MB `SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB`)**: Absolute muzzle. ALL non-essential logging is halted. Only "Special" logs pass.
- **Tier 2: Low (< 50MB `SYSTEM_STORAGE_LOW_THRESHOLD_MB`)**: Throttled logging. Only "Important" or "Special" logs pass.

## 4. Navigation & Context
- **Intelligent Return**: Closing the log overlay returns the user to the previous primary screen (Map or Dashboard).
- **Header Integration**: The Log button in the `HeaderBar` reflects the current view state.

## 5. Forensic Logic & Batching
- **Fuzzy Matching**: Merges consecutive events with dynamic parts into a single entry using regex-based variable stripping.
- **Accumulated Metrics**: Combined entries display occurrence counts `(xN)` and total active duration.
- **Monotonic Timing**: Uses `TimeProvider` for all forensic duration calculations.

## 6. Forensic Unification
Legacy version tags (`ver`, `vid`) have been removed from data models and database schemas in v8.8.35. The system maintains auditability by injecting the version string at the emission layer (SyncManager/LogSink), ensuring a lean and maintainable data structure.
