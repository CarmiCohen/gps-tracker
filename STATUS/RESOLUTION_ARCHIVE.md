# 🏛️ Resolution Archive - Oct.5.1

## 🏁 Issue #SIMP-1201-1 / SOT ID 615: Logic State Serialization Expansion
*   **Resolved**: Oct.5.1
*   **Root Cause**: Alarm evaluation state (geofence counters, violation timestamps, lockout heuristics) was stored as scattered individual properties in DataStore, leading to parameter bloat in repository methods and potential state desynchronization during service restarts.
*   **Remediations**:
    *   **Unified Schema**: Defined `LogicStateProto` in `app_settings.proto` and added `role_logic_states` map to `AppSettings`.
    *   **Consolidated Persistence**: Refactored `SettingsRepository.saveLogicState` and `AppAlarmManager.saveLogicState` to serialize the entire `AlarmEvaluationState` into a single binary blob.
    *   **Forensic Continuity**: Implemented binary-safe recovery of monotonic timestamps and siren lockout states, keyed by `boot_id` to handle full reboots vs. service restarts correctly.
    *   **API Simplification**: Eliminated 10 individual parameters from `saveLogicState` across the repository chain.
*   **Significance**: Medium (Simplicity/Robustness).
*   **SOT ID**: 615

## 🏁 Issue #1173 / SOT ID 616: Protobuf-First Persistence (Phase 2)
*   **Resolved**: Oct.5.1
*   **Root Cause**: Phase 1 established the schema and mapping, but the system still relied on Room columns for history restoration and lacked a migration path for legacy JSON/Column-based entries in the offline buffer.
*   **Remediations**:
    *   **Binary Restoration**: Updated `TelemetryMapper` (`mapEntityToApp`, `mapPendingToStatus`) to prioritize restoration from the `payload` BLOB, treating legacy columns purely as metadata for indexing.
    *   **On-the-fly Migration**: Implemented a "repair" strategy in `OfflineRepository.getPendingStatusUpdates` that converts legacy SQLite entries into binary Protobuf payloads upon first read.
    *   **History Serialization**: Integrated `mapAppToBinary` into `TelemetryMapper.mapAppToEntity` to ensure all new ribbon history entries are persisted as binary-first.
    *   **Schema Parity**: Expanded `TrackerStatusProto` and `RealtimeStatus` to include 100% field parity with the `LocationUpdate` monolith.
*   **Significance**: Medium (Performance).
*   **SOT ID**: 616

---

# 🏛️ Resolution Archive - Oct.4.6

## 🏁 Issue #1160 / SOT ID 614: Telemetry Pooling & Flyweight Expansion
*   **Resolved**: Oct.4.6
*   **Root Cause**: High-frequency telemetry evaluation (10Hz GPS + 100Hz IMU) was generating excessive object allocations (LocationUpdate, ProcessedLocation), leading to GC pressure and potential thread-safety risks during event emission from the evaluation loop.
*   **Remediations**:
    *   **Pooling Infrastructure**: Implemented `RingBufferPool` (thread-safe, zero-allocation circular pool) and centralized `EnginePools` registry.
    *   **Mutability Hardening**: Refactored `ProcessedLocation`, `TrajectoryNode`, `SentinelResult`, and `JumpConfidence` to support `reset()` and `copyFrom()` for safe reuse.
    *   **Engine Migration**: Migrated `LocationProcessor.processGpsPoint` and `LocationSentinel` to acquire pooled results, eliminating per-tick allocation.
    *   **GTO Optimization**: Refactored `GtoEngine.getWindow()` to utilize pooled `TRAJECTORY_NODE` objects, resolving a major allocation hotspot during trajectory promotion.
    *   **Service Integration**: Updated `MonitorService` to use pooled snapshots for alarm evaluation and forensic capture.
*   **Significance**: Medium (Performance).
*   **SOT ID**: 614

---

# 🏛️ Resolution Archive - Oct.4.5

## 🏁 Issue #1425 / SOT ID 613: Unified Clock Authority
*   **Resolved**: Oct.4.5
*   **Root Cause**: Mixed use of `System.currentTimeMillis()` (Wall Clock) and `SystemClock.elapsedRealtime()` (Monotonic) for duration and "freshness" logic led to arithmetic errors and UI jitter during NTP synchronization or manual clock adjustments.
*   **Remediations**:
    *   **Monotonic Logic**: Standardized all logic-bound temporal deltas (UI pulses, forensic sampling intervals, GPS age calculation, background service heartbeats) to strictly use `elapsedRealtime()` via the `TimeProvider` interface.
    *   **Service Hardening**: Migrated `BaseMonitorService` and `MonitorService` pulse timers to monotonic sources.
    *   **UI Synchronization**: Refactored `MainViewModel` state mapping and `GpsStatusManager` to use `pulseRt` for all freshness evaluations.
    *   **Orchestration Fix**: Updated `UiEventCoordinator` to use `appStartRt` (monotonic) for calculating the startup settling delay (2000ms), ensuring reliable service startup regardless of wall-clock drift.
    *   **Repository Interval Fix**: Migrated `LogRepository` batch and drain timers to monotonic time to prevent skipping or double-flushing log batches.
