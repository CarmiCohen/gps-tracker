# SOT Master Requirements & Hardening Status (Oct.3.8)

## 🏗️ Architectural Master Rules (122 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.75 Coupled Alarm Signaling (R042/S576)**: The system MUST NOT trigger physical audio (siren) without an accompanying high-priority notification and visual context. (Oct.1.6).
*   **1.76 System-Wide Alarm Overlay (R578)**: Critical theft alarms MUST utilize `SYSTEM_ALERT_WINDOW` to guarantee UI promotion. (Oct.1.7).
*   **1.77 Global Alarm Acknowledgment (R185/S579)**: Alarm acknowledgment state MUST be synchronized globally. All triggers MUST be evaluated against `lastAlarmAckTs` carried in the telemetry stream to ensure idempotency across peer re-installs. (Oct.1.8 - Issue #1410).
*   **1.78 Mode-Based Alert Restriction (R588)**: Full-screen alert overlays MUST be restricted to Viewer mode only to preserve Tracker stealth. (Oct.1.8).
*   **1.79 Local State Authority (R589)**: Local diagnostic dashboards MUST reflect engine behavioral state independently of peer connectivity status. (Oct.1.8).
*   **1.80 Engine Map Atomicity (R585)**: All mutations to the shared alarm state map MUST be synchronized to prevent concurrent modification during high-frequency audits. (Oct.1.8).
*   **1.81 Load-Aware IMU Gating (R590)**: The system MUST compensate for accelerometer jitter during CPU saturation (>0.85) by expanding evaluation thresholds by 1.5x. (Oct.2.2 - Issue #1415).
*   **1.82 Stable Load Calibration (R591)**: `Passive Zeroing` and vibration floor recalibration MUST be paused during high CPU load to prevent baseline corruption from hardware drift. (Oct.2.2 - Issue #1415).
*   **1.83 Heap-Aware Forensics (R592)**: High-frequency forensic sampling MUST be throttled (up to 4x interval) and aggressive GC triggered when heap allocation exceeds memory pressure thresholds (200MB/250MB). (Oct.2.2 - Issue #1416).
*   **1.84 Connectivity Hysteresis (R593)**: `RELAY_OFFLINE` and `SIGNAL_LOSS` transitions MUST utilize a 3s temporal hysteresis to prevent alert oscillation during transient 500ms relay lag. (Oct.2.2 - Issue #1417).
*   **1.85 WindowManager Lifecycle Hardening (R582)**: Overlay services MUST implement full `Lifecycle` transitions (`ON_RESUME`/`ON_PAUSE`) and explicit `disposeComposition()` to prevent window leaks on budget hardware (A15). (Oct.2.3).
*   **1.86 Native Sensor Pulse (R256)**: High-frequency sensor pulses (250Hz+) MUST be tracked in native code (JNI) to eliminate JVM heap churn and garbage collection pressure during sustained monitoring. (Oct.2.5 - Issue #SIMP-1416-1).
*   **1.87 Real-time Only Telemetry (R1175)**: Forensic backfilling and gap-filling logic MUST be excluded from the telemetry pipeline to ensure zero-churn real-time state management and minimize heap pressure. (Oct.2.6 - Issue #1175).
*   **1.88 Telemetry Mapping Convergence (R596)**: Domain orchestration MUST utilize consolidated mapping authorities (e.g., `TelemetryMapper.mapTickToOutputs`) to prepare persistence and signaling DTOs, ensuring atomic field injection and reducing coordinator complexity. (Oct.2.7 - Issue #1329).
*   **1.89 Snap-to-Update Monolith (R597)**: The system MUST utilize a unified `LocationUpdate` DTO for both engine evaluation and telemetry persistence. Redundant bridge layers (e.g. SystemEvaluationSnapshot) MUST be merged to eliminate mapping overhead and object churn. (Oct.2.8 - Issue #1330).
*   **1.90 TrackerStatus Convergence (R598)**: The system MUST maintain a single telemetry source of truth by merging the `TrackerStatus` DTO into the `LocationUpdate` monolith. All domain, signaling, and persistence layers MUST operate on the unified monolith to ensure architectural consistency and zero-allocation parity. (Oct.2.9 - Issue #1314).
*   **1.91 Native FastPath (R257)**: High-frequency sensor spike detection (Acoustic/Light) MUST be offloaded to native code (JNI) to minimize JVM event processing latency and eliminate heap churn during sustained monitoring. The system MUST maintain a JVM fallback to ensure functional parity on hardware where the native library fails to load. (Oct.2.15 - Issue #1176).
*   **1.92 UI State Consolidation (R1290)**: Activity-scoped UI state projection logic MUST be consolidated within the `MainViewModel` to minimize dependency layers and simplify the state transformation pipeline. (Oct.2.15 - Issue #1290).
*   **1.93 Granular HUD Binding (R1420)**: UI components and services MUST consume granular, slice-based interfaces (e.g., `Locatable`, `BatteryProvider`) rather than the monolithic `LocationUpdate` object to reduce engine-to-UI coupling and prevent redundant recompositions. (Oct.3.1 - Issue #1420).
*   **1.94 Native Stationary Convergence (R1510)**: Stationary detection math and vibration floor EMA calculations MUST be offloaded to native code (JNI) via the `NativeFastPathProvider` to eliminate JVM floating-point overhead on high-frequency hot paths. (Oct.3.1 - Issue #SIMP-1510-1).
*   **1.95 HUD Stabilization (R1420-S)**: UI components MUST utilize standardized interface properties (e.g., `locationPendingReason`) to ensure type safety and architectural alignment with slice-based data models. (Oct.3.2 - Issue #1420-S).
*   **1.96 HUD Transparency & Viewport Maximization (R1421)**: The HUD background MUST maintain an alpha <= 0.6 and utilize a consolidated, single-row layout in portrait mode to ensure maximum map visibility and prevent occlusion of overlay controls (Settings/Logs). (Oct.3.5 - Issue #1421).
*   **1.97 Reactive Connection Re-Binding (R1422)**: Signaling parameters (Relay URL, Device IDs) MUST be observed reactively. The transport layer MUST force immediate reconnection upon configuration changes, bypassing "already connected" cache states to resolve sticky-link issues. (Oct.3.6 - Issue #1422).
*   **1.98 WebSocket Transport Priority (R1423)**: The system MUST prioritize `websocket` transport over `polling` to ensure session persistence on cloud-relay environments (Render/Heroku) and prevent SRV status deadlock. (Oct.3.6 - Issue #1422).
*   **1.99 StatusBar Portrait Logic (R1424)**: Technical telemetry details MUST be displayed in a vertical `Column` in portrait mode to prevent horizontal overflow and layout stacking anomalies. (Oct.3.7 - Issue #1423).
*   **1.100 Smart Signaling Dispatcher (R1172)**: ALL signaling triggers (including joins, pings, and telemetry) MUST be routed through a unified `SmartSignalingDispatcher`. The dispatcher MUST enforce connection-aware delivery and adaptive throttling, ensuring high-priority commands bypass inter-frame delays while telemetry is conflated and throttled to preserve bandwidth. (Oct.3.8 - Issue #1172).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 608**: Smart Signaling Dispatcher - Consolidated all signaling triggers into a reactive coordination layer with adaptive throttling and conflation. (Resolved Oct.3.8).
*   **SOT ID 607**: StatusBar Visual & Logic Hardening - Fixed portrait overflow, color inconsistency, and clock source mismatch. (Resolved Oct.3.7).
*   **SOT ID 605**: Connection Sticky-State & Validation - Fixed bug where CommunicationManager would ignore URL/ID changes; corrected log relay validator. (Resolved Oct.3.6).
*   **SOT ID 604**: HUD Visibility & Occlusion - Consolidated portrait HUD; reduced background opacity; ensured map visibility behind overlays. (Resolved Oct.3.5).
*   **SOT ID 603**: HUD Stabilization - Resolved post-refactor compilation failures and property naming mismatches. (Resolved Oct.3.2).
*   **SOT ID 602**: Native Stationary Convergence - Offloaded stationary detection and vibration floor EMA to JNI. (Resolved Oct.3.1).
*   **SOT ID 601**: Granular HUD Binding - Decoupled UI components from LocationUpdate monolith via interface slicing. (Resolved Oct.3.1).
*   **SOT ID 600**: UI State Mapper Consolidation - Merged UiStateCoordinator into MainViewModel to reduce architectural complexity. (Resolved Oct.2.15).
*   **SOT ID 599**: Native FastPath Transitions - Offloaded Acoustic and Light spike detection to JNI to reduce JVM overhead and GC pressure. (Resolved Oct.2.15).
*   **SOT ID 598**: TrackerStatus Convergence - Purged redundant TrackerStatus DTO and consolidated all state into LocationUpdate monolith. (Resolved Oct.2.9).
*   **SOT ID 597**: Snap-to-Update Monolith - Merged SystemEvaluationSnapshot into LocationUpdate to eliminate bridge mapping layers. (Resolved Oct.2.8).
*   **SOT ID 596**: Telemetry Mapping Convergence - Consolidated construction of update DTOs into TelemetryMapper to centralize domain orchestration logic. (Resolved Oct.2.7).
*   **SOT ID 595**: Real-time Only Path - Strategically removed forensic backfilling to simplify architectural state. (Resolved Oct.2.6).
*   **SOT ID 594**: Native Sensor Pulse - Offloaded 250Hz frequency auditing to JNI to eliminate heap churn. (Resolved Oct.2.5).
*   **SOT ID 582**: WindowManager Hardening - Implemented full lifecycle transitions and explicit disposal. (Resolved Oct.2.3).
*   **SOT ID 593**: Connectivity Hysteresis - Implemented 3s temporal suppression for offline alerts. (Resolved Oct.2.2).
*   **SOT ID 592**: Memory Pressure Mitigation - Integrated heap-aware throttling for forensic sampling. (Resolved Oct.2.2).
*   **SOT ID 591**: Stable Load Calibration - Paused vibration floor updates during CPU saturation. (Resolved Oct.2.2).
*   **SOT ID 590**: Load-Aware IMU Gating - Compensated for LIS2DLC12 jitter under load. (Resolved Oct.2.2).

---

## 🏁 Verification Chapters
*   **Chapter 31.228 (Smart Signaling Audit)**: PASSED - Verified connection-aware delivery for all commands. Confirmed adaptive throttling allows join/ping bursts while maintaining inter-frame delays for location updates. (Oct.3.8)
*   **Chapter 31.227 (StatusBar Hardening Audit)**: PASSED - Verified vertical stacking in portrait. Confirmed unified color logic and compatible clock sources for GPS age. (Oct.3.7)
*   **Chapter 31.226 (Connectivity Hardening Audit)**: PASSED - Verified immediate SRV reconnection upon URL/ID change. Confirmed tracker logs are correctly processed by the viewer following validator fix. (Oct.3.6)
*   **Chapter 31.225 (HUD Visibility Audit)**: PASSED - Confirmed compact single-row HUD in portrait. Verified transparency allows map viewing through HUD background. Verified Settings overlay accessibility. (Oct.3.5)
*   **Chapter 31.224 (HUD Stabilization Audit)**: PASSED - Verified full project compilation. Confirmed all UI call sites utilize interface-compliant properties (`locationPendingReason`). (Oct.3.2)
*   **Chapter 31.223 (Native Convergence Audit)**: PASSED - Verified zero JVM math overhead for stationary detection and vibration floor recalibration via JNI offloading. (Oct.3.1)
*   **Chapter 31.222 (HUD Decoupling Audit)**: PASSED - Confirmed AlarmOverlay and StatusBar consume granular interfaces. Verified zero redundant recompositions in AlarmOverlayService during telemetry-only updates. (Oct.3.1)
*   **Chapter 31.221 (UI State Consolidation Audit)**: PASSED - Verified successful merge of UiStateCoordinator into MainViewModel and removal of redundant dependency layer. All dashboard and map state transformations validated. (Oct.2.15)
*   **Chapter 31.220 (Native FastPath Audit)**: PASSED - Verified zero heap churn and reduced CPU latency for high-frequency acoustic/light spike detection via JNI FastPath on A15 hardware. (Oct.2.15)
*   **Chapter 31.219 (TrackerStatus Purge Audit)**: PASSED - Verified complete removal of TrackerStatus DTO and successful migration to LocationUpdate monolith across UI, engine, and persistence. (Oct.2.9)
*   **Chapter 31.218 (Monolith DTO Audit)**: PASSED - Verified removal of SystemEvaluationSnapshot and unification into LocationUpdate across all engine and signaling modules. (Oct.2.8)
*   **Chapter 31.217 (Orchestration Convergence Audit)**: PASSED - Verified that all telemetry DTO construction for tick events is handled by TelemetryMapper, removing manual injection from AppEventCoordinator. (Oct.2.7)
*   **Chapter 31.216 (Telemetry Stream Audit)**: PASSED - Verified zero heap churn during simulated network gaps via removal of backfill buffers. (Oct.2.6)
*   **Chapter 31.215 (Native Pulse Audit)**: PASSED - Verified zero heap allocation spikes during 250Hz sensor stream via JNI pulse tracker on A15. (Oct.2.5)
*   **Chapter 31.214 (Resource Lifecycle Audit)**: PASSED - Verified zero WindowManager leaks during 1-hour sustained overlay alert on A15. (Oct.2.3)
*   **Chapter 31.213 (Connectivity Jitter Audit)**: PASSED - Verified 3s hysteresis prevents oscillation during 500ms relay jitter. (Oct.2.2)
*   **Chapter 31.212 (Heap Pressure Audit)**: PASSED - Confirmed sampling throttling and GC execution at 200MB heap threshold. (Oct.2.2)
*   **Chapter 31.211 (Sensor Load Audit)**: PASSED - Confirmed IMU threshold expansion under 100% CPU saturation on A15 hardware. (Oct.2.2)
*   **Chapter 31.210 (Thread-Safety Stress Audit)**: PASSED - Verified zero CME crashes during 250Hz sensor audit under CPU saturation. (Oct.2.1)
