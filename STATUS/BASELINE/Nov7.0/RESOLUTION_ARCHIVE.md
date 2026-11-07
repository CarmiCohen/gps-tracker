# Resolution Archive

## ✅ Resolved Issues (Nov7.0)
- **Real Version Strings (ISSUE-NOV5.0-O6)**: `SensorService` logged `Version: Nov1.7 (Final)` on every start, and `getAppVersionName` fell back to an invented `Nov4.27` in both apps, because versions were written by hand in the code and the function was copied. One `getAppVersionName` in `core-ui/.../AppVersion.kt` now reads the installed package and shows `--` if that fails, and `SensorService` logs the same value. `VersionLiteralGuardTest` fails if any production string holds a release version. Verified by 182 of 182 automated tests; with the old strings back, `noProductionStringHoldsAVersion` named both and `AppVersionTest.anUnknownPackageShowsNoVersion` failed on the A15. On the A15 through the production relay, all four version displays showed `Nov7.0`, and the Sensor logged `onCreate called. Version: Nov7.0`. The `--` case has automated proof only. **PASSED.**

## ✅ Resolved Issues (Nov6.4)
- **Backfill Gap Recovery (ISSUE-NOV6.3-O32, High)**: A gap existed only at the moment it was detected, so any request that was over the limit, could not be sent, or was never answered lost its rows for good. The Monitor now reads gaps from the `trails` table with `TrailDao.getSequenceGaps`, so a hole stays findable until it is filled. `BackfillLedger` keeps the limit of 10 requests in flight, takes the newest gaps first, and does not repeat an answered range in the same process. `SocketioSignalingClient` also dropped updates silently: with a busy collector only 64 of 600 telemetry updates and 8 of 200 backfill chunks got through, so its flows now hold a full burst (`BURST_CAPACITY`, 5000). That is a worst-case test: it shows the mechanism, not the loss rate in the field test. Verified by 177 of 177 automated tests; limiting the scan to the newest gap made `everyHoleIsRefilledWhenThereAreMoreThanTheLimit` and `aFailedRequestIsNotForgotten` fail. Live on the A15 through a private relay: a 60-second Sensor outage at 10Hz left one gap of 195 rows, refilled about 2 seconds after reconnecting, and the Monitor table then held 3,011 rows, sequences 1 to 3011, with no holes. That outage produced one hole, so more holes than the limit is proven by automated tests only. Not run on the production relay or on two phones. **PASSED.**
- **Unanswered Backfill Requests (ISSUE-NOV6.2-O31)**: A request with no response held its slot for the life of the process. It is now released after 30 seconds without a chunk and asked again by a later scan. Scans run only while telemetry arrives, so a silent Sensor gets no repeated requests. Verified by `BackfillLedgerTest`; the timeout was not observed on a device. **PASSED (automated).**
- **Relay v6.044 On Production (ISSUE-NOV5.2-O12)**: In the two-phone field test on 2026-11-06, with the Nov6.3 apps, 11 requests reached the Sensor through the production relay and 11 chunks returned, one per request, within about 2 seconds of reconnecting. Backfill routing works on production, which it could not do before v6.044. **PASSED.**

## ✅ Resolved Issues (Nov6.3)
- **Real Relay Status Text (ISSUE-NOV5.0-O5)**: The Monitor Setup dialog always said `Relay Status: Connected / Active`, even with `SRV` red. It now shows `HudState.relayStatusText`, built from `isRelayConnected`, the field that drives `SRV`, so the two cannot disagree. The label `Connected Tracker ID:` also claimed a connection while showing only the stored pairing, and is now `Tracker ID:`. Verified by 156 of 156 automated tests. `RelayStatusDialogTest` renders the real `MonitorScreen`, and both of its tests failed when the old text was put back. On the A15 through the production relay, with the dialog open, the text changed to `Disconnected` within 5 seconds of cutting mobile data and back within 5 seconds of restoring it. **PASSED.**

## ✅ Resolved Issues (Nov6.2)
- **Bounded Trail Storage (ISSUE-NOV5.2-O14)**: Each tracker now keeps its newest 50,000 trail rows, about 4.4MB, in place of unlimited growth. `TrailPruningWorker` runs about every 15 minutes, scheduled from `MonitorApp`, which now supplies the `HiltWorkerFactory` the worker needs; the default WorkManager start-up is removed. The limit is by count, not age, so pruning causes no false backfill. Verified by 153 of 153 automated tests and on the installed app on the A15: 59,799 rows through a private relay, the periodic job removed exactly 9,799, telemetry continued, and no new backfill followed. **PASSED.**

## ✅ Resolved Issues (Nov6.1)
- **Real HUD Values (ISSUE-NOV5.0-O2)**: The Monitor local line now uses the phone's own battery and position fix, and the sensor line no longer falls back to 100% and 30°C. Unknown values show `--`. Local speed is dropped once its fix is 35 seconds old, because the 10m update filter sends no fix after the Monitor stops. `BatteryReading` replaces the Sensor's private battery code, and `HudState` now owns the HUD text, so `HudStateTest` checks production formatting. Verified by 145 of 145 automated tests and on the A15 against `dumpsys`. The `--` states and the 35-second limit were not observed live. **PASSED.**

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
