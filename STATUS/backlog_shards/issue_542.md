# Issue #542: Startup Frame Skipping / Main Thread Congestion

## 🎯 Status: Resolved (July.24.07)
**Category**: Performance / UX

---

## 📝 Description
Significant frame skipping detected during `MainActivity` creation. Cold start frame skipping has escalated from 70 frames to **305 frames (~5.1s)** on target hardware (Samsung A15).

## 🔍 Observations
- **Observation**: "Skipped 305 frames!" in Logcat during app launch.
- **Impact**: Critical first-frame delay, poor user perception, and potential ANR risk.

## 🛠️ Resolution
- **Forensic Fix**: Refactored `MainAppContent.kt` to defer the collection of heavy Room-backed flows (`eventLogsFlow`, `trackerTrailFlow`, `viewerTrailFlow`, `violationPointsFlow`).
- **Effect**: These flows are now only collected when the user navigates into the specific `Tracker` or `Viewer` routes.
- **Result**: Cold-start main thread congestion significantly reduced. Frame skips dropped from 305 to <50.

## 🔗 References
- **Requirement**: R526 (Main-Thread Purity), R542 (Deferred Flow Collection)
- **Cycle**: July.24.07
