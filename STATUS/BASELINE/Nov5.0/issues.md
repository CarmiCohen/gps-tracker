# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov5.0)

## 🎯 Next Resumption Focus: Restore the production relay (ISSUE-NOV5.0-O1), then re-run the two-device test against it

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

### 2. Open Gaps Identified in Nov5.0
- **ISSUE-NOV5.0-O1 (High) - Production relay unresponsive**: `https://gps-relay-server.onrender.com` timed out over HTTPS (`curl` exit 28 at 45s and 60s) and both phones logged `SocketIOException: timeout` every 25s from about 10:46 to 11:00 on Nov 5, while its edge answered plain HTTP. The cause is outside this repository. Until it is restored, devices on the default URL cannot connect.
- **ISSUE-NOV5.0-O2 (Medium) - Monitor local HUD line is hardcoded**: `MonitorViewModel.localHudState` always reports battery 100, temperature 30, accuracy 5.0, age 0, speed 0, and STATIONARY instead of real device values.
- **ISSUE-NOV5.0-O3 (Medium) - Telemetry interval differs from the documented value**: the A15 logged `Interval: 100ms` while charging, whereas Chapter 115.2 documents 10000ms. Confirm whether this is the intended charging acceleration (Chapter 49); it affects relay load and data use.
- **ISSUE-NOV5.0-O4 (Medium) - Test coverage gap**: most physical chapters of `TEST_PROCEDURE.md` and all five manual sections of `TEST_FORENSIC_STRESS_PROTOCOL.md` were not executed in Nov5.0 (see `TESTS/test_results.md`). `ProductionReadinessAuditTest` holds three math checks and does not measure Davey or StackLog. Earlier "100% passing" entries covered unit tests only: the Monitor instrumented suite did not compile from Nov4.37 until Nov5.0, and the zeroed telemetry defect passed every earlier audit.
- **ISSUE-NOV5.0-O5 (Low) - False relay status text**: the Monitor Setup dialog hardcodes `Relay Status: Connected / Active`, even while `SRV` is red.
- **ISSUE-NOV5.0-O6 (Low) - Stale version strings**: `SensorService` logs `Version: Nov1.7 (Final)`, and `getAppVersionName` falls back to `Nov4.27` in both apps.
- **ISSUE-NOV5.0-O7 (Low) - Zero-position rows are still stored and sent**: the sensor sends `(0,0)` until its first fix and the Monitor stores those rows. They are now filtered at display time only.

---

## 🟢 Resolved Gaps (Nov5.0)
*   **Compact Left-To-Right HUD (GAP-REVOL69.0-G1)**: Added `compact` mode to `HudStatusBar` (root cause: text inherited the 24sp `bodyLarge` line height) and forced left-to-right layout and text direction in both apps. Verified on A15 and S21FE under `he-IL`. **PASSED.**
*   **Real-Position Map Centering (GAP-REVOL69.0-G2)**: Removed the hardcoded San Francisco `DEFAULT_LAT`/`DEFAULT_LNG`. The Monitor now requests location permission and reads its own position through `MonitorLocationSource`, and `resolveMapCenter` selects the center. `SENSOR` no longer snaps away while the peer is inactive. With no position the map shows the world view. Verified on S21FE. **PASSED.**
*   **Zeroed Telemetry (GAP-REVOL69.0-G3, Critical)**: `SensorService` released pooled `LocationUpdate` objects while `SocketioSignalingClient` still held them, so normal updates reached the relay as `seq=0, lat=0, lng=0, ts=0`. The client now keeps `LocationUpdate.snapshot()` copies. Accuracy, speed, battery, charging, and temperature were added to the wire format. Verified end to end from A15 to S21FE. **PASSED.**
*   **VWR LED Never Activated (GAP-REVOL69.0-G4)**: The client had no listener for `viewer_status_relay`, so `VWR` could never turn green (Rule 1.348). Listener added. Verified on A15. **PASSED.**
*   **Relay URL Change Ignored (GAP-REVOL69.0-G5)**: `connect()` ignored a new URL while connected and leaked the old socket when not connected, contradicting Chapter 107. The old session is now closed first. Verified by unit test only, because the production relay outage prevented a hardware check. **PASSED (automated).**
*   **Trail Drawn To (0,0) (GAP-REVOL69.0-G6)**: `knownPositions` removes zero rows from the polyline, marker, and map center. Verified on S21FE. **PASSED.**
*   **Monitor Instrumented Suite Did Not Compile (GAP-REVOL69.0-G7)**: `FakeSignalingClient` lacked `viewerStatusFlow` since Nov4.37. Fixed, and the suite now runs 12/12 on A15. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.44)
*   **Release Candidate Verification & Reciprocal Virtual Presence Simulation Audit (GAP-REVOL68.0-G1)**: Executed all automated test suites (`test --warning-mode all`) across all subprojects with 100% passing results (0 failures), conducted reciprocal virtual presence simulation testing in single-device environment, updated `TESTS/TEST_PROCEDURE.md` (Chapter 117), and synchronized all forensic baseline documentation. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.37)
*   **Decoupled Peer LED Signaling & Relay-Independent Status Audit (GAP-REVOL67.0-G1)**: Decoupled peer connection LEDs from relay server connection state. Updated `SRV` LED to reflect relay server connection only (GREEN when connected, RED otherwise), `TRK` LED to reflect tracker connection only, and `VWR` LED to reflect viewer connection only. Updated real-time room presence fan-out in `relay-server/index.js`, added Chapter 116 to `TESTS/TEST_PROCEDURE.md`, and updated test evaluation rules. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.36)
*   **Dual Device Hardware Deployment & Telemetry Audit (GAP-REVOL66.0-G1)**: Successfully deployed `:app-monitor` to S21FE (`R5CRC14PG4F`) and `:app-sensor` to A155 (`R58X40GV2AR`), monitored logcat and event logs for both apps, verified active 10s background telemetry generation, updated `TESTS/TEST_PROCEDURE.md` (Chapter 115), and verified 100% passing test execution across all modules. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.35)
*   **Monitor Hardware Deployment & Single-Line Fitting Audit (GAP-REVOL65.0-G1)**: Successfully deployed `:app-monitor` to target hardware (S21FE / R58X40GV2AR), inspected logcat and UI snapshot, remediated upper menu header title line wrapping and HUD status bar `idDisplay` / telemetry age formatting, updated `TESTS/TEST_PROCEDURE.md` (Chapter 114), and verified 100% passing test execution across all modules. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.34)
*   **Upper Menu Line Expansion (GAP-REVOL64.0-G1)**: Added direct one-tap quick navigation buttons for Map (`Place`), Events (`Menu`), Graphs (`Star`), and Info (`Info`) alongside Setup, Settings, and Tools in the upper menu line of both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.33)
*   **Revived Sound & Setup Options Integration (GAP-REVOL63.0-G1)**: Revived and integrated `CleanSetupOverlay` and `PhoneSetupOverlay` in `:core-ui`, wiring them into configuration settings across both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications. **PASSED.**

