# Test Procedure - GPS Tracker (vNov8.5 - EXHAUSTIVE VERIFICATION BASELINE)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

---

## Chapter 0 - Operational Forensic Audit
**Status:** 📂 Moved to separate file to ensure persistence and integrity.
**Link:** [OPERATIONAL_FORENSIC_AUDIT.md](OPERATIONAL_FORENSIC_AUDIT.md)

---

## Chapter 1 - Deployment & Initial Launch
*   **1.1 Environment Reset:** Uninstall existing versions.
*   **1.2 Deployment:** Deploy via Android Studio (Nov7.8).
*   **1.3 Version Verification:** Confirm string matches `Nov2.24_PLATINUM`.
*   **1.6 Branding Audit:** Verify the "Platinum" branding and JD logos are centered.

## Chapter 2 - Setup and Configuration (Bypass Active)
*   **2.3 ID Persistence:** Modify IDs, close (X), and reopen. Confirm preservation.

## Chapter 3 - Tracker Mode & Sensor Telemetry
*   **3.1 Main Screen Completeness:** Verify HUD elements and Stationary status.
*   **3.2 Physical Sentinel & Stealth Audit (R872):** Shake device for 5s; verify ABSOLUTE STEALTH (Dark screen, silent).
*   **3.3 Sensor JNI Bridge Verification:** Confirm `Lux` or `Tilt` values update in HUD/Logs.

## Chapter 4 - Viewer Mode & Remote Sync
*   **4.2 Remote HUD Sync:** Verify Alarm manifest ONLY appears on Viewer.
*   **4.3 State Sync:** Confirm Tracker and Viewer HUD transition between states simultaneously.
*   **4.4 Dual HUD Status Lines Audit:** Verify the Monitor displays two distinct status lines: Line 1 (Local Device) with a local GPS LED in **Cyan (`#06B6D4`)**, and Line 2 (Sensor / Tracker) with a sensor GPS LED in **Green (`#10B981`)** when connected.
*   **4.5 Dual Centering Buttons Audit:** Verify map controls feature two distinct centering buttons: **`SENSOR`** (Green icon) and **`LOCAL`** (Cyan icon).
*   **4.6 Connection-Aware Auto-Centering Audit:** 
    *   *Disconnected State:* Confirm that when devices are not connected, the local line reflects local GPS status and the map automatically centers around the monitor device.
    *   *Connected State:* Confirm that when connected, each GPS LED reflects its device correctly and the map auto-centers around the known device (Sensor if both known).
    *   *User Override:* Confirm that tapping `SENSOR` or `LOCAL` buttons immediately centers the camera on the respective device and locks/follows that target.

## Chapter 5 - GNSS & Persistence Gap (Physical)
*   **5.1 GNSS Zombie:** Shield in metal box (60s). Verify `SIGNAL LOSS` and recovery.
*   **5.2 Marker Persistence:** Kill app; confirm historical trail markers persist.

## Chapter 6 - Forensic Stress & Heat Mitigation
*   **6.1 Stress Latching:** Trigger "FORENSIC STRESS TEST". Verify `JAMMER SUSPICION` and `GPS_STALL` markers.
*   **6.2 Heat Mitigation (R133):** Trigger thermal simulation; verify "Cooling Mode" and 10s sampling interval.

## Chapter 7 - Map & Geofencing (Expansion Phase)
*   **7.1 Stationary Anchor:** Verify "ANCHOR LOCKED" badge appears after IMU confidence > 90%.
*   **7.2 Geofence Breakout:** Move device > 20m from anchor. Confirm transition to "MOVING".
*   **7.3 Uncertainty Drift (R460):** Force GPS Stall; verify accuracy buffer growth.

## Chapter 8 - Alarm & Siren Logic (Authority Audit)
*   **8.1 Siren Suppression (Tracker):** Confirm NO local audio or red-screen. Automated by `SensorStealthGuardTest` (chapter 129.5).
*   **8.2 Siren Authority (Viewer):** Confirm Viewer triggers full-screen Red Alert and a siren that sounds until acknowledged (user decision, 2026-11-07; chapter 129).

## Chapter 9 - Acoustic & Tamper Escalation
*   **9.1 Acoustic Breach:** Produce noise (>85dB). Verify `ACOUSTIC_ALERT` triggers.
*   **9.2 Siren Override:** Silence active siren; verify `ACK` event is logged.

## Chapter 11 - Geofence Predictive Breach (R71)
*   **11.1 Predictive Breach:** Move device towards boundary at speed; verify alarm triggers 2.0s BEFORE breach.

## Chapter 12 - Maintenance & Pruning
*   **12.1 Database Pressure:** Trigger 100Hz Log Pressure. Verify `PruningWorker` reduces count.

## Chapter 13 - Remote Command Execution
*   **13.1 Mode Switch Audit:** Change Tracker mode from Viewer. Verify HUD updates within 5s.

## Chapter 14 - UI Performance & Ribbons
*   **14.1 Ribbon Persistence:** Toggle orientation. Confirm ribbons do not overlap.
*   **14.2 Frame Stability:** Observe Logcat for "Skipped frames" during sensor bursts.

## Chapter 15 - Power & Battery Tamper
*   **15.1 Power Tamper:** Unplug USB. Verify `POWER_TAMPER` event (±10ms).

## Chapter 16 - Network Resilience
*   **16.1 Relay Handoff:** Toggle Wi-Fi OFF. Verify `MOBILE` reconnection < 15s.

## Chapter 17 - Forensic Export
*   **17.1 Export Verification:** Verify `.json` contains hardware-IDs and scrubbed path metadata.

## Chapter 18 - System Integrity & Hardening
*   **18.1 Diagnostics Live-Refresh:** Revoke Overlay permission; verify UI updates to "DENIED".
*   **18.2 Temporal Authority:** Set Viewer time -2m; verify HUD remains Green.

## Chapter 19 - Multi-Peer Consistency
*   **19.1 Parallel Sync:** Connect two Viewers; verify simultaneous HUD updates (<2s delta).

## Chapter 20 - Hard Reboot Continuity
*   **20.1 Reboot Recovery:** Force restart device; verify `TrackerService` resumes and captures boot event.

## Chapter 21 - High-Speed Kinematics (Rule 1.322)
*   **21.1 Velocity Promotion:** Simulate > 100 km/h movement. Verify status is `MOVING` and NOT `JUMP`.

## Chapter 22 - Identity & Alias Uniqueness (R499)
*   **22.1 Alias Verification:** Modify device alias in Settings. Confirm reflected in Export/Viewer HUD.

## Chapter 23 - Power Policy & Doze Deferral (R497)
*   **23.1 Doze Entry:** Force idle; confirm telemetry log pulse persists (R497).

## Chapter 24 - Database Schema & Migration Integrity
*   **24.1 Migration Audit:** Install Nov2.24 over Nov2.23. Verify all trail markers rendered correctly.

## Chapter 25 - Map Follow & UI Performance
*   **25.1 Follow Modes:** Toggle between Tracker, Viewer, and Auto follow.
*   **25.2 Ghost Mode:** Wait 35s without telemetry; verify markers dim to `Slate500`.
*   **25.3 Pruning:** Verify polyline points do not exceed 1000.

## Chapter 26 - Alarm Suppression & Cooldowns
*   **26.1 Bootstrap Phase:** Trigger violation within 60s of launch; verify NO alarm.

## Chapter 27 - System Health Alarms
*   **27.1 Critical Battery:** Simulate 9% battery; verify alert.
*   **27.2 High Temp:** Trigger thermal event; verify `High Temp` alert.

## Chapter 28 - Advanced Tamper Escalation
*   **28.1 Shock & Lift:** Simulate >0.8G shock or >0.8m lift. Verify triggers.
*   **28.2 Proximity:** Trigger proximity breach; verify alert.

## Chapter 29 - Connectivity Alarms (Non-Critical)
*   **29.1 Signal/Relay Loss:** Disable Wi-Fi; verify notification-only alert (No siren).

## Chapter 30 - Map Settings & Persistence (R800)
*   **30.1 Tools Overlay:** Toggle purple gear; verify Map Tools visibility.
*   **30.2 Back = Commit:** Change settings and exit via Back; verify re-init and commit.

## Chapter 31 - Accessibility & Screen Reader (R69)
*   **31.1 TalkBack Audit:** Navigate HUD with TalkBack; ensure content descriptions reflect live telemetry.

## Chapter 32 - Display Scaling & Density (R67)
*   **32.1 Dynamic Font Scaling:** Increase font to "Largest"; verify no HUD clipping or overlap.

## Chapter 33 - Map Follow Mode Transitions (R1.61)
*   **33.1 Camera Actions:** Verify camera centering via `SharedFlow<CameraAction>` during mode switch.

## Chapter 34 - Siren Z-Index & System-Wide Overlay (Issue #1402)
*   **34.1 Overlay Authority:** Launch another app; trigger siren. Verify Red Alert obscures third-party app.

## Chapter 35 - Forensic Chain of Custody (R66)
*   **35.1 Log Integrity:** Verify exported JSON contains hardware-ID hashes and scrubbed metadata.

## Chapter 36 - Proximity Pocket Rejection (R86)
*   **36.1 Cross-talk Audit:** Cover sensor with cloth; verify engine rejects false-positive "Near" triggers.

## Chapter 37 - Light Sensor Blinding Recovery (R85)
*   **37.1 Saturation Recovery:** Expose sensor to direct sunlight; verify recovery within 2s.

