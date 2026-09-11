# SOT Master Requirements (Sep.11.20)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (58 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.2 State Persistence (R002)**: **MANDATORY**. Service state MUST be persisted to `MainRepository` or `DataStore` immediately upon change. In-memory state is transient and MUST be reconstructible after process death.
*   **1.3 Monotonic Time (R003)**: **MANDATORY**. All timing calculations (intervals, timeouts, jitter) MUST use `SystemClock.elapsedRealtime()` via `TimeProvider`.
*   **1.4 Atomic Operations (R004)**: **MANDATORY**. Critical updates to telemetry or health records MUST be atomic. Use database transactions or synchronization primitives.
*   **1.5 Permission Guard (R005)**: **MANDATORY**. All hardware-bound operations MUST be guarded by the latest permission state from `SystemStatusProvider`.
*   **1.6 Dependency Injection (R006)**: **MANDATORY**. All core logic components MUST be injected via Hilt. Manual instantiation is prohibited for singletons.
*   **1.7 Exception Boundary (R007)**: **MANDATORY**. Services MUST implement a `CoroutineExceptionHandler` that captures stack traces and triggers `stopSelf()` on critical failures.
*   **1.8 Resource Cleanup (R008)**: **MANDATORY**. All sensor listeners, flow collectors, and timers MUST be explicitly cancelled in `onDestroy()`.
*   **1.9 Thread Segregation (R009)**: **MANDATORY**. CPU-intensive work MUST run on `Dispatchers.Default`; IO work MUST run on `Dispatchers.IO`. Main thread is for UI updates only.
*   **1.10 Logging Integrity (R010)**: **MANDATORY**. All service lifecycle events and alarm triggers MUST be logged to the forensic log sink.
*   **1.11 Service Foreground (R011)**: **MANDATORY**. Services MUST maintain a foreground notification with role-specific pulse messaging.
*   **1.12 Discovery Phase (R012)**: **MANDATORY**. Peer discovery MUST follow the specified phases: BOOTSTRAP -> MONITORING.
*   **1.13 Heartbeat Loop (R013)**: **MANDATORY**. Services MUST maintain a 1s tick loop and a 5s heartbeat loop for UI/Notification updates.
*   **1.14 Power Save Adaptation (R014)**: **MANDATORY**. GPS and tick intervals MUST adjust dynamically based on power-save mode status.
*   **1.15 Background Restriction Audit (R015)**: **MANDATORY**. The app MUST detect and report `isBackgroundRestricted` state to guide recovery logic.
*   **1.16 Role Switching (R016)**: **MANDATORY**. Mode transitions MUST explicitly terminate the previous service and clear all in-memory telemetry state (activity timestamps/buffers) before initiating the next to prevent cross-role status ghosting (Sep.07.82).
*   **1.17 Data Backfill (R017)**: **MANDATORY**. Offline data MUST be backfilled to the relay server chronologically upon reconnection.
*   **1.18 Telemetry TTL (R018)**: **MANDATORY**. Telemetry records MUST have a strictly enforced time-to-live in persistence.
*   **1.19 UI State Flow (R019)**: **MANDATORY**. ViewModels MUST expose UI state via `StateFlow` to ensure consistent state capture.
*   **1.20 Intent Package Enforcement (R020)**: **MANDATORY**. Internal Broadcast Intents MUST explicitly set the package name.
*   **1.21 WakeLock Lifetime (R021)**: **MANDATORY**. WakeLocks MUST be acquired with a specific timeout to prevent battery drain.
*   **1.22 Alarm Manager Exact (R022)**: **MANDATORY**. Critical watchdog alarms MUST use `setExactAndAllowWhileIdle`.
*   **1.23 Teardown Determinism (R923)**: **MANDATORY**. All hardware teardown sequences MUST join the forensic settling window using a managed `Job` to prevent async races and concurrent registration attempts during rapid service toggles (Sep.06.30).
*   **1.24 Hydration Watchdog Trigger (R924)**: **MANDATORY**. When the Hydration Watchdog triggers, the system MUST enter a "Safe Mode" that suppresses all signaling connection attempts via `CommunicationManager` (Sep.06.01).
*   **1.25 Clock Parity (R922)**: **MANDATORY**. All forensic indexing and backfill queries MUST use monotonic `SystemClock.elapsedRealtime()` as the primary key. Wall-clock time (UTC) MUST only be used for display and persistence metadata, never for interval calculation or sample correlation (Sep.06.17).
*   **1.26 Forensic Separation (R922b)**: **MANDATORY**. Specialized hardware audits (GNSS jitter, sensor rates, energy footprints) MUST be decoupled from hardware bridge implementations (e.g., `HardwareProvider`) into dedicated forensic auditors to maintain bridge leaness and SRP (Sep.06.17).
*   **1.27 Viewer Background Persistence (R926)**: **MANDATORY**. The `ViewerService` MUST utilize `specialUse` FGS type on Android 14+ and maintain a 30s hardware "Poke" rhythm to prevent Samsung-specific background suspension (Sep.06.45).
*   **1.28 Service Mutual Exclusivity (R975)**: **MANDATORY**. The application MUST ensure that only one role-specific foreground service (Tracker or Viewer) is active at any time.
*   **1.29 Reference-Counted Hardware (R975b)**: **MANDATORY**. `HardwareProvider` MUST utilize internal reference counting to manage the lifecycle of physical sensors and GNSS status callbacks. Teardown sequences MUST be suppressed if an active user (Tracker or Viewer) remains, ensuring continuity during rapid mode transitions (Sep.08.00).
*   **1.30 Role Identity Segregation (R799f)**: **MANDATORY**. All UI components representing role-specific data (Map markers, status badges, telemetry labels) MUST use `BrandJd` for Tracker and `ViewerCyan` for Viewer identity. Mixing or defaulting to a single color for both roles is strictly prohibited (Sep.09.16).
*   **1.31 Connectivity State Determinism (R941)**: **MANDATORY**. All signaling lifecycle managers (e.g., `ConnectivitySuite`) MUST explicitly reset local relay connection status (`isRelayConnected`) and RTT metrics in their teardown/stop sequence. The `TelemetryRepository` MUST likewise clear these fields during its `clear()` sequence to prevent stale UI status badges during role transitions (Sep.10.05).
*   **1.32 Unified Session Controls (R285)**: **MANDATORY**. Critical session lifecycle controls (e.g., Termination/Exit) MUST utilize unified components from `SharedUiComponents.kt` to ensure identical visual feedback, labeling, and confirmation logic across all application modes (Tracker/Viewer) (Sep.10.06).
*   **1.33 HUD State Segmentation (R286)**: **MANDATORY**. The UI layer MUST subscribe directly to segmented HUD sub-state flows (`HudConnectivityState`, `HudTelemetryState`, `HudHealthState`). The use of monolithic HUD state facades is prohibited to ensure optimal recomposition performance and clear data-path separation (Sep.10.08).
*   **1.34 Map State Partitioning (R287)**: **MANDATORY**. The Map UI layer MUST utilize a partitioned state model via `MapViewState`. All map telemetry, configuration, and camera triggers MUST be bundled into this object before reaching map components to minimize recomposition overhead and maintain interface stability (Sep.10.12).
*   **1.35 Watchdog Grid Precision (R302)**: **MANDATORY**. The system MUST utilize Fixed Grid Scheduling for all watchdog pulses, anchored to the service start monotonic time (`elapsedRealtime`). Each subsequent pulse MUST align to a strict 90s grid.
*   **1.36 Danger Window Suppression (R302b)**: **MANDATORY**. Watchdog alarms MUST NOT be scheduled within the 20s danger window; if a grid point falls within this window, the schedule MUST push to the next grid interval to prevent recovery loops (Sep.10.30).
*   **1.37 Deterministic Scheduling Logic (R302c)**: **MANDATORY**. The watchdog scheduling calculation MUST be implemented as a deterministic pure function (Companion Object) to ensure full unit test coverage of alignment and danger window logic (Sep.10.39).