*   **Significance**: Medium (Logic/Robustness).
*   **SOT ID**: 613

---

# 🏛️ Resolution Archive - Oct.4.1

## 🏁 Issue #1202 / SOT ID 612: UI Event Routing Unification
*   **Resolved**: Oct.4.1
*   **Root Cause**: Procedural UI logic (permission checks, service startup delays) and navigation triggers were scattered across View and ViewModel layers, creating tight coupling and making the system harder to test.
*   **Remediations**:
    *   **Effect Stream**: Introduced `UiEffect` SharedFlow in `MainViewModel` to deliver imperative commands to the UI.
    *   **Coordinator Authority**: Migrated all procedural orchestration from `MainAppContent` and `MainViewModel` into `UiEventCoordinator`.
    *   **Domain Ownership**: Migrated mode transition logic (`InitiateMode`, `RequestProceedToMode`) and permission request logic into the domain layer.
    *   **Passive View**: Refactored `MainAppContent` to act as a passive receiver of `UiEffect`, handling navigation and system intents without maintaining internal procedural state.
    *   **Decoupling**: Removed all direct references to screen routes from `MainViewModel`, delegating to `NavigationUseCase` via the coordinator.
*   **Significance**: Medium (Architecture).
*   **SOT ID**: 612

---

# 🏛️ Resolution Archive - Oct.3.9

## 🏁 Issue #1201 / SOT ID 610: Reactive Siren Lockout
*   **Resolved**: Oct.3.9
*   **Root Cause**: Siren cooldown and lockout logic were coupled with audio generation and violation detection, leading to redundant state management (`lastSirenStopRt`) in multiple layers.
*   **Remediations**:
    *   **Decoupling**: Centralized siren lockout authority in `SirenLockoutUseCase`.
    *   **Engine Refactor**: Purged `lastSirenStopRt` from `AlarmEvaluationState` and updated `MainAlarmLogic.detectViolations` to accept an external `isLockedOut` boolean.
    *   **Reactive Flow**: Configured `AppAlarmManager` to reactively observe `SirenLockoutUseCase.silencedUntilRt` to refresh siren requirements.
    *   **State Recovery**: Refactored `AppAlarmManager.restoreLogicState` to recover siren lockout status from persistence via the centralized UseCase.
    *   **Persistence Cleanup**: Removed redundant `lastSirenStopRt` parameters from `MainRepository` and `SettingsRepository`.
*   **Significance**: Medium (Domain Logic).
*   **SOT ID**: 610

---

# 🏛️ Resolution Archive - Oct.3.8

## 🏁 Issue #1172 / SOT ID 608: Smart Signaling Dispatcher
*   **Resolved**: Oct.3.8
*   **Root Cause**: Signaling triggers (joins, pings, telemetry) were scattered across `CommunicationManager` and `ConnectivitySuite`, leading to race conditions during connection transitions and lack of unified throttling/conflation control.
*   **Remediations**:
    *   **Dispatcher Completion**: Finalized `SmartSignalingDispatcher` to handle all signaling commands.
    *   **Unified Routing**: Migrated `join`, `leave`, and `ping`/`pong` handshakes from `CommunicationManager` to the dispatcher.
    *   **Adaptive Throttling**: Implemented burst delivery for `HIGH` priority commands (handshakes, immediate alerts) while maintaining inter-frame delays for `NORMAL` telemetry to preserve bandwidth.
    *   **Connection Guarding**: Ensured the dispatcher respects `isConnected` state before attempting emission, preventing queue bloat during outages.
    *   **Payload Optimization**: Refactored payload generation to use native Maps, eliminating redundant `JSONObject` wraps before dispatcher ingestion.
*   **Significance**: High (Network Reliability).
*   **SOT ID**: 608

---

# 🏛️ Resolution Archive - Oct.3.7

## 🏁 Issue #1423 / SOT ID 607: StatusBar Visual & Logic Hardening
*   **Resolved**: Oct.3.7
*   **Root Cause**: 
    *   **Layout**: `Row(modifier = Modifier.weight(1f))` in portrait caused sub-rows to attempt a 50/50 horizontal split that exceeded screen width, leading to vertical clipping/stacking anomalies.
    *   **Color**: Kinematic speed and tracker state used independent color logic, leading to green speed labels next to gray state labels.
    *   **GPS Logic**: `isLocalGpsActive` mapping in `MainViewModel` relied on a stale evaluation pulse rather than checking for a non-zero GPS timestamp.
    *   **Age Display**: `lastGpsTs` was being compared to an incompatible clock source (Unix vs Realtime), resulting in negative age values.
*   **Remediations**:
    *   **Portrait Layout**: Switched StatusBar details to a `Column` layout in portrait to prevent overflow and ensure clean vertical separation (R1424).
    *   **Color Unification**: Implemented `stateColor` authority to synchronize state labels and speed colors based on GPS freshness.
    *   **Telemetry Mapping**: Corrected `MainViewModel.mapHudTelemetry` to use `loc.kinetic.rt` (elapsed realtime) for GPS age evaluation, ensuring compatibility with `health.systemPulse`.
    *   **Fix Verification**: Validated correct GPS badge activation and positive age reporting.