## Chapter 38 - Input Validation & Relay Sanitization (R91)
*   **38.1 URL Injection:** Attempt malformed Relay URLs; verify SettingsRepository sanitizes input.

## Chapter 39 - Forensic Buffer Rollover (R83)
*   **39.1 Wrap-around Logic:** Generate 10,000+ entries; verify rollover prevents corruption.

## Chapter 40 - Multi-Viewer Payload Stress (R25)
*   **40.1 Parallel Load:** Connect 5 Viewers; verify RTT < 500ms for all.

## Chapter 41 - Dark Mode Contrast Audit (R51)
*   **41.1 Readability:** Toggle Dark Mode; verify telemetry ribbons remain legible.

## Chapter 42 - Landscape Layout Stability (R52)
*   **42.1 Orientation Audit:** Rotate to Landscape; verify HUD ribbons do not overlap map controls.

## Chapter 43 - Barometric Lift Precision (R76)
*   **43.1 Lift Verification:** Raise device 1m rapidly; verify `LIFT_ALERT` triggers before GPS.

## Chapter 44 - Magnetometer & Compass Interference (R87)
*   **44.1 Interference Rejection:** Place near electronics; verify compass heading remains stable.

## Chapter 45 - Thermal Throttling Hysteresis (R88)
*   **45.1 Boundary Stability:** Maintain temp at 46°C; verify "Cooling Mode" does not oscillate.

## Chapter 46 - WorkManager Expedited Execution (R31)
*   **46.1 Android 14+ Policies:** Verify Maintenance workers execute as "Expedited".

## Chapter 48 - Activity Recognition Motion Stability (R36)
*   **48.1 False-Positive Rejection:** Shake device while in a vehicle; verify status remains `MOVING`.

## Chapter 49 - Charging State Sampling Acceleration (R39)
*   **49.1 Power Connected:** Plug in charger; verify telemetry sampling rate accelerates.

## Chapter 50 - Maintenance Worker Periodic Integrity Check (R40)
*   **50.1 Background Audit:** Ensure background cleanup tasks run successfully every 15 minutes.

## Chapter 51 - Denied Microphone Recovery (Rule 1.15)
*   **51.1 Revocation Audit:** Revoke Microphone permission at runtime. Verify HUD shows "Audio Denied".

## Chapter 52 - Denied Overlay Recovery (Rule 1.16)
*   **52.1 Revocation Audit:** Revoke Overlay permission in background. Verify prompt upon relaunch.

## Chapter 53 - History Ribbon Aggregation (1h Audit)
*   **53.1 Aggregation Accuracy:** Track for 1 hour. Verify polyline points match raw entries.

## Chapter 54 - Foreground Service Sticky Restart
*   **54.1 OS Recovery Audit:** Force kill app; verify OS restarts Foreground Service within 60s.

## Chapter 55 - WebSocket Reconnection Backoff
*   **55.1 Relay Downtime Audit:** Disable Relay; confirm app uses exponential backoff with jitter.

## Chapter 56 - Protocol Version Mismatch
*   **56.1 Relay Safety Audit:** Simulate unsupported protocol version. Verify user notification.

## Chapter 57 - Large Payload Fragmentation
*   **57.1 Log Sync Audit:** Sync 500+ entries. Ensure payload is fragmented.

## Chapter 58 - Signature Collision Resilience
*   **58.1 Deduplication Audit:** Inject duplicate entries. Verify rejection by `LogRepository`.

## Chapter 59 - High-Contrast Mode Accessibility
*   **59.1 Contrast Audit:** Enable System High-Contrast; verify HUD readability.

## Chapter 60 - Overlay Transparency (Simulated Sunlight)
*   **60.1 Visibility Audit:** Verify HUD visibility under max brightness.

## Chapter 61 - Stationary Drift Mitigation (Urban Multipath)
*   **61.1 Anchor Stability:** Simulate 5m GPS drift. Anchor must remain locked if IMU > 90%.

## Chapter 62 - Jump Engine Tier 3 Suppression
*   **62.1 Signal Bounce Audit:** Simulate 50m "Jump". Verify rejection by Sentinel Logic.

## Chapter 63 - Adaptive Vibration Floor Convergence
*   **63.1 Floor Audit:** Observe floor for 300s. Verify convergence without oscillation.

## Chapter 64 - Acoustic Duty Cycle Latency
*   **64.1 Power-Save Audit:** Verify no missed breaches during microphone cycling.

## Chapter 65 - High-Memory Pressure Service Restart
*   **65.1 LMK Restoration:** Simulate Low Memory Kill (LMK). Verify `sequenceId` continuity.

## Chapter 66 - Database Transaction Atomicity
*   **66.1 Power Loss Audit:** Crash during DB write. Verify no corrupted entries.

## Chapter 67 - Write-Ahead-Log (WAL) Performance
*   **67.1 Burst Latency Audit:** Trigger 100Hz burst. Latency must remain < 10ms.

## Chapter 68 - Indexing Speed (50k+ History Points)
*   **68.1 Scale Audit:** Populate 50,000 points. Rendering/queries must complete in < 2s.

## Chapter 69 - WorkManager Worker Chaining
*   **69.1 Maintenance Audit:** Verify `MaintenanceWorker` triggers `PruningWorker`.

## Chapter 70 - Hilt Lifecycle during Process Death
*   **70.1 Restoration Audit:** Verify Hilt dependencies re-injected correctly after process death.

## Chapter 72 - Local Log Encryption Performance
*   **72.1 Overhead Audit:** measure latency with encryption. Overhead must be < 5ms.

## Chapter 73 - Permission Revoking in Foreground
*   **73.1 Immediate Halt Audit:** Revoke Location. Engine must stop immediately.

## Chapter 74 - Log Stripping Regex Bench
*   **74.1 CPU Usage Audit:** Measure load during log cleaning. Must consume < 5% CPU.

## Chapter 75 - UI State Flow Emission Frequency
*   **75.1 Throttle Audit:** Verify aggregation at maximum of 2Hz.

## Chapter 76 - Samsung DOZE_SUSPEND Flicker Audit (R290)
*   **76.1 Display Noise Audit:** Enable Samsung deep sleep; verify HUD ribbons do not flicker or ghost.

## Chapter 77 - A15 GNSS Hysteresis Cooling (R274)
*   **77.1 Cooling Stability:** Rapid temp drop; verify 10s cooling window before signal re-acquisition.

## Chapter 79 - Forensic Trace Scrubbing (R779)
*   **79.1 Metadata Sanitization:** Verify absolute removal of path/HW-ID metadata in forensic logs.

## Chapter 81 - Signaling Tick-Loop Resumption (R314)
*   **81.1 Tick-Loop Audit:** Verify proactive loop initiation on every peer pulse receipt.

## Chapter 82 - Accuracy Window Buffer Filling (R316)
*   **82.1 State Restoration:** Verify restoration logic fills accuracy buffer during load.

## Chapter 83 - Main-Thread Stall Watchdog (R33)
*   **83.1 ANR Recovery:** Block main thread for 5s; verify automatic engine recovery.

## Chapter 85 - Side-Effect Persistence Authority (R492)
*   **85.1 Persistence Routing:** Verify role-agnostic routing for all telemetry side-effects.

## Chapter 86 - Remote Integrity Prefix Shielding (R493)
*   **86.1 State Shielding:** Verify evaluation states are shielded from remote peer visibility.

## Chapter 87 - Activity-Scoped Routing Unification (R494)
*   **87.1 ViewModel SSOT:** Audit activity-scoped navigation for stability during configuration changes.

## Chapter 88 - Hydration Staggering Determinism (R495)
*   **88.1 Startup Order:** Verify repository hydration order under CPU/IO saturation.

## Chapter 90 - Unified Doze-Emergency Override (R497)
*   **90.1 Sleep Wakeup:** Verify priority geofence alarms wake the device from deep sleep.

## Chapter 91 - Hardware Poke Boundary Precision (R498)
*   **91.1 Boundary Matching:** Audit inclusive matching logic for sensor polling thresholds.

## Chapter 93 - Programmatic 24h Soak Simulation (R500)
*   **93.1 Soak Stability:** Verify zero forensic loss over 24-hour simulation cycle.

## Chapter 95 - Foreground Notification Action Life (R41)
*   **95.1 Action Persistence:** Verify "Stop" action works after process death/sticky restart.

## Chapter 96 - Map Follow Mode Transitions (R1.61)
*   **96.1 Camera Centering:** Audit camera centering via `SharedFlow<CameraAction>` in all modes.

## Chapter 97 - Marker Pooling Efficiency (Issue #147)
*   **97.1 Heap Stability:** Verify memory stability during 50-marker violation bursts.

## Chapter 98 - Geofence Uncertainty Growth (Issue #431)
*   **98.1 Radius Caps:** Audit radius cap at 33.3m/s during signal loss or stall.

## Chapter 99 - Settings Wheel Layout (R1.62)
*   **99.1 Orientation Metrics:** Verify orientation-aware height metrics for all Map UI overlays.

## Chapter 100 - Final SOT Compliance Check (R45)
*   **100.1 Requirement Parity:** Final verification that system state matches all requirements in SOT_MASTER_REQUIREMENTS.md.