### 2. Forensic & Performance Authority (Restored)
*   **1.38 Android 15 (16KB Page Size) Compatibility (R732)**: All native libraries MUST be aligned for 16KB page size compatibility using `-Wl,-z,max-page-size=16384` during linking.
*   **1.39 JNI Namespace Integrity (R733)**: All hardware-specific JNI bridge calls MUST strictly utilize the `jdMbrain` namespace. References to legacy `mbrainSDK` identifiers are forbidden.
*   **1.40 Forensic Bloat Prevention (R731)**: The `LogRepository` MUST implement a secondary safety tier that chunk-prunes forensic logs when the total count exceeds `LOG_LIMIT_STRICT` (5000 entries).
*   **1.41 Automated Database Integrity Validation (R729)**: The `MaintenanceWorker` MUST execute a periodic database integrity audit using `PRAGMA integrity_check` every 24 hours.
*   **1.42 Storage-Aware Adaptive Pruning (R728)**: The `LogRepository` MUST implement granular, fragmentation-aware pruning based on `StorageStatsManager`.
*   **1.43 UI Ribbon Optimization (R726)**: Forensic Ribbons MUST utilize `drawWithCache` and hardware acceleration.
*   **1.44 Forensic Delta-Encoding Hardening (R725)**: The `ForensicSpillBuffer` MUST implement Adaptive Base Resetting when drained.
*   **1.45 Package Name Shadow-Caching (R724)**: Utilize the `GpsApplication.PACKAGE_NAME` shadow-cache for all system identifier lookups.
*   **1.46 Non-Blocking Forensic Audit (R723)**: All `/proc` file operations MUST be executed on `Dispatchers.IO`.
*   **1.47 Hardware State Refresh Throttling (R722)**: Polling of expensive system hardware/permission states is restricted to a minimum cooldown of 15s.
*   **1.48 Memory-Mapped Metadata Header Authority (R717)**: The `ForensicSpillBuffer` MUST utilize a 128-byte persistent metadata header at the start of the file for integrity validation.
*   **1.49 Critical Battery Sentinel Authority (R716)**: The system MUST monitor for abnormal battery discharge rates and correlate them with high sensor activity.
*   **1.50 Persistence Health Alerting Authority (R715)**: Trigger an `ALERT_ID_PERFORMANCE_SPIKE` if `forensicReliability` drops below 0.85 for 30s.
*   **1.51 Persistence Reliability Authority (R714)**: The `LogRepository` MUST track flush success rate via an EMA reliability factor.
*   **1.52 Log Buffer Drain Throttling Authority (R713)**: Forensic persistence flushes MUST be load-aware, scaling dynamically based on `cpuLoad`.
*   **1.53 Adaptive Pruning Authority (R712)**: The `LogRepository` MUST implement fill-level aware proactive pruning using dynamic thresholds.
*   **1.54 Thermal-Aware Sampling Authority (R709)**: Forensic sampling MUST be throttled to 500ms whenever `isCoolingModeActive` is true.
*   **1.55 Forensic Convergence Monitoring Authority (R708)**: The `LogRepository` MUST monitor backfill convergence and trigger a warning if the buffer fails to clear.
*   **1.56 Forensic Adaptive Flushing Authority (R707)**: Forensic traces MUST be persisted to the database at 50% capacity or after a 5-second idle period.
*   **1.57 Binary Trace Delta-Encoding Authority (R706)**: Forensic traces MUST utilize header-based delta encoding for high-fidelity fields.
*   **1.58 Transactional Forensic Backfill Authority (R704)**: Forensic traces MUST be drained from the spill-buffer using a transactional peek/commit pattern.

