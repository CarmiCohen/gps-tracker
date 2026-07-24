# Issue #338: Unified Telemetry Freshness

## 🎯 Status: Resolved (Historical)
**Category**: UI / UX / Staleness Logic

---

## 📝 Description
The UI lacked a unified mechanism to determine if the incoming telemetry from the remote peer was "fresh" or "stale". This led to inconsistent status displays across the Tracker and Viewer screens.

## 🛠️ Resolution
- Implemented `isTelemetryFresh` flag in the state pipeline.
- Established a unified staleness threshold (35,000ms) in `EngineConstants.kt`.
- Propagated the freshness state to `TrackerScreen` and `ViewerScreen` for visual indication.

## 🔗 References
- **Requirement**: R338 (Staleness Authority)
- **Files**: `TrackerScreen.kt`, `ViewerScreen.kt`