## Chapter 101 - Manual Tracker ID Customization & Version Display (Nov3.21)
*   **101.1 Manual Device ID Entry:** Verify that users can manually set custom device IDs (any non-blank sequence of characters, including single-character IDs) via the Sensor configuration settings overlay and Monitor pairing wizard.
*   **101.2 Version Name Display:** Verify that the application version name is visible on the first screen of both the Sensor (`AdminActivity`) and Monitor (`MonitorActivity`).

## Chapter 102 - Telemetry Session Activation & Parameter Configuration (Nov3.21)
*   **102.1 Telemetry Session Initialization:** Verify that pairing a Tracker ID in the Monitor app automatically initiates `MonitorSessionService` and connects to the signaling relay, transitioning from "Waiting for Telemetry..." to live trail rendering.
*   **102.2 Parameter & ID Configuration:** Verify setting Viewer/Tracker IDs in the Pairing Wizard and configuring Custom IDs / Factory Defaults via the Sensor Settings Gear overlay.

## Chapter 103 - Default Tracker & Monitor ID Verification (Nov4.0)
*   **103.1 Default Tracker ID ("T"):** Verify that the Sensor generates Default Tracker ID `"T"` upon identity generation/reset, and that the Monitor pairing wizard pre-fills `"T"` in the Tracker ID text field.
*   **103.2 Default Monitor ID ("V"):** Verify that the Monitor generates Default Monitor ID `"V"` upon identity generation/reset.

## Chapter 104 - Advanced Relay Partition Tolerance & Telemetry Edge Synchronization (Nov4.3)
*   **104.1 Partition Tolerance Sequence Deduplication:** Verify that `SignalingQueue` correctly deduplicates incoming sequence IDs and tags partitioned/buffered updates with `isBuffered = true` during network disconnection and flapping.
*   **104.2 Edge Telemetry Buffering & Batch Polling:** Verify robust backpressure handling, batch polling, and re-queueing of pending telemetry batches upon reconnection without data loss or duplication.

## Chapter 105 - Cloud Relay Server Revival & Default OnRender Relay URL Configuration (Nov4.21)
*   **105.1 Cloud Relay Server Health & Persistence:** Verify that `relay-server/index.js` responds successfully to HTTP health checks (`200 OK`) and maintains persistent device state caching via `device_cache.json`.
*   **105.2 Default OnRender Relay URL Fallback:** Verify that `MonitorSessionService` defaults to `https://gps-survival-relay.onrender.com` when no custom relay URL is configured in DataStore, enabling seamless out-of-the-box cross-network communication between physical devices (`A15` and `S21FE`).

## Chapter 106 - HUD Integration & Status LED Verification (Nov4.22-addHUD)
*   **106.1 Relay Server Connection LED (SRV):** Verify that the SRV status badge displays active green when connected to the signaling relay server (`SignalingClient.isConnected = true`) and inactive red when disconnected, across both Monitor and Sensor applications.
*   **106.2 Local GPS Fix LED (GPS):** Verify that the GPS status badge displays active green when local device location fix is active and fresh (<35s) and inactive red when stale or unavailable, on both devices.
*   **106.3 Peer Activity & Device Link LED (TRK / VWR):** Verify that the Monitor app displays active green TRK badge when target telemetry is received within 35s, and the Sensor app displays active green VWR badge when signaling link to viewer is active.
*   **106.4 Data Integrity & Watchdog LEDs (DAT / WDG):** Verify that the DAT badge displays active green when INT, SRV, and Peer links are all active, and WDG badge reflects active background service health.

## Chapter 107 - Relay Server URL Setup & Configuration Option (Nov4.27)
*   **107.1 Sensor Relay URL Configuration:** Verify that users can view and edit the Relay Server URL in `SettingsOverlay` (`AdminActivity.kt` / `:app-sensor`), apply custom URLs, and reset to default (`https://gps-survival-relay.onrender.com`), and that `SensorViewModel` and `SensorService` reactively reconnect `SignalingClient` upon changes.
*   **107.2 Monitor Relay URL Configuration:** Verify that Monitor app users can view and configure the Relay Server URL during pairing (`PairingWizard`) and via the Setup & Info dialog (`MonitorScreen`).

## Chapter 108 - Pre-Revolution Map Functionality & Specifications (Nov4.28)
*   **108.1 Map Tools Overlay & Settings Wheel:** Verify `MapSettingsToggle` toggle button and `MapToolsOverlay` quick action tools (`IN`/`OUT`, `TRACKER`/`VIEWER`, `JUMP`/`OUT`, `SAVE`/`LOAD`, `ADD`/`DEL`/`FENCE`, `CLEAR`).
*   **108.2 Dual Trail Management & Export/Import:** Verify independent polyline rendering for Tracker and Viewer trails, trail clearing, and manual JSON trail export (`SAVE`) / import (`LOAD`).
*   **108.3 Home Points & Geofence Boundary Circles:** Verify Home Point marker display, tap-to-add (`ADD`), marker-tap-to-remove (`DEL`), async geofence boundary circles (`Polygon.pointsAsCircle`), and fence visibility toggle (`FENCE`).
*   **108.4 Spatial Violation Markers & Accuracy Buffers:** Verify purple/pink Jump alert markers (`JUMP`), red Out-of-Range geofence breach markers (`OUT`), independent visibility toggles, and uncertainty accuracy circles.

## Chapter 109 - Alarming Setup & Alert Management Audit (Nov4.30)
*   **109.1 Alert Management Overlay & Bulk Controls:** Verify that users can open `AlertManagementOverlay` from Sensor settings or Monitor setup, toggle master Global Mute, configure individual alert categories (Communication, Geofence/Location, Power/Device Health, Storage Integrity, and Sentinel Motion Sensors such as Tamper, Tilt, Acoustic, and Lift with vibration/tilt sensitivity sliders), and use `ALL ON` / `RESET ALL` bulk actions.
*   **109.2 Siren Sound & Audio Setup Overlay:** Verify that users can open `AlarmSoundOverlay`, test siren audio output (`TEST SIREN AUDIO` / `STOP`), select siren sound wave types (`Siren`, `Chimes`, `Pulse`), configure audio behaviors (haptic vibration, override Silent/DND, force max volume), and adjust custom app volume mode and volume slider levels.
*   **109.3 DataStore Persistence:** Verify that `AlertSettings` and siren type selections persist across app restarts and reboots via DataStore.

## Chapter 110 - Device-Specific Setup & Hardware Hardening Audit (Nov4.31)
*   **110.1 Hardware Capabilities & Performance Profiling:** Verify `HardwareCapabilities` correctly profiles device manufacturer and model characteristics, automatically assigning `PerformanceTier.STAGGERED` for budget tier hardware (such as Samsung A15 SM-A155F) and `PerformanceTier.STANDARD` for flagship/standard hardware (such as Samsung S21FE).
*   **110.2 Vendor Power Management & WakeLock Escalation:** Verify `DeviceHardeningStrategy` enforces vendor-specific power management policies, wake lock renewals, and watchdog scheduling for Samsung and Huawei devices.
*   **110.3 Background Transition Hardening Window:** Verify that `DeviceHardeningStrategy` detects app background transitions and applies the 60-second background escalation window to eliminate jitter spikes on staggered tier devices.

## Chapter 111 - System Tools & Diagnostics Menu Audit (Nov4.32)
*   **111.1 Tools Option Under Top Menu Bar:** Verify that a dedicated **Tools** option (refresh/tools icon button) is positioned directly under the top menu bar alongside the Setup (`Build`) and Settings (`Settings`) gear icons in both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications.
*   **111.2 System Tools & Diagnostics Overlay:** Verify that tapping the Tools option opens `ToolsOverlay` with quick action buttons:
    - **Trigger High-Frequency Burst (100Hz)**
    - **Prune Database & Reclaim Cache**
    - **Trigger Test Alarm / Violation**
    - **Reset Service Watchdog**
*   **111.3 Action Dispatch & Response:** Verify that activating any tool action dispatches the appropriate logging/maintenance event and can be successfully closed via the `CLOSE (X)` button.

## Chapter 112 - Revived Sound & Setup Options Audit (Nov4.33)
*   **112.1 Clean Setup Overlay & Maintenance Actions:** Verify that users can open `CleanSetupOverlay` from settings in both Sensor and Monitor apps, and perform maintenance actions including clearing home points/geofence, resetting operational statistics, and triggering full system re-initialization.
*   **112.2 Phone Setup & Capabilities Guide Overlay:** Verify that users can open `PhoneSetupOverlay` to inspect device manufacturer/model profiling, guided permission check steps (Location, Battery Optimization, Overlay permission, Exact Alarms, Notifications, Home Points), and launch app info, battery settings, permission dialogs, diagnostics, and remote alarm test.
*   **112.3 Sound & Alert Setup Integration:** Verify full integration and accessibility of `AlarmSoundOverlay` and `AlertManagementOverlay` from configuration menus in both Sensor and Monitor applications.

## Chapter 113 - Upper Menu Line Expansion Audit (Nov4.34)
*   **113.1 Expanded Quick Navigation Icons:** Verify that the upper menu line across both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications includes direct one-tap navigation buttons for Setup (`Build`), Settings (`Settings`), Tools (`Refresh`), Map (`Place`), Events (`Menu`), Graphs (`Star`), and Info (`Info`).
*   **113.2 Action Dispatch:** Verify that tapping each upper menu action correctly dispatches the intended navigation event, opens the corresponding overlay/dialog, or logs telemetry feedback.