---

## 🟢 Resolved Gaps (Nov4.32)
*   **System Tools & Diagnostics Menu Integration (GAP-REVOL62.0-G1)**: Integrated dedicated **Tools** menu option in top header bar of both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications, opening `ToolsOverlay` (`:core-ui`) for 100Hz telemetry burst simulation, database pruning, test violation triggering, and watchdog resetting. **PASSED.**

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL39.0-S1**: Integrated Kinetic State Observer. Consolidate acceleration, velocity, and thermal status evaluation into a single unified observer in `core-shared`.
*   **SIMP-REVOL38.3-S1**: Dynamic Buffer Auto-tuner. Consolidate bitrate detection and buffer resizing into an adaptive native manager.
*   **SIMP-NOV5.0-S1**: Single Map Path. `OsmMap` in `MapComponents.kt` has no callers, while `MonitorScreen` drives its own `MapView`. Either adopt that stack or delete it, along with its unused models and controller.

---

## 📊 Hardening Progress Dashboard
- **Nov5.0: [SOT Count: 928 (Rules: 428), Open: H:1, M:3, L:3, Ideas: H:0, M:3, L:8, Testing: PASSED (Sub-items: 100), QA: 928]** - Audit Baseline: 100 automated tests passing on the final code; two-device live run through a local relay because the production relay was down; manual physical chapters not executed.
- **Nov4.44: [SOT Count: 924 (Rules: 424), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Release Candidate & Reciprocal Simulation Audit), QA: 924]**
- **Nov4.37: [SOT Count: 924 (Rules: 424), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Decoupled Peer LED Signaling Audit), QA: 924]**
- **Nov4.36: [SOT Count: 923 (Rules: 423), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Dual Device Hardware Deployment & Telemetry Audit), QA: 923]**
- **Nov4.35: [SOT Count: 922 (Rules: 422), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Monitor Hardware Deployment & Single-Line Fitting Audit), QA: 922]**
- **Nov4.34: [SOT Count: 921 (Rules: 421), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Upper Menu Line Expansion), QA: 921]**
- **Nov4.33: [SOT Count: 920 (Rules: 420), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Revived Sound & Setup Options Integration), QA: 920]**
- **Nov4.32: [SOT Count: 919 (Rules: 419), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (System Tools & Diagnostics Menu Integration), QA: 919]**
- **Nov4.31: [SOT Count: 918 (Rules: 418), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Device-Specific Setup & Hardware Hardening Revival), QA: 918]**
- **Nov4.30: [SOT Count: 917 (Rules: 417), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Monitor Dual HUD, Centering & Alarming Setup Revival), QA: 917]**
- **Nov4.29: [SOT Count: 915 (Rules: 415), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Nov4.29 Stabilization & Comprehensive Test Execution Across All Subprojects), QA: 915]**
- **Nov4.28: [SOT Count: 914 (Rules: 414), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Pre-Revolution Map Functionality & Specifications Implementation & Test Chapters 107-108), QA: 914]**
- **Nov4.27: [SOT Count: 913 (Rules: 413), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Relay URL Setup Option Integration in Sensor App & Reactive Signaling Reconnect), QA: 913]**
- **Nov4.26: [SOT Count: 912 (Rules: 412), Open: H:0, M:0, L:0, Ideas: H:0, M:3, L:7, Testing: PASSED (Monitor UI Refinement, Upper Menu, Setup Button, SRV/GPS LED Fixes & Map Controls), QA: 912]**
