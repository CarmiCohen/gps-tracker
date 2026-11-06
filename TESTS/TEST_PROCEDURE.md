# Test Procedure - GPS Tracker (vNov2.24 - EXHAUSTIVE VERIFICATION BASELINE)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

---

## Chapter 0 - Operational Forensic Audit
**Status:** 📂 Moved to separate file to ensure persistence and integrity.
**Link:** [OPERATIONAL_FORENSIC_AUDIT.md](OPERATIONAL_FORENSIC_AUDIT.md)

---

## Chapter 1 - Deployment & Initial Launch
*   **1.1 Environment Reset:** Uninstall existing versions.
*   **1.2 Deployment:** Deploy via Android Studio (Nov2.24).
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
*   **8.1 Siren Suppression (Tracker):** Confirm NO local audio or red-screen.
*   **8.2 Siren Authority (Viewer):** Confirm Viewer triggers full-screen Red Alert and 30s Siren.
*   **8.3 Mute Lockout:** Dismiss siren on Viewer. Confirm 30s manual mute lockout.

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
*   **26.2 Siren Auto-Stop:** Trigger siren; verify it stops after 30s.
*   **26.3 Lockout Period:** Dismiss siren; verify 30s manual mute lockout.

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
*   **122.1 Local HUD Line:** On the Monitor, verify that the local line shows the phone's own battery, charging state, temperature, fix accuracy, and fix age, and compare them with `dumpsys battery` and `dumpsys location`. Automated by `MonitorHudValuesTest.usesBatteryAndFixValues`.
*   **122.2 Battery Reading:** Verify that a level or temperature the system does not report stays unknown instead of becoming 100% or 30°C, in both apps. Automated by `BatteryReadingTest`.
*   **122.3 Unknown Values:** With no fix, or before any telemetry, verify that the affected HUD values show `--` and no number, and that a real 0 still shows as a number. Automated by `HudStateTest` and `MonitorHudValuesTest.noFixShowsNoPositionValues`.
*   **122.4 Old Fix:** Stop moving with the Monitor. Verify that after 35 seconds without a new fix, speed and motion state show `--`, while accuracy and age stay. Automated by `MonitorHudValuesTest.oldFixKeepsItsAccuracyAndAgeButNotItsSpeed`.

---
**Active Testing Chapters: 112 of 122**
*Chapters 10, 47, 71, 78, 80, 84, 89, 92, 94, 117 and items 2.1, 2.2 are retired; see [RETIRED_CHAPTERS.md](RETIRED_CHAPTERS.md). Chapter numbers are not reused.*

*(Procedure exhaustive for all SOT Rules and Hardening Baselines)*