## Chapter 114 - Monitor Hardware Deployment, UI Single-Line Fitting & HUD Telemetry Formatting Audit (Nov4.35)
*   **114.1 Hardware Deployment & Live Telemetry Monitoring:** Verify deployment of `:app-monitor` to target hardware (Samsung S21FE / connected device R58X40GV2AR) and verify live logcat monitoring and telemetry status handling.
*   **114.2 Upper Menu Header Single-Line Fitting:** Verify that the upper menu header bar (`Surface` / `Row`) across both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications fits title text (`MONITOR: ...` / `SENSOR: ...`) and all 7 action buttons (Setup, Settings, Tools, Map, Events, Graphs, Info) and Disconnect button without text wrapping or layout distortion.
*   **114.3 HUD Telemetry Formatting & Text Ellipsization:** Verify that `HudStatusBar` row 2 metadata formatting correctly preserves telemetry status text (`B:100%|T:30°C|A:...|AGE:0s`) without truncating the age parameter, and applies single-line ellipsization (`TextOverflow.Ellipsis`) to device identity strings.

## Chapter 115 - Dual Device Hardware Deployment & Telemetry Logcat Audit (Nov4.36)
*   **115.1 Dual Device Simultaneous Deployment:** Verify concurrent deployment of `:app-monitor` to Samsung S21FE (`R5CRC14PG4F`) and `:app-sensor` to Samsung A155 (`R58X40GV2AR`).
*   **115.2 Sensor Service Telemetry Generation:** Verify that `SensorService` on A155 continuously generates and queues telemetry updates (`REV_SensorService: Telemetry sent. Seq: ... | Interval: 10000ms`) without memory leaks or thread exhaustion.
*   **115.3 Monitor UI & Event Log Verification:** Verify that `MonitorActivity` on S21FE maintains stable UI rendering, handles back navigation events cleanly, and logs telemetry events to system logcat.

## Chapter 116 - Decoupled Peer LED Signaling & Relay-Independent Status Audit (Nov4.37)
*   **116.1 Server Connection LED (SRV):** Verify that the `SRV` status badge on both Monitor and Sensor applications displays active GREEN ONLY when connected to the Relay Server (`SignalingClient.isConnected = true`), and displays RED otherwise.
*   **116.2 Tracker Peer Connection LED (TRK):** Verify that the `TRK` status badge on the Monitor application displays active GREEN ONLY when connected to the TRACKER (`T`), independent of relay server connection state, and displays RED otherwise.
*   **116.3 Viewer Peer Connection LED (VWR):** Verify that the `VWR` status badge on the Sensor application displays active GREEN ONLY when connected to a VIEWER (`V`), independent of relay server connection state, and displays RED otherwise.
*   **116.4 Peer Connection & Alarm Event Test Evaluation Rule:** Verify test evaluation criteria for peer connection tests:
    - *Therefore, if there is an active connection — the test PASSED.*
    - *Else if there is an alarm or at least an event logged — the test PASSED OK.*
    - *Else — the test FAILED.*

## Chapter 118 - HUD Direction, Compact Spacing & Real-Position Map Centering (Nov5.0)
*   **118.1 HUD Left-To-Right Authority:** Verify that `HudStatusBar` keeps left-to-right layout and text order in both Sensor and Monitor applications under an RTL system language (`INT` left of `WDG`, row 2 reads `B:...|T:...|A:...|AGE:...`). Automated by `HudLayoutAuditTest`.
*   **118.2 Real-Position Map Centering:** Verify that the Monitor map centers only on real device positions: the local device when disconnected, the sensor when connected, and the locked device after `SENSOR` / `LOCAL` is tapped, including `SENSOR` while the peer is inactive. Automated by `MapCenterResolverTest`.
*   **118.3 Compact HUD Line Spacing:** Verify that the Monitor HUD uses compact line spacing with no clipped text, and that compact height is under 60% of the standard height. Automated by `HudLayoutAuditTest`.
*   **118.4 No-Position Behavior:** Verify that with location permission denied or no fix available, the map shows the world view and never a hardcoded city, then moves to device zoom on the first real position.
*   **118.5 Viewer Presence Signaling (`VWR`):** Verify that `SocketioSignalingClient` consumes `viewer_status_relay`, so the Sensor `VWR` LED turns GREEN when a viewer shares the room and RED when it leaves. Automated by `SignalingBatchTest.testViewerStatusFollowsRelayPresence`.
*   **118.6 Telemetry Payload Integrity:** Verify that a pooled `LocationUpdate` released immediately after `sendTelemetry` still reaches the relay intact, online and through the offline queue, and that position, timestamp, sequence, battery, charging, temperature, accuracy, and speed all survive the round trip. Automated by `SignalingBatchTest.testRecycledUpdateIsSentIntactAndRoundTrips` and `testRecycledUpdateSurvivesOfflineQueue`. On hardware, confirm that the Monitor shows the Sensor's real battery and a live `AGE`, with `TRK` GREEN.
*   **118.7 No-Fix Trail Filtering:** Verify that `(0,0)` trail rows are never drawn, so the polyline, marker, and map center use real positions only, while points on the equator or prime meridian are kept. Automated by `MapCenterResolverTest`.
*   **118.8 Relay URL Hot Switch:** Verify that changing the relay URL while connected closes the old socket and connects to the new relay without an app restart, and that reconnecting to the same relay keeps the existing socket. Automated by `SignalingBatchTest.testRelayUrlChangeReplacesTheSocket`.

## Chapter 119 - Default Relay Address & Production Relay Verification (Nov5.1)
*   **119.1 Single Default Relay Address:** Verify that both apps take the default relay address from `RelayConfig.DEFAULT_URL` (`https://gps-survival-relay.onrender.com`), keep a saved custom URL, and use the default when the saved URL is missing or blank. Automated by `RelayConfigTest`.
*   **119.2 Production Relay Link:** With Sensor on A15 and Monitor on S21FE using the default relay, verify that `SRV` turns GREEN on both devices, including recovery after a relay cold start longer than the 20s connection timeout.
*   **119.3 Production Telemetry:** Verify that the Monitor shows `TRK` GREEN, the Sensor's real battery, and a live `AGE`, and that the Sensor shows `VWR` active.

## Chapter 120 - Runtime Resilience & Display Limits (Nov5.2)
*   **120.1 Sequence Continuity:** Verify that the Sensor continues its sequence numbers after a crash or reboot instead of restarting at 0. Since Nov6.0 a restart can skip up to 100 numbers (see 121.3). Automated by `SequenceContinuityTest`; on hardware, crash the Sensor and compare the sequence before and after.
*   **120.2 Wake Lock Renewal:** Verify that the Sensor still holds its wake lock after the original 10 minutes, and that telemetry continues during forced Doze.
*   **120.3 Trail Cap:** Verify that the map receives at most the newest 1000 trail points. Automated by `TrailCapTest`.
*   **120.4 UI Update Rate:** Verify that a 10Hz telemetry stream updates the Monitor trail at no more than 2Hz. Automated by `UiRateTest`.
*   **120.5 Relay URL Validation:** Verify that malformed relay URLs cannot be saved in either app, and that a stored malformed URL falls back to the default without a crash. Automated by `RelayConfigTest`.
*   **120.6 Monitor Menu Visibility:** Verify that all eight Monitor menu actions stay visible with long IDs. Automated by `HudLayoutAuditTest.menuBarKeepsEveryActionVisibleWithLongIds`.
*   **120.7 Backfill Round Trip:** After a network outage, verify that a Monitor gap request reaches the Sensor through the relay and that a response returns (`Received request`, then `Received backfill chunk`). Payload routing is automated by `SignalingBatchTest.testBackfillResponseIsRoutableAndKeepsItsOwner`.
*   **120.8 Instrument Font Cap:** With the system font at its largest, verify that HUD speed and map button labels stay visible. Automated by `HudLayoutAuditTest.speedStaysVisibleAtTheLargestSystemFont`.
*   **120.9 Paired Device Isolation:** After re-pairing the Monitor to a different ID, verify that the sensor HUD line shows no values from the previous device. Automated by `PairedTelemetryTest`.

## Chapter 121 - Forensic Entry Integrity (Nov6.0)
*   **121.1 Interrupted Write:** Verify that an entry left half written or without its checksum is skipped, and that all other entries read correctly. Automated by `ForensicEntryIntegrityTest.interruptedWriteOverTheOldestEntryIsSkipped` and `checksumWrittenLastMeansAFullyCopiedButUncheckedEntryIsSkipped`.
*   **121.2 Power Loss:** Verify that a header claiming entries that never reached storage does not produce invented entries. Automated by `ForensicEntryIntegrityTest.headerClaimingEntriesThatNeverReachedStorageIsHandled`.
*   **121.3 No Sequence Reuse:** Verify that every sent number is below the reservation stored in the header, so a restart after power loss never reuses one. A restart can skip up to 100 numbers; the Monitor sends one backfill request for the gap and receives an empty reply. Automated by `sequenceNumbersAreNotReusedAfterPowerLoss` and `everyWrittenNumberIsBelowTheStoredReservation`.
*   **121.4 Damaged Header:** Verify that an out-of-range or partly updated header is rebuilt from the entries, in order, and that writing continues. A reset after an unknown version must be stored in the file. Automated by `outOfRangeWriteIndexIsRebuiltAndWritingContinues`, `headerTornBetweenIndexAndCountIsRebuilt`, `headerTornAtTheWrapPointIsRebuiltInOrder`, and `resetAfterUnknownVersionIsStoredInTheFile`.
*   **121.5 Upgrade From Version 2:** Install the new Sensor over an older one with `adb install -r`. Verify that the file reaches version 3, the entries remain, and numbering continues. Automated by `versionTwoFileKeepsItsEntriesAndSequenceAfterUpgrade` and `partlyFilledVersionTwoFileDoesNotAdoptUnusedSlots`.
*   **121.6 Older Android Compatibility:** Verify that `ForensicBuffer` calls no method missing below API 35. Automated by `ForensicBufferApiLevelTest`, which reads the compiled class. On a device with API 34 or lower, verify that the Sensor starts and writes telemetry.
*   **121.7 Process Kill:** While telemetry runs, kill the Sensor process with `kill -9`. Verify that it restarts by itself, logs no damaged entries, and that the stored reservation advances.

