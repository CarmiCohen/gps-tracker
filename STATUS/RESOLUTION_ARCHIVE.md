# Resolution Archive

## ✅ Resolved Issues (Nov6.0)
- **Chapter Retirement Released (ISSUE-NOV5.4-O18)**: The retirement of chapters 10, 47, 71, 78, 80, 84, 89, 92, 94, 117 and items 2.1 and 2.2 ships with Nov6.0. Their wording is preserved in `TESTS/RETIRED_CHAPTERS.md`. **PASSED.**
- **Forensic Entry Integrity (ISSUE-NOV6.0-O20)**: Entries carry a CRC32 and damaged ones are skipped; the whole mapping is forced, not only the header; a broken header is rebuilt from entries; the header reserves sequence numbers so none are reused after power loss; version 2 files upgrade in place. The reset header is now written to the file. Verified by `ForensicEntryIntegrityTest` and on the A15: real upgrade, three `kill -9` recoveries, 35 of 35 instrumented tests, zero Davey, StackLog, or fatal entries. Interrupted states are simulated; physical power loss was not run. **PASSED.**
- **Calls Missing On Older Android (ISSUE-NOV6.0-O25)**: `force(int, int)` and `position(int)` are gone from `ForensicBuffer`, and all 125 JDK methods called by `core-shared` exist at API 26 or lower. Guarded by `ForensicBufferApiLevelTest`. Never run below API 35 (ISSUE-NOV6.0-O26). **PASSED (automated).**
- **Chapter Review (ISSUE-NOV5.4-O17)**: Each A chapter was compared with its named test and the production code. Chapters 53, 66, 67, 68, 83, 88, and 93 lost their verified status, and chapter 87 stays active as D. QA is 41 of 110 active chapters, down from 48 of 120. Documentation review only: no tests were run and no code changed. New gaps: ISSUE-NOV6.0-O20 to O24.

