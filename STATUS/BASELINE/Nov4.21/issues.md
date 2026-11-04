# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov4.21)

## 🎯 Next Resumption Focus: Default OnRender Cloud Relay URL Configuration (GAP-REVOL51.0-G1) - COMPLETED

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

### 2. Flexible Tracker / Device ID Validation
- **Component**: `com.jd.core.shared.ForensicIdentity` & `MonitorActivity`
- **Severity**: Enhancement / Requirement Change
- **Description**: Relax strict 64-character SHA-256 hex hash validation on the "Enter Tracker ID" field to allow any sequence of characters (including a single character).
- **Status**: Implemented. `ForensicIdentity.isValid` updated to accept any non-blank character sequence.

### 3. Default Tracker ID and Monitor ID Specification
- **Component**: `com.jd.core.shared.ForensicIdentity`, `com.jd.core.network.ForensicIdentityManager`, `MonitorActivity`
- **Severity**: Requirement / Specification (Nov4.0)
- **Description**: Configure Default Tracker ID to `"T"` and Default Monitor ID to `"V"`. Pre-fill the pairing wizard text field in `MonitorActivity` with `"T"`.
- **Status**: Implemented & Passed. `ForensicIdentity.DEFAULT_TRACKER_ID = "T"`, `ForensicIdentity.DEFAULT_MONITOR_ID = "V"`.

---

## 🔴 Open Gaps (Nov4.21)
*   *None. All gaps resolved in Nov4.21 baseline.*

---

## 🟢 Resolved Gaps (Nov4.21)
*   **Default OnRender Cloud Relay URL Configuration (GAP-REVOL51.0-G1)**: Configured default fallback Relay URL in `MonitorSessionService` to point to `https://gps-relay-server.onrender.com` for out-of-the-box OnRender cloud connectivity and remote cross-network device testing (A15 and S21FE). **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.20-withServer)
*   **Cloud Relay Server Revival & Relay URL Configuration (GAP-REVOL50.0-G1)**: Revived Node.js Socket.io signaling relay server in `relay-server/index.js` with persistent disk caching and health check endpoints. Configured application DataStore and `MonitorSessionService` to support cloud-hosted Relay URLs for seamless cross-network device testing. **PASSED.**

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL39.0-S1**: Integrated Kinetic State Observer. Consolidate acceleration, velocity, and thermal status evaluation into a single unified observer in `core-shared`.
*   **SIMP-REVOL38.3-S1**: Dynamic Buffer Auto-tuner. Consolidate bitrate detection and buffer resizing into an adaptive native manager.
*   **SIMP-REVOL38.2-S1**: Thermal Status Observer Unification. Move all thermal listening logic to a specialized `ThermalManager` in `core-shared`.

---

## 📊 Hardening Progress Dashboard
- **Nov4.21: [SOT Count: 908 (Rules: 408), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Default OnRender Cloud Relay URL Configuration), QA: 908]**
- **Nov4.20-withServer: [SOT Count: 907 (Rules: 407), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Cloud Relay Server Revival & Relay URL Configuration), QA: 907]**
- **Nov4.3: [SOT Count: 906 (Rules: 406), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Advanced Relay Partition Tolerance & Telemetry Edge Synchronization), QA: 906]**
- **Nov4.2: [SOT Count: 905 (Rules: 405), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Multi-Device Telemetry Sync & Expansion), QA: 905]**
- **Nov4.0: [SOT Count: 904 (Rules: 404), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Default Tracker ID "T" & Monitor ID "V"), QA: 904]**