## Chapter 122 - Real HUD Values (Nov6.1)
*   **122.1 Local HUD Line:** On the Monitor, verify that the local line shows the phone's own battery, charging state, temperature, fix accuracy, and fix age, and compare them with `dumpsys battery` and `dumpsys location`. Automated by `FixReadingTest.usesBatteryAndFixValues` (`MonitorHudValuesTest` until Nov7.1).
*   **122.2 Battery Reading:** Verify that a level or temperature the system does not report stays unknown instead of becoming 100% or 30°C, in both apps. Automated by `BatteryReadingTest`.
*   **122.3 Unknown Values:** With no fix, or before any telemetry, verify that the affected HUD values show `--` and no number, and that a real 0 still shows as a number. Automated by `HudStateTest` and `FixReadingTest.noFixShowsNoPositionValues`.
*   **122.4 Old Fix:** Stop moving with the Monitor. Verify that after 35 seconds without a new fix, speed and motion state show `--`, while accuracy and age stay. Automated by `FixReadingTest.oldFixKeepsItsAccuracyAndAgeButNotItsSpeed`.

## Chapter 123 - Monitor Trail Storage Bound (Nov6.2)
*   **123.1 Row Cap:** Verify that each tracker keeps only its newest 50,000 trail rows, that one row over the limit removes only the oldest row, and that a tracker at or under the limit loses nothing, even when another tracker is over it. Automated by `TrailStorageBoundTest.workerKeepsTheNewestRowsOfEachTrackerAtTheRealLimit`, `rowCountAtTheLimitIsLeftAlone`, and `oneRowOverTheLimitRemovesOnlyTheOldest`.
*   **123.2 Sequence Safety:** Verify that pruning does not change the last sequence number, and that an idle tracker keeps old rows under the limit, so the next update requests no false backfill. Automated by `pruningDoesNotMoveTheLastSequence` and `oldRemoteRowsSurviveWhenUnderTheLimit`.
*   **123.3 Local Rows:** Verify that local user rows older than 48 hours are deleted and newer ones stay. Automated by `localRowsExpireAfter48Hours`. No code writes local rows yet (ISSUE-NOV6.2-O29).
*   **123.4 Scheduling:** Verify that starting the Monitor registers one periodic job, and that repeated starts do not add another. Automated by `schedulingTwiceKeepsOnePeriodicJob`. On hardware, confirm the job in `dumpsys jobscheduler` and `Worker result SUCCESS` in logcat, which also proves that `HiltWorkerFactory` creates the worker.
*   **123.5 Pruning On Hardware:** Load more than 50,000 rows into the installed Monitor through a private relay, never the production relay. Wait for the periodic run and verify `Trail cleanup finished. Rows removed: N` in logcat, then verify that live telemetry continues and requests no new backfill.

## Chapter 124 - Real Relay Status Text (Nov6.3)
*   **124.1 Status Follows The Relay:** Open the Monitor Setup dialog. Verify that it shows `Relay Status: Connected` while `SRV` is on and `Relay Status: Disconnected` while `SRV` is off, including a change while the dialog stays open. Automated by `HudStateTest.relayStatusTextFollowsTheSrvState` and `RelayStatusDialogTest`.
*   **124.2 Relay Lost On Hardware:** With the dialog open on the A15, cut the network. Verify that the status changes to `Disconnected` together with `SRV`, and returns to `Connected` after the network returns.

## Chapter 125 - Backfill Gap Recovery (Nov6.4)
*   **125.1 Holes Come From The Table:** Verify that every hole between stored rows is listed, newest first, ignoring other trackers and local rows, and that a scan of a full table stays under one second. Automated by `BackfillGapRecoveryTest.gapsAreListedNewestFirst`, `aContinuousTrailHasNoGaps`, `otherTrackersAndLocalRowsAreIgnored`, and `scanningTheFullTableStaysFast`.
*   **125.2 More Holes Than The Limit:** With more holes than the 10-request limit, verify that only 10 requests are in flight, and that every hole is refilled as slots free. Automated by `everyHoleIsRefilledWhenThereAreMoreThanTheLimit` and `BackfillLedgerTest.gapsOverTheLimitWaitAndAreRequestedWhenASlotFrees`.
*   **125.3 Failed And Unanswered Requests:** Verify that a request that could not be sent is made again by a later scan, and that an unanswered request is released after 30 seconds without a chunk and asked again. Automated by `aFailedRequestIsNotForgotten`, `BackfillLedgerTest.anUnansweredRequestIsReleasedAndAskedAgain`, and `eachChunkRestartsTheTimeout`.
*   **125.4 Unfillable Holes:** Verify that a hole the Sensor answers without rows is not requested again in the same process, while a new hole still is. Automated by `aHoleTheSensorCannotFillIsAskedOnce` and `BackfillLedgerTest.anAnsweredHoleIsNotAskedForever`.
*   **125.5 Reconnect Burst:** Verify that a flushed queue and a full backfill reach a busy collector with nothing dropped. Automated by `SignalingBatchTest.testReconnectBurstReachesABusyCollector` and `testBackfillBurstReachesABusyCollector`.
*   **125.6 Missing Start:** Verify that a fresh pairing requests the rows before its first update, and that a table pruned to its cap does not request its deleted rows. Automated by `aMissingStartIsRequestedOnAFreshPairing` and `aPrunedStartIsNotRequested`.
*   **125.7 Outage On Hardware:** Through a private relay, never the production relay, cut only the Sensor for 60 seconds at the 10Hz charging rate. Pull the Monitor database after recovery and verify that no hole remains. Repeat with the Monitor on a second phone through the production relay when one is available.

## Chapter 126 - Real Version Strings (Nov7.0)
*   **126.1 No Version In Code:** Verify that no string in the production Kotlin sources of any module holds a release version, and that comments may still name one. Automated by `VersionLiteralGuardTest`. Run it with `--rerun`, because Gradle does not track the scanned files as inputs of the test.
*   **126.2 Version From The Package:** Verify that the shown version is the installed `versionName`, and that a package that cannot be read shows `--` and no invented version. Automated by `AppVersionTest`.
*   **126.3 Version On Hardware:** Install both apps on the A15. Verify that the Sensor first screen, the Sensor Setup dialog, the Monitor pairing screen, and the Monitor Setup dialog all show the `versionName` from the build file, and that the Sensor logs `onCreate called. Version:` with that same version when its service starts.

## Chapter 127 - Unpair Confirmation (Nov7.1)
*   **127.1 Every Unpair Control Asks First:** On the paired Monitor screen, tap the `Settings / Re-pair` gear, the `Disconnect` X, and each of the three `CLEAN` actions in turn. Verify that each shows `Unpair from tracker <id>?` and that the pairing is not cleared. Automated by `UnpairConfirmationTest.gearAsksBeforeUnpairing`, `disconnectAsksBeforeUnpairing`, and `cleanActionsAskBeforeUnpairing` (ISSUE-NOV5.2-O15).
*   **127.2 Cancel And Confirm:** Verify that `Cancel` closes the dialog and keeps the pairing, and that `Unpair` clears it exactly once and returns to the pairing screen. Automated by `cancelKeepsThePairing` and `confirmUnpairsOnce`.
*   **127.3 Unpair On Hardware:** On the A15, tap the gear and then `Cancel`, and verify that the map screen and telemetry stay. Tap `Disconnect` and then `Unpair`, and verify that the pairing screen opens and the session notification stops. Pair again with `T`.

## Chapter 128 - Real Sensor HUD Values (Nov7.2)
*   **128.1 One HUD Rule For Both Apps:** Verify that the Sensor HUD and the Monitor local line build their motion state, speed, accuracy, and age with the same function, `withLocalReadings`, and that a fix without accuracy shows `A:--`. Automated by `FixReadingTest` (ISSUE-NOV6.1-O27).
*   **128.2 The Service's Fix Reaches The HUD:** Verify that a fix published by the Sensor service shows its own motion state, speed, accuracy, and age on the Sensor HUD, and that a `Location` keeps its time, accuracy, and speed, or reports none when it has none. Automated by `SensorHudValuesTest.showsThePublishedFix`, `aLocationKeepsItsSpeedAccuracyAndTime`, and `aLocationWithoutSpeedOrAccuracyReportsNeither`.
*   **128.3 No Fix Shows No Values:** With the service stopped, or before its first fix, verify that the Sensor HUD shows `--` for motion state, speed, accuracy, and age, and no `STATIONARY`, `0.0 km/h`, `±5.0m`, or `0s`. Automated by `SensorHudValuesTest.noFixShowsNoPositionValues` and `clearingTheFixRemovesItsValues`.
*   **128.4 Sensor HUD On Hardware:** On the A15, before `START SENSOR SERVICE`, verify that the Sensor HUD shows `--` for motion state, speed, accuracy, and age. Start the service and verify that the accuracy and speed match the last fused fix in `dumpsys location`, and that the age counts up between fixes and stays under about 10 seconds. The app has no control that stops the service, and `am stopservice` is refused because the service is not exported, so the return to `--` after a stop has automated proof only.