*   **Significance**: Medium (UX/Logic).
*   **SOT ID**: 607

---

# 🏛️ Resolution Archive - Oct.3.2

## 🏁 Issue #1420-S / SOT ID 603: HUD Stabilization & Build Recovery
*   **Resolved**: Oct.3.2
*   **Root Cause**: The granular interface slicing introduced in Issue #1420 caused multiple compilation failures across the UI layer due to naming mismatches (`trackerLocPendingReason` vs `locationPendingReason`) and deprecated method signatures in `AlarmOverlay`.
*   **Remediations**:
    *   **Interface Compliance**: Updated `SystemHealthState` to explicitly implement the `Locatable` interface.
    *   **UI Alignment**: Synchronized `MainViewModel.kt`, `UiStateCoordinator.kt`, and `SharedUiComponents.kt` to use the standardized `locationPendingReason` property.
    *   **Call Site Refactor**: Updated `AlarmOverlay` consumers in `AlarmActivity` and `MainAppContent` to pass the `locatable` health slice instead of raw booleans.
    *   **Build Recovery**: Verified full project compilation via `:app:assembleDebug`.
*   **Significance**: High (Build Integrity).
*   **SOT ID**: 603

---

# 🏛️ Resolution Archive - Oct.3.1

## 🏁 Issue #1420 / SOT ID 601: Granular HUD Binding
*   **Resolved**: Oct.3.1
*   **Root Cause**: UI components and services were directly dependent on the monolith `LocationUpdate` object, causing unnecessary coupling and redundant recompositions whenever any field in the monolith changed.
*   **Remediations**:
    *   **Interface Slicing**: Introduced `Locatable`, `BatteryProvider`, and `DeviceIdentity` interfaces in `core:engine`.
    *   **Implementation**: Updated `LocationUpdate` to implement these granular interfaces.
    *   **UI Refactoring**: Refactored `AlarmOverlay` and HUD state models to consume these interfaces.
    *   **Service Optimization**: Updated `AlarmOverlayService` to collect and pass only the required slice to the overlay.
*   **Significance**: Medium (Decoupling).
*   **SOT ID**: 601

---

## 🏁 Issue #SIMP-1510-1 / SOT ID 602: Native Stationary Convergence
*   **Resolved**: Oct.3.1
*   **Root Cause**: Performing high-frequency stationary detection and vibration floor EMA math in the JVM hot-path introduced overhead.
*   **Remediations**:
    *   **Native Offloading**: Expanded `JdHardwareManager` and native bridge (`n12`/`n13`) to handle stationary logic in C++.
    *   **Validator Integration**: Refactored `SentinelValidator` to utilize a `NativeFastPathProvider` delegate.
    *   **Fallback Integrity**: Maintained JVM implementation as a fallback for non-supported hardware.
*   **Significance**: Medium (Performance).
*   **SOT ID**: 602

---

# 🏛️ Resolution Archive - Oct.2.15

## 🏁 Issue #1290 / SOT ID 600: UI State Mapper Consolidation
*   **Resolved**: Oct.2.15
*   **Root Cause**: The `UiStateCoordinator` (or `UiStateMapper`) introduced an unnecessary layer of indirection for activity-scoped state projection, increasing dependency complexity without providing multi-consumer utility.
*   **Remediations**:
    *   **Logic Merging**: Transferred all mapping functions (`mapDashboardConnectivity`, `mapDashboardTelemetry`, `mapDashboardHealth`, `mapHudConnectivity`, `mapHudTelemetry`, `mapHudHealth`, `mapMapViewState`, `computeTrailSegments`) into `MainViewModel`.
    *   **State Migration**: Moved coordinate smoothing variables (`sTrkLat`, `sTrkLng`, etc.) to private properties within `MainViewModel`.
    *   **Dependency Pruning**: Removed `UiStateCoordinator` from the Hilt injection graph. Note: File remains on disk as dead code due to environment limitations.
*   **Significance**: Medium (Maintainability).
*   **SOT ID**: 600

---

## 🏁 Issue #1176 / SOT ID 599: Native FastPath Transitions
*   **Resolved**: Oct.2.15
*   **Root Cause**: High-frequency sensor spike detection (Acoustic and Light) in the JVM layer introduced significant event processing latency and GC pressure during sustained monitoring (250Hz+).
*   **Remediations**:
    *   **JNI Offloading**: Migrated the `HardwareFastPath` evaluation logic to native code (`n10`/`n11` in `jdHardware`).
    *   **JVM Fallback**: Maintained a graceful fallback to JVM-based spike detection in `HardwareSuite` if the native library is unavailable.
    *   **State Synchronization**: Implemented `updateFastPathConfig` to sync baseline, thresholds, and debounce parameters with the native layer.
    *   **Lifecycle Integrity**: Ensured native trackers are reset during suite teardown and baseline resets.
*   **Significance**: Low (Performance Optimization).
*   **SOT ID**: 599
