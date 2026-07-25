# Issue #542: Startup Frame Skipping / Main Thread Congestion

## 🎯 Status: Open (July.24.06)
**Category**: Performance / UX

---

## 📝 Description
Significant frame skipping detected during `MainActivity` creation. Cold start frame skipping has escalated from 70 frames to **305 frames (~5.1s)** on target hardware (Samsung A15).

## 🔍 Observations
- **Observation**: "Skipped 305 frames!" in Logcat during app launch.
- **Impact**: Critical first-frame delay, poor user perception, and potential ANR risk if the main thread remains congested.

## 🛠️ Planned Action
- Profile `MainActivity.onCreate` and `MainAppContent` composition.
- Identify heavy initialization tasks (Database, Hardware Managers) that can be deferred or moved to background threads.
- Implement staggered initialization for non-critical UI components.

## 🔗 References
- **Requirement**: R526 (Main-Thread Purity)
- **Cycle**: July.24.06