## Chapter 129 - Monitor Alarms And Siren (Nov7.3)
*   **129.1 Alarm Conditions:** Verify that the Monitor raises `Tracker offline` when no update from the paired tracker arrived for 35 seconds, counted from the start of watching when none arrived yet; `Tracker power disconnected` when the tracker reports no charging; `Tracker battery low` below 20%, ending only at 22% or more; and `Tracker temperature high` above 46°C, ending only at 44°C or less. Verify that a late queued update cannot replace a newer one. Automated by `MonitorAlarmsTest` (ISSUE-NOV5.2-O11).
*   **129.2 Until Acknowledged:** Verify that an alarm is raised once, sounds until acknowledged even after its condition clears, and is then shown as `(cleared)`; that an acknowledged alarm is raised again only after its condition has cleared; and that switching an alarm off in Alert Settings, or turning on Global Mute, drops it at once. The switches are `Peer Connection Offline`, `Power Disconnected / Unplugged`, `Low Battery Level (<20%)`, and `High Device Temperature (>46°C)`. Automated by `MonitorAlarmsTest`.
*   **129.3 Pairing And Settings:** Verify that only the paired tracker raises alarms, that a new pairing starts with no alarms, and that nothing is raised before the saved settings are loaded. Automated by `MonitorAlarmsTest`.
*   **129.4 Siren:** Verify that the siren loops until stopped on the alarm stream, or on the notification stream when `Override ... Silent Mode` is off; that `Force Maximum` sets the alarm volume to maximum and restores the earlier volume when the siren stops, and wins over the custom volume; that each wave type has its own sound; and that `TEST SIREN AUDIO` plays and stops the same siren. Automated by `SirenSoundTest` and `MonitorAlarmDeviceTest`, which plays at gain 0 and restores the phone's alarm volume.
*   **129.5 Sensor Stays Silent:** Verify that no Sensor, `core-shared`, or `core-network` source uses audio or vibration classes (Rule 14, R872). Automated by `SensorStealthGuardTest`; run it with `--rerun`.
*   **129.6 Alarm Screen And Notification:** Verify that the alarm notification uses the high-importance `monitor_alarm` channel, that the session notification uses the low-importance `monitor_session` channel, and that the shared `critical_violation_channel` that was lowered to low importance is removed. Verify that the alarm screen names each pending alarm and its start time and acknowledges with one tap. Automated by `MonitorAlarmDeviceTest`.
*   **129.7 Alarms On Hardware:** On the A15 with both apps paired through the production relay, and the siren at a low volume: simulate an unplugged charger with `dumpsys battery unplug` and `dumpsys battery set status 3`, and verify the full-screen alarm, the siren in `dumpsys audio`, and the alarm notification, then `ACKNOWLEDGE ALARM`. Restore with `dumpsys battery reset`, send the Monitor to the background, unplug again, and verify that the alarm returns and that tapping its notification opens `AlarmOverlayActivity`. Force-stop the Sensor and verify `Tracker offline` after about 35 seconds, then restart it and verify `(cleared)` with the siren still on. Simulate `level 15` and `temp 470` and verify both alarms. Always end with `dumpsys battery reset`.

## Chapter 130 - Communication Alarms (Nov7.5)
*   **130.1 Communication Alarm Conditions:** Verify that the Monitor raises `Relay disconnected` when relay socket connection is disconnected (`signalingClient.isConnected` false), `Monitor internet lost` when local internet network capabilities report no internet, and `Tracker GPS signal loss` when telemetry from the paired tracker reports `(0,0)` coordinates, 0 accuracy, or a `NO_FIX` / `GPS_SIGNAL_LOSS` status while the tracker is online. Automated by `MonitorAlarmsTest` (ISSUE-NOV5.2-O11).
*   **130.2 Communication Alert Settings Controls:** Verify that each communication alarm follows its respective Alert Settings toggle (`Relay Connection Disconnected`, `Local Internet Offline`, `GNSS Signal Loss`) and Global Mute, dropping immediately when switched off. Automated by `MonitorAlarmsTest`.
*   **130.3 Communication Alarm Lifecycle:** Verify that communication alarms sound the siren and show on the full-screen alarm overlay until acknowledged even after conditions clear, and that an acknowledged communication alarm re-arms only after its condition clears. Automated by `MonitorAlarmsTest`.

## Chapter 131 - 30-Second Siren Auto-Stop (Nov7.6)
*   **131.1 Siren Auto-Stop Timer:** Verify that when any monitor alarm raises the siren, it automatically stops after 30 seconds (`30s`) even if not acknowledged. Automated by `MonitorAlarmEngine` 30s timeout and `MonitorAlarmsTest` (ISSUE-NOV7.6-O40).
*   **131.2 First Event (Timer or Acknowledge):** Verify that the siren stops on whichever event occurs first: the 30-second timer expiring OR the user tapping Acknowledge. When the siren auto-stops via timer, verify that the visual alarm and notification remain pending until explicitly acknowledged. Automated by `MonitorAlarmEngine`.

## Chapter 132 - False MOVING & Sensor-Detected Violations (Nov7.7)
*   **132.1 Accelerometer Motion Validation:** Verify that the Sensor runs `SentinelLogic.validateMovement` with accelerometer motion (`hasPhysicalMotion`) on each location fix, suppressing GPS drift and false MOVING while stationary. Automated by `SentinelLogicTest` and `SensorService` (ISSUE-NOV5.2-O13).
*   **132.2 Sensor-Detected Violation Transmission:** Verify that when a kinematic jump or outlier (`JUMP`, `OUTLIER`) is detected, the Sensor transmits the violation status in telemetry and dispatches a `ViolationEvent` through `signalingClient.sendViolation`. Automated by `SensorService`.
*   **132.3 Monitor GPS Jump Alarm:** Verify that the Monitor raises `Tracker GPS jump` when telemetry status is `JUMP` or `OUTLIER` or when a `ViolationEvent` is received, following `AlertSettings.jammerDetection`. Automated by `MonitorAlarmsTest`.

## Chapter 133 - HUD Flow 2Hz Rate Limiting (Nov7.9)
*   **133.1 HUD Flow Rate Throttling:** Verify that Monitor `localHudState`, `remoteHudState`, and Sensor `hudState` apply `atUiRate()` (500ms sampling window) to incoming telemetry and fix streams, capping UI state recalculations and emissions to at most 2Hz. Automated by `UiRateTest` and `HudRateTest` (ISSUE-NOV6.0-O21).

## Chapter 134 - Screen State & Rotation Preservation (Nov7.10)
*   **134.1 Overlay and Follow Mode Preservation Across Rotation:** Verify that open overlays, dialog states, and map follow modes persist across screen rotation and configuration changes in `MonitorActivity` and `AdminActivity` by using `rememberSaveable` (ISSUE-NOV6.0-O22).

## Chapter 135 - Thread-Safe Signaling Queue & Order-Preserving Failed Flush (Nov7.11)
*   **135.1 Thread Safety & ReentrantLock Conflation:** Verify that `SignalingQueue` operations (`offer`, `pollBatch`, `pushFront`, `clear`) are fully thread-safe under concurrent access protected by `ReentrantLock`. Automated by `SignalingQueueTest.testThreadSafetyConcurrentAccess` (ISSUE-NOV6.0-O24).
*   **135.2 PushFront Failed Flush Order Preservation:** Verify that when `SocketioSignalingClient` fails a batch flush, `pushFront` returns updates to the front of `SignalingQueue` in their exact original sequence order (`1, 2, 3 ...`), preventing reordering behind newer incoming updates. Automated by `SignalingQueueTest.testPushFrontPreservesOrder`.

## Chapter 136 - Coarse Network Fix Filtering (Nov7.12)
*   **136.1 Coarse Fix Rejection While GPS Fresh:** Verify that `MonitorLocationSource.isBetterLocation` rejects coarse Network location fixes (e.g., accuracy ±77.6m) when a recent, more accurate GPS fix (e.g., accuracy ±11m) is less than 2 minutes (120s) old, preventing location jitter on the Monitor UI and map. Automated by `MonitorLocationSourceTest.coarseNetworkFixRejectedWhenGpsFixIsFresh` (ISSUE-NOV6.1-O28).
*   **136.2 Stale GPS Fix Fallback:** Verify that a coarse Network fix is accepted when the GPS fix is older than 2 minutes (120s), providing fallback positioning when GPS signal is lost. Automated by `MonitorLocationSourceTest.coarseNetworkFixAcceptedWhenGpsFixIsStale`.

