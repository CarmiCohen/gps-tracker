# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov4.28)

## 🎯 Next Resumption Focus: Nov4.28 Baseline Synchronization - COMPLETED

---

## ⚠️ Active Issues & Warnings

### 1. Compose SnapshotStateList Lock Verification Warning
- **Component**: `androidx.compose.runtime.snapshots.SnapshotStateList`
- **Severity**: Warning
- **Description**: 
  ```
  Method boolean androidx.compose.runtime.snapshots.SnapshotStateList.conditionalUpdate(boolean, kotlin.jvm.functions.Function1) failed lock verification and will run slower.
  ```
- **Impact**: Non-blocking performance warning during Compose state updates on Android runtime.
- **Status**: Monitored. No crashes or functional failures observed.

---

## 🟢 Resolved Gaps (Nov4.28)
*   **Pre-Revolution Map Functionality & Specifications Implementation (GAP-REVOL57.0-G1)**: Successfully implemented exhaustive pre-revolution map functionality in `:app-monitor`, including osmdroid marker/polyline pooling (`MapOverlayManager`), imperative camera & follow controller (`MapController`), Map Tools Overlay (`MapToolsOverlay`) with quick actions (`IN`, `OUT`, `TRACKER`, `VIEWER`, `JUMP`, `SAVE`, `LOAD`, `ADD`, `DEL`, `FENCE`, `CLEAR`), Home Point boundary circles (`Polygon.pointsAsCircle`), and spatial violation markers. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.27)
*   **Relay URL Setup Option Integration in Sensor App (GAP-REVOL56.0-G1)**: Integrated Relay URL setup section into `SettingsOverlay` in `AdminActivity.kt`, updated Sensor Setup & Info dialog with Relay URL display & change shortcut, made `SensorViewModel` and `SensorService` reactively observe Relay URL changes and dynamically reconnect `SignalingClient`, and updated Monitor app with Relay URL configuration support. **PASSED.**

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL39.0-S1**: Integrated Kinetic State Observer. Consolidate acceleration, velocity, and thermal status evaluation into a single unified observer in `core-shared`.
*   **SIMP-REVOL38.3-S1**: Dynamic Buffer Auto-tuner. Consolidate bitrate detection and buffer resizing into an adaptive native manager.

---

## 📊 Hardening Progress Dashboard
- **Nov4.28: [SOT Count: 914 (Rules: 414), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Pre-Revolution Map Functionality & Specifications Implementation), QA: 914]**
- **Nov4.27: [SOT Count: 913 (Rules: 413), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Relay URL Setup Option Integration in Sensor App & Reactive Signaling Reconnect), QA: 913]**
- **Nov4.26: [SOT Count: 912 (Rules: 412), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Monitor UI Refinement, Upper Menu, Setup Button, SRV/GPS LED Fixes & Map Controls), QA: 912]**
- **Nov4.25: [SOT Count: 911 (Rules: 411), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Nov4.25 Stabilization & Reciprocal Simulation), QA: 911]**
- **Nov4.24: [SOT Count: 910 (Rules: 410), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (HUD Status Bar Compact Layout Hardening & UI Refinement), QA: 910]**
- **Nov4.22-addHUD: [SOT Count: 909 (Rules: 409), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Post-Revolution HUD Integration & LED Status Verification), QA: 909]**