## 🧩 Functional Requirements (254 IDs)
*   **R-ID 259 (Energy Footprint Integration)**: Forensic energy footprints (Delta mA, Temp Rise, Duration) MUST be structured and propagated from `ForensicAuditor` through the tracking engine to provide a persistent "Last Revival Impact" metric in the HUD and logs (Sep.08.13).
*   **R-ID 267 (A15 Hysteresis Visibility)**: The UI MUST display a "THR" (Throttled) badge when GNSS sampling rates are reduced due to A15 resource load or MaliAnomaly hysteresis to explain telemetry latency to the user (Sep.08.13).
*   **R-ID 274 (GNSS Hysteresis Suppression)**: The system MUST utilize a 10s cooldown window (GNSS_THROTTLING_HYSTERESIS_MS) after a thermal or load-based anomaly clears on Samsung A15 hardware to prevent HUD speed jitter and UI status flickering (Sep.09.15).
*   **R-ID 279 (Monotonic Signaling Hardening)**: The system MUST include the monotonic `rt` (elapsedRealtime) field in all `RealtimeStatus` Protobuf payloads to eliminate heuristic drift in remote HUD signaling and resolve false-positive "Red-Lock" states (Sep.08.10).
*   **R-ID 280 (Forensic Auditor Consolidation)**: Shared stability audit logic (Reliability % / GNSS Jitter) MUST be centralized in `ForensicAuditor` to ensure consistent reporting across Tracker and Viewer roles (Sep.08.12).
*   **R-ID 281 (Hydration Watchdog Recovery)**: The system MUST implement active recovery for hydration stalls. If stuck at Level 2, the system MUST force a reset and restart of the `LifecycleHydrationManager` sequence (Sep.08.12).
*   **R-ID 282 (Single-Device Role Parity)**: The system MUST detect role switches (Tracker ↔ Viewer) on the same device and force a fresh signaling handshake and room registration in `CommunicationManager` to prevent stale peer status (Sep.08.13).
*   **R-ID 283 (Teardown Constraint Hardening)**: The system MUST utilize `safeAverage()` or explicit `isNaN()` guards for all satellite signal averages and derived indices to prevent `NaN` propagation into the persistence layer and avoid SQLite `NOT NULL` constraint violations during service teardown (Sep.08.20).
*   **R-ID 284 (Telemetry Partitioning)**: The `LocationUpdate` model MUST utilize partitioned sub-states (`KineticState`, `AtmosphericState`, `IntegrityState`) for logical separation. Consumers MUST access fields via these sub-states directly. Legacy bridge properties are strictly prohibited (Sep.09.10).
*   **R-ID 285 (Session Termination Unification)**: The session termination flow MUST be visually and functionally identical in both Tracker and Viewer modes, utilizing the `SessionTerminationButton` to prevent user confusion and reduce maintenance overhead (Sep.10.06).
*   **R-ID 286 (Segmented HUD Emissions)**: The HUD state aggregation logic MUST emit segmented flows for Connectivity, Telemetry, and Health to distribute JIT load and minimize the impact of high-frequency telemetry updates on UI responsiveness (Sep.10.08).
*   **R-ID 287 (Map View State Consolidation)**: The map UI must consume a single `MapViewState` object to aggregate configuration and telemetry, reducing the parameter surface area of `AppMapContainer` from ~40 to 1. Rigorous Audit (Sep.10.20) eliminated all UI-side derived flags (Freshness/Validity) in favor of ViewModel-driven state (Sep.10.20).
*   **R-ID 288 (Forensic Tamper Visibility)**: The UI MUST display the specific forensic cause (e.g., Tilt, Shock) beside the `[TAMPER]` badge. This reason MUST be propagated from the tracking engine through signaling and persistence to ensure transparency across roles (Sep.10.40).
*   **R-ID 301 (Alarm Logic Partitioning)**: The alarm evaluation pipeline MUST be partitioned into specialized evaluators (Connectivity, Physical, Geofence, System) to reduce cyclomatic complexity and enable granular forensic auditing of subsystem violations (Sep.09.00).
*   **R-ID 302 (Fixed Grid Watchdog Hardening)**: The system MUST utilize Fixed Grid Scheduling for all watchdog pulses, anchored to the service start monotonic time (`elapsedRealtime`). Each subsequent pulse MUST align to a strict 90s grid. 
    *   **Hardening (Sep.10.30)**: Alarms MUST NOT be scheduled within the 20s danger window; if a grid point falls within this window, the schedule MUST push to the next grid interval to prevent recovery loops on Samsung S21 FE hardware.
    *   **Hardening (Sep.10.39)**: The scheduling calculation MUST be implemented as a deterministic pure function (Companion Object) to ensure full unit test coverage of the danger window logic (Issue #945).

*(Total: 58 Architectural Rules + 254 Functional R-IDs = 312 Items)*