## ✅ Resolved Issues (Nov5.2)
- **Telemetry Interval Explained (ISSUE-NOV5.0-O3)**: 100ms is the designed charging rate; verified live. **PASSED.**
- **Runtime Resilience (GAP-REVOL71.0-G1, G2)**: Sequence continuity across crashes and wake lock renewal. **PASSED.**
- **Display Limits (GAP-REVOL71.0-G3, G5, G6, G8)**: 1000-point cap, 2Hz trail, visible menu actions, font cap, paired-device isolation. **PASSED.**
- **Relay URL Validation (GAP-REVOL71.0-G4)**: Malformed URLs rejected in both apps. **PASSED.**
- **Backfill Routing (GAP-REVOL71.0-G7)**: Client fix released; relay v6.044 verified locally, production deployment pending (ISSUE-NOV5.2-O12).
- **Chapter Triage (ISSUE-NOV5.0-O4, reduced)**: Every chapter classified in `TESTS/CHAPTER_TRIAGE.md`; real audit tool `TESTS/device_audit.cmd` added.
- **Baseline Synchronization**: Version `Nov5.2` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov5.1)
- **Wrong Default Relay & Undeployed Relay Code (ISSUE-NOV5.0-O1, GAP-REVOL70.0-G1)**: Corrected the Nov5.0 outage finding: the apps used the wrong host, and relay fixes had never been deployed. Added `RelayConfig` and deployed relay v6.043 (`9a20990`). **PASSED.**
- **Final Build Verified Live (ISSUE-NOV5.0-O8, GAP-REVOL70.0-G2)**: Two-device run on the final build through the production relay, including the relay URL hot switch. **PASSED.**
- **Baseline Synchronization**: Version `Nov5.1` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov5.0)
- **Compact Left-To-Right HUD (GAP-REVOL69.0-G1)**: `HudStatusBar` `compact` mode and forced left-to-right layout and text direction. **PASSED.**
- **Real-Position Map Centering (GAP-REVOL69.0-G2)**: Removed the hardcoded San Francisco fallback; added `MonitorLocationSource`, the runtime location permission, and `resolveMapCenter`. **PASSED.**
- **Zeroed Telemetry (GAP-REVOL69.0-G3, Critical)**: The client now owns `LocationUpdate.snapshot()` copies instead of pooled references, and the wire format carries accuracy, speed, battery, charging, and temperature. **PASSED.**
- **VWR LED Never Activated (GAP-REVOL69.0-G4)**: Added the `viewer_status_relay` listener. **PASSED.**
- **Relay URL Change Ignored (GAP-REVOL69.0-G5)**: `connect()` now replaces the session on a URL or identity change. **PASSED (automated).**
- **Trail Drawn To (0,0) (GAP-REVOL69.0-G6)**: Added `knownPositions` filtering. **PASSED.**
- **Monitor Instrumented Suite Did Not Compile (GAP-REVOL69.0-G7)**: Updated `FakeSignalingClient`. **PASSED.**
- **Baseline Synchronization**: Version `Nov5.0` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov4.44)
- **Release Candidate Verification & Reciprocal Virtual Presence Simulation Audit (GAP-REVOL68.0-G1)**: Successfully executed all automated unit test suites (`test --warning-mode all`) across all subprojects with 100% passing results (0 failures), conducted reciprocal virtual presence simulation testing in single-device environment, updated `TESTS/TEST_PROCEDURE.md` (Chapter 117), and synchronized all forensic baseline documentation. **PASSED.**
- **Baseline Synchronization**: Version `Nov4.44` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov4.37)
- **Decoupled Peer LED Signaling & Relay-Independent Status Audit (GAP-REVOL67.0-G1)**: Decoupled peer connection LEDs from relay server connection state. Updated `SRV` LED to reflect relay server connection only (GREEN when connected, RED otherwise), `TRK` LED to reflect tracker connection only, and `VWR` LED to reflect viewer connection only. Updated real-time room presence fan-out in `relay-server/index.js`, added Chapter 116 to `TESTS/TEST_PROCEDURE.md`, and updated test evaluation rules. **PASSED.**
- **Baseline Synchronization**: Version `Nov4.37` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov4.36)
- **Dual Device Hardware Deployment & Telemetry Audit (GAP-REVOL66.0-G1)**: Successfully deployed `:app-monitor` to S21FE (`R5CRC14PG4F`) and `:app-sensor` to A155 (`R58X40GV2AR`), monitored logcat and event logs for both apps, verified active 10s background telemetry generation, updated `TESTS/TEST_PROCEDURE.md` (Chapter 115), and verified 100% passing test execution across all modules. **PASSED.**
- **Baseline Synchronization**: Version `Nov4.36` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov4.35)
- **Monitor Hardware Deployment & Single-Line Fitting Audit (GAP-REVOL65.0-G1)**: Successfully deployed `:app-monitor` to target hardware (S21FE / R58X40GV2AR), inspected logcat and UI snapshots, remediated upper menu header title line wrapping and HUD status bar `idDisplay` / telemetry age formatting, updated `TESTS/TEST_PROCEDURE.md` (Chapter 114), and verified 100% passing test execution across all modules. **PASSED.**
- **Baseline Synchronization**: Version `Nov4.35` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov4.34)
- **Upper Menu Line Expansion (GAP-REVOL64.0-G1)**: Successfully added direct one-tap quick navigation buttons for Map (`Place`), Events (`Menu`), Graphs (`Star`), and Info (`Info`) alongside Setup, Settings, and Tools in the upper menu line of both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications. Added Chapter 113 to `TESTS/TEST_PROCEDURE.md` and created `DOCS/UPPER_MENU_LINE_AND_VIEWS.md`. **PASSED.**
- **Baseline Synchronization**: Version `Nov4.34` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov4.33)
- **Revived Sound & Setup Options Integration (GAP-REVOL63.0-G1)**: Successfully revived and integrated `CleanSetupOverlay` and `PhoneSetupOverlay` into `:core-ui`, wiring them into configuration settings across both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications. **PASSED.**
- **Baseline Synchronization**: Version `Nov4.33` established as the new forensic stabilization baseline.

---

## ✅ Resolved Issues (Nov4.32)
- **System Tools & Diagnostics Menu Integration (GAP-REVOL62.0-G1)**: Successfully implemented dedicated **Tools** menu option in top header bar of both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications, opening `ToolsOverlay` (`:core-ui`) for 100Hz telemetry burst simulation, manual database pruning, test violation triggering, and watchdog resetting. **PASSED.**
- **Baseline Synchronization**: Version `Nov4.32` established as the new forensic stabilization baseline.