## Chapter 137 - Backfill Answer Stops On Send Failure (Nov7.15)
*   **137.1 Complete Answer:** Verify that a backfill range of 120 rows is sent as chunks of 50, 50, and 20 in sequence order, and that only the last chunk carries `isComplete`. Automated by `BackfillResponderTest.allChunksSent_onlyLastIsComplete` and `singleChunk_isComplete` (ISSUE-NOV6.4-O33).
*   **137.2 Stop On Failed Chunk:** Verify that when `sendBackfillResponse` fails for a chunk, no later chunk of that request is sent and the completion chunk is never sent, so the Monitor does not count the range as answered. Automated by `BackfillResponderTest.failedMiddleChunk_stopsAndNeverSendsComplete`, `failedFirstChunk_sendsNothingElse`, and `failedLastChunk_reportsFailure`.
*   **137.3 Empty Range:** Verify that a request for a range the Sensor no longer holds is answered with one empty completion chunk, and that a failed send of it is reported. Automated by `BackfillResponderTest.emptyRange_sendsOneCompleteResponse` and `emptyRange_failedSendReported`.
*   **137.4 Monitor Re-request (manual, A15 + Monitor):** With the Sensor connected, cut the Sensor's network while it answers a large backfill, then restore it. Within about 30 s of the last chunk received, the Monitor log MUST show `Requested <first> to <last>` again for the rows still missing, without a Monitor restart.

## Chapter 138 - Real Sensor Service Status In The Setup Dialog (Nov7.17)
*   **138.1 Service-Reported State:** Verify that `SensorServiceStatus` starts as not running, becomes running only when `SensorService` has started its components (`markRunning` at the end of `initializeServiceComponents`), and returns to not running in `onDestroy` (`markStopped`). Automated by `SensorServiceStatusTest.aNewStatusIsStopped` and `markRunningThenStoppedFollowsTheService` (ISSUE-NOV6.4-O34).
*   **138.2 Dialog Text:** Verify that the Setup dialog lines read `Sensor Service: Running` / `Stopped` and `Relay Connection: Connected` / `Disconnected`, and that no line claims a fixed `Active` or `Ready` state. Automated by `SensorServiceStatusTest.serviceLineShowsRunningOrStopped`, `relayLineShowsConnectedOrDisconnected`, and `noLineClaimsAnInventedReadyState`.
*   **138.3 Live Check (manual, A15):** With the Sensor service not running (after `adb install -r`, or with location permission revoked before launch), open the Sensor `Setup` dialog: it MUST show `Sensor Service: Stopped` and `Relay Connection: Disconnected`. Close it, tap `START SENSOR SERVICE`, reopen it: it MUST show `Sensor Service: Running` and, once connected, `Relay Connection: Connected`. The `Close` and `CHANGE RELAY URL` buttons MUST each show their full label (not squeezed into a vertical strip).

## Chapter 139 - No False Alarms While The Monitor And Tracker Start (Nov7.19)
*   **139.1 Relay And GPS Loss At Start:** Verify that a relay that has not yet connected raises no `RELAY_DISCONNECTED` until `RELAY_FIRST_CONNECT_GRACE_MS` (30 s) after watching starts, that a relay lost after a connection raises it at once, and that `GPS_SIGNAL_LOSS` is raised only after the tracker has reported no position for `GPS_SIGNAL_LOSS_AFTER_MS` (30 s), a position in between restarting the count. Automated by `MonitorAlarmsTest.aRelayNotYetConnectedIsLostOnlyAfterTheGracePeriod`, `gpsSignalLossNeedsNoPositionForTheWholeLimit`, `aColdStartRaisesNoAlarmWhileTheRelayConnectsAndTheTrackerFindsItsFirstFix`, `aRelayThatNeverConnectsIsLostAfterTheGracePeriod`, `aTrackerWithoutAPositionForTheLimitRaisesGpsSignalLoss`, and `aPositionInBetweenRestartsTheGpsSignalLossLimit` (ISSUE-NOV7.17-O42).
*   **139.2 Movement Checked Once Per Fix:** Verify that the Sensor checks movement only when a new fix arrives, against the previous fix and timed by the fixes' monotonic clock, so a fix sent again on later loop ticks is not checked again and the A15's network-to-GPS correction (34.5 m) is valid. Automated by `MovementCheckTest` (6 tests).
*   **139.3 Drift Inside The GPS Error:** Verify that `SentinelLogic.validateMovement` returns `VALID` (`Within GPS error`) when the distance between two fixes is smaller than the larger of their accuracies (the A15 still: 23 m in 1.6 s at 68 m and 59 m accuracy), that a larger move is still judged, and that a `JUMP` reason carries its speed, distance, interval, and accuracies. Automated by `SentinelLogicTest` `Drift inside the GPS error radius of a still phone is valid` and `A move beyond the GPS error radius is still judged and reported with its measurements`.
*   **139.4 Live Cold Start (manual, A15):** Force-stop both apps, start the Sensor service, then start the Monitor within about 10 s, and leave the phone still for at least 60 s. The Monitor log MUST show `Connecting to relay` and `Connected to relay` with no `Pending alarms` line and no siren, and the Sensor log MUST show no `Violation detected`.

## Chapter 140 - The Sensor Reports A Real GPS Loss (Nov7.21)
*   **140.1 Critical Delivery By Flag:** Verify that only `LocationUpdate.isCritical` makes an update bypass batching, survive a full queue, and escape sequence deduplication; that a `NO_FIX` update is batched, deduplicated, and dropped like any reading; that the flag is not sent on the wire; and that `reset()` clears it. Automated by `SignalingQueueTest.testOverflowDropsOrdinaryUpdatesAndKeepsCriticalOnes`, `testNoFixUpdatesAreDeduplicatedLikeOrdinaryOnes`, `testCriticalUpdatesAreNeverDeduplicated`, `testResetClearsTheCriticalFlag`, and `SignalingBatchTest.testCriticalEventFlushesBatch` and `testNoFixStatusIsBatchedNotSentAlone` (ISSUE-NOV7.19-O43, SIMP-NOV7.19-S1).
*   **140.2 Fix Freshness:** Verify that a fix counts as current for less than `FIX_STALE_AFTER_MS` (15 s) on the monotonic clock and as stale from then on. Automated by `FixFreshnessTest` (5 tests).
*   **140.3 No Movement Verdict On Coarse Fixes:** Verify that `MovementCheck` neither judges nor keeps as its reference a fix without accuracy or coarser than 100 m, so the A15's 700 m cell fix after a GPS loss and the GPS fix after it are not violations, while a real jump across a loss is still reported. Automated by `MovementCheckTest.theCellFixAfterAGpsLossSeenOnTheA15IsNotJudged`, `aFixWithoutAccuracyIsNotJudged`, and `aRealJumpAcrossAGpsLossIsStillReported`.
*   **140.4 Live GPS Loss (manual, A15):** With the Sensor service running and a fix found, run `adb shell cmd location set-location-enabled false`, then switch it back on (always check `cmd location is-location-enabled` afterwards). The Sensor log MUST show `No current GPS fix (none, or older than 15000ms): reporting NO_FIX` about 15 s after the last fix, `GPS fix current again` within a few seconds of switching back on, and no `Violation detected` after it. With the Monitor running and the loss kept under 30 s, the Monitor MUST store the `NO_FIX` rows with no position, with no gap in their sequence numbers, and MUST NOT raise an alarm; kept longer than about 45 s, it MUST raise `Tracker GPS signal loss`.

## Chapter 141 - Sensor Battery Seeding & Unknown Value Guarding (Nov7.24)
*   **141.1 Initial Battery State Seeding:** Verify `SensorService.registerBatteryReceiver()` captures sticky `ACTION_BATTERY_CHANGED` broadcast intent returned by `registerReceiver` before `startSensingLoop()` starts, seeding initial charging state, battery level, and temperature, preventing false `POWER_DISCONNECTED` sirens when starting Sensor service while Monitor watches. Automated by `SensorService`.
*   **141.2 Unknown Battery and Temperature Guarding:** Verify unknown battery level (-1) and temperature (0f) are handled gracefully and that Monitor `LOW_BATTERY` and `HIGH_TEMPERATURE` checks guard against unknown/uninitialized values (`batteryLevel >= 0`, `tempCelsius > 0f`). Automated by `MonitorAlarms`.

## Chapter 142 - PermissionGuard Re-checks Sensor Permissions On Resume (Nov7.26)
*   **142.1 Permission Guard Re-check On Resume:** Verify `PermissionGuard` in `AdminActivity` observes `Lifecycle.Event.ON_RESUME` using `LocalLifecycleOwner.current` and `DisposableEffect`, re-evaluating `checkSensorPermissions` so that returning to `AdminActivity` after granting permissions in system Settings or external permission flows immediately updates `allGranted` without requiring button re-clicks. Automated by `PermissionGuardTest` (5 tests) (ISSUE-NOV7.19-O44).
*   **142.2 Required Sensor Permissions By API Level:** Verify `getRequiredSensorPermissions` returns `ACCESS_FINE_LOCATION` and `ACCESS_NETWORK_STATE` on API 26+, adds `POST_NOTIFICATIONS` on API 33+ (TIRAMISU), and adds `FOREGROUND_SERVICE_LOCATION` on API 34+ (UPSIDE_DOWN_CAKE). Automated by `PermissionGuardTest`.

