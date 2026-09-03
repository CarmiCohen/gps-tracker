# Resolution Archive (Sep.03.110)

## 🟢 Resolved Issues (Sep.03.110)
*   **Issue #897: Target SDK 35 FGS Compatibility**. Fixed `InvalidForegroundServiceTypeException` in `MaintenanceWorker` and `BootServiceStartWorker` by explicitly declaring and passing `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`. Standardized `getForegroundInfo()` across all work artifacts to ensure compliance with Android 15's stricter service type enforcement (R897).

## 🟢 Resolved Issues (Sep.03.100)
*   **Issue #247: Signal Loss False Positives**. User reported "SIGNAL LOSS" alerts despite good GPS signal on S21FE and A15 devices. Root cause was lack of telemetry grace periods during aggressive power management and relay recovery. Resolved by introducing `BUDGET_HARDWARE_SIGNAL_GRACE_MS` (5s) for A15 hardware and correlating Signal Loss triggers with relay recovery states in `MainAlarmLogic`. This prevents false alarms during network handovers and device stabilization (R248).

## 🟢 Resolved Issues (Sep.02.76)
*   **Issue #246: Map Settings in Viewer Mode**. User reported that map settings and tool buttons were unresponsive in viewer mode. Root cause was identified as missing event delegation in `MainViewModel.onEvent` for viewer-specific map interactions. Resolved by integrating `MapUseCase` and `HomePointUseCase` into the event pipeline and implementing `handleMapTap`, `handleAddHomePoint`, and `handleRemoveHomePoint` handlers. This ensures full parity of map functionality (fencing, violations, tools) across both app roles (R-ID 247).

## 🟢 Resolved Issues (Sep.02.70)
*   **Idea #241: Protobuf Mapping Unification**. Consolidated mapping logic for `RealtimeStatus` (Signaling) and `TrackerStatusProto` (Persistence) into `TelemetryProtobufMapper`. Synchronized schemas to ensure 100% field parity and removed redundant serialization boilerplate from domain models (R-ID 245).
*   **Idea #240: ContextShadow Automation**. Integrated Hilt-managed `@ShadowContext` injection across all singleton services and suites. Migrated `AudioSynthesizer` to a `@Singleton` class to support injection. This eliminates manual `ContextShadow` wrapping boilerplate and ensures consistent IPC optimization (R-ID 244).
*   **Idea #239: Signaling Interface Consolidation**. Refactored `SignalingProvider` to remove redundant `emitMap` and `emitBinary` overloads. Migrated Protobuf and JSON telemetry serialization logic from `ConnectivitySuite` into `CommunicationManager.transmit()`, simplifying the communication pipeline (R-ID 239).
*   **Issue #245: "SYS" Badge Deactivation lifecycle**. Added handlers for `ConfirmStopTracking` and `ManualExit` in `MainViewModel` to ensure `isSystemActive` is toggled false upon session termination, maintaining visual parity (R-ID 246).
