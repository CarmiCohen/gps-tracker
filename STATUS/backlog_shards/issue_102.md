# Issue #102: Temporal Forensic Integrity

## 🎯 Status: Resolved (Historical)
**Category**: Core Engine / Data Integrity

---

## 📝 Description
The system lacked a reliable way to distinguish between wall-clock time (subject to user tampering or NTP jumps) and monotonic time (elapsed since boot) in forensic logs. This caused "clock regression" errors and inconsistent duration calculations.

## 🛠️ Resolution
- Implemented a **Dual-Time Strategy**: 
    - `rt` (Realtime): Monotonic nanoseconds for logic, debounce, and duration calculations.
    - `ts` (Timestamp): Wall-clock milliseconds for human-readable forensic logging.
- Standardized all engine models (`EngineGeoPoint`, `EngineConnectionPoint`, etc.) to include both fields.
- Added clock jump detection in `HistoryManager` to flag NTP/Manual changes.

## 🔗 References
- **Requirement**: R102 (Temporal Forensic Integrity)
- **Files**: `core:engine`, `HistoryManager.kt`, `Models.kt`