## Chapter 143 - Sensor Siren Test Button Disabled On Stealth Tracker (Nov7.28)
*   **143.1 AlarmSoundOverlay Stealth Tracker Mode:** Verify `AlarmSoundOverlay` checks `isTracker`, rendering `Disabled (Stealth Tracker)` in place of the `TEST SIREN AUDIO` button when `isTracker` is true, ensuring the Sensor (tracker) never provides an interactive local audio test button in compliance with Rule 14 (Stealth Authority, R872). Automated by `SensorStealthGuardTest` and UI inspection (ISSUE-NOV7.3-O38).
*   **143.2 SensorViewModel Stealth Posture:** Verify `SensorViewModel` does not contain siren playback state or audio test triggers. Automated by unit tests and `SensorStealthGuardTest`.

## Chapter 144 - Real Telemetry Unknown Values Handling (Nov7.30)
*   **144.1 Unknown Speed Handling:** Verify that when a location fix has no speed (`!hasSpeed()`), `SensorService` transmits `-1f` as the speed, and the Monitor and Sensor HUDs display `--` instead of an invented `0.0 km/h` or `STATIONARY`. Automated by `UnknownTelemetryTest` and `FixReadingTest` (ISSUE-NOV7.2-O37).
*   **144.2 Unknown Battery and Temperature Handling:** Verify that before battery intents arrive, or when battery level and temperature are unavailable, `SensorService` uses `-1` for battery level and `-1f` for temperature, preventing false low-battery or high-temperature alarms and displaying `--` on HUDs. Automated by `UnknownTelemetryTest`.

## Chapter 145 - Alarm Reach Under Do Not Disturb & Locked Screen Setup (Nov7.32)
*   **145.1 Do Not Disturb (DND) Access Guide:** Verify that `PhoneSetupOverlay` provides dedicated guidance and a direct action button ("Grant DND Access") opening `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` so the user can grant notification policy access for critical alarm bypass. Automated by unit tests and UI inspection (ISSUE-NOV7.3-O39).
*   **145.2 Full-Screen Intent Permission Guide:** Verify that `PhoneSetupOverlay` on Android 14+ (API 34+) provides guidance and an action button ("Authorize Full-Screen Intent") opening `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` so the app can automatically display emergency alarm overlays over locked screens. Automated by unit tests and UI inspection.

## Chapter 146 - Sensor Automatic Service Startup, Live Uptime Timer & Protected Stop Control (Nov7.34)
*   **146.1 Automatic Service Startup On App Launch:** Verify that `AdminActivity` automatically launches `SensorService` on app start if the service is not already running and permissions are granted, ensuring continuous background sensing without manual user intervention. Automated by `AdminActivity` inspection and lifecycle handling (ISSUE-NOV7.2-O36).
*   **146.2 Service Uptime & Start Time Tracking:** Verify that `SensorServiceStatus` tracks service start time (`startTimeMillis`) when `markRunning` is called and resets it to 0 on `markStopped`, and that `formatUptime` formats elapsed time accurately as `HH:mm:ss`. Automated by `SensorServiceStatusTest.markRunningThenStoppedFollowsTheService` and `SensorServiceStatusTest.uptimeFormattingIsCorrect`.
*   **146.3 Service Status Display & Setup Dialog:** Verify that `serviceStatusText` formats the status line with live uptime (e.g. `Sensor Service: Running (00:01:04)`), and that `SensorViewModel` exposes live `serviceUptimeText` updating on a 1s ticker. Automated by `SensorServiceStatusTest.serviceLineShowsRunningOrStoppedWithUptime`.
*   **146.4 Protected Stop Control & Intent Stop Handling:** Verify that `AdminActivity` renders a protected `STOP SENSOR SERVICE` control when `isServiceRunning` is true, presenting a confirmation dialog before sending `SensorService.ACTION_STOP_SERVICE` intent, and that `SensorService.onStartCommand` cleanly handles `ACTION_STOP_SERVICE` by calling `stopSelf()`. Automated by `SensorServiceStatusTest` and service intent handling.

## Chapter 147 - Independent Clean Setup Actions (Nov7.36)
*   **147.1 Clear Home Points & Geofence Action:** Verify that clicking "Clear Home Points & Geofence" in `CleanSetupOverlay` clears geofence violation alert settings (`distance`, `jammerDetection`, `tiltAlert`, `liftAlert`, `tamperAlert`) and acknowledges active alarms in `MonitorAlarmEngine` without unpairing the tracker. Automated by `CleanSetupActionsTest.clearHomePointsClearsGeofenceAlertSettings` (ISSUE-NOV7.1-O35).
*   **147.2 Reset Operational Statistics Action:** Verify that clicking "Reset Operational Statistics" in `CleanSetupOverlay` clears stored trail history from Room DB via `MonitorRepository.clearTrail()` (`TrailDao.deleteAll()`), clears latest telemetry in `MonitorViewModel`, acknowledges alarms on Monitor, and resets service uptime timer in `SensorServiceStatus` / `SensorViewModel` on Sensor. Automated by `CleanSetupActionsTest` and `SensorServiceStatusTest.resetUptimeResetsStartTimeWhenRunning`.
*   **147.3 Trigger Full System Re-Initialization Action:** Verify that clicking "Trigger Full System Re-Initialization" in `CleanSetupOverlay` clears target tracker pairing ID, resets `AlertSettings` to default defaults, resets `sirenType` to `"Siren"`, resets `relayUrl` to `RelayConfig.DEFAULT_URL`, clears stored trail history, and resets service status on both apps. Automated by `CleanSetupActionsTest.defaultAlertSettingsHasSirenTypeSiren` and `CleanSetupActionsTest.defaultRelayUrlMatchesSpecification`.

## Chapter 148 - Local Trail Storage & Pruning (Nov7.38)
*   **148.1 Local Trail Entity Ingestion:** Verify that the Monitor persists its own movement path (`isLocalUser = true`) in `TrailDao` via `MonitorRepository.insertLocalTrail` when receiving location fixes from `MonitorLocationSource`, storing local trail points with `isLocalUser = true`. Automated by `LocalTrailTest` and `MonitorViewModel` local fix collection (ISSUE-NOV6.2-O29).
*   **148.2 Dual Trail Map Rendering:** Verify that `MonitorActivity` renders independent polylines for remote tracker trail (Blue) and local viewer trail (Cyan) on the osmdroid map canvas. Automated by UI implementation and `LocalTrailTest`.
*   **148.3 Local Trail TTL Pruning:** Verify that `TrailDao.pruneLocalTrail` successfully prunes local user trail entries older than 48 hours without affecting remote tracker trail storage. Automated by `TrailStorageBoundTest.localRowsExpireAfter48Hours`.

## Chapter 149 - Unpaired Tracker Trail Retention (Nov8.0)
*   **149.1 Unpaired Tracker Trail Persistence:** Verify that unpairing a tracker (clearing the target ID via `identityManager.clearTargetId()`) does not delete historical trail data stored in Room database (`TrailDao`), preserving historical telemetry and movement points for potential future re-pairing. **Not automated**: the `UnpairedTrackerTrailRetentionTest` named here in Nov8.0 asserted a local `false` and was removed in Nov8.5 (ISSUE-NOV8.2-O46); a real test must unpair and then read the trail table (ISSUE-NOV6.0-O23).

## Chapter 150 - Tracking Document Integrity (Nov8.5)
*Nov8.2 cited a chapter 150 for ISSUE-NOV7.15-O40 but never wrote it; this chapter takes the number (ISSUE-NOV8.2-O46).*
*   **150.1 Required Sections Survive:** Verify that `DOCS/issues.md` still holds its Next Resumption Focus, Work Queue, Open Gaps, Resolved Gaps, Strategic Simplification Ideas, and Hardening Progress Dashboard sections, and that the dashboard is its last section, so a rewrite that cuts the end of the file fails. Automated by `TrackingDocumentIntegrityTest.issuesKeepsEveryRequiredSectionWithTheDashboardLast`.
*   **150.2 Every Resolved Version In Every Record:** Verify that the versions of the `RESOLUTION_ARCHIVE.md` sections equal the versions of the Resolved Gaps sections of `issues.md`, and that each one appears in the SOT Core Hardening Baseline and in the dashboard (a row such as `Nov7.7 to Nov7.12` covers each version in it). Automated by `TrackingDocumentIntegrityTest.everyResolvedVersionIsRecordedInEveryTrackingDocument` and `aDashboardRangeRowCoversEachVersionInIt`.
*   **150.3 Oldest Entries Survive:** Verify that the oldest entries are still present: the Nov4.32 archive and Resolved Gaps sections, the Nov4.26 dashboard row, SOT verification chapter 5.52, and the `Nov4.22-addHUD` baseline entry. Automated by `TrackingDocumentIntegrityTest.historyStillReachesItsOldestEntries`.
*   **150.4 Order And Detection:** Verify that versions are unique and newest first, and that a truncated archive is detected. Automated by `TrackingDocumentIntegrityTest.versionsAreUniqueAndNewestFirst` and `aTruncatedArchiveIsDetected`. The test runs again whenever one of the three documents changes, because `core-shared/build.gradle.kts` declares them as inputs of `:core-shared:test`. Deliberate break: run against the committed Nov8.2 documents, 3 of the 6 tests failed.

---
**Active Testing Chapters: 140 of 150**
*Chapters 10, 47, 71, 78, 80, 84, 89, 92, 94, 117 and items 2.1, 2.2, 8.3, 26.2, 26.3 are retired; see [RETIRED_CHAPTERS.md](RETIRED_CHAPTERS.md). Chapter numbers are not reused.*

*(Procedure exhaustive for all SOT Rules and Hardening Baselines)*




