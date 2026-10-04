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
*   **Root Cause**: UI components and services were directly dependent on the monolithic `LocationUpdate` object, causing unnecessary coupling and redundant recompositions whenever any field in the monolith changed.
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
