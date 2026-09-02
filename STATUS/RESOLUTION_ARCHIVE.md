# Resolution Archive (Sep.02.76)

## 🟢 Resolved Issues (Sep.02.76)
*   **Issue #246: Map Settings in Viewer Mode**. User reported that map settings and tool buttons were unresponsive in viewer mode. Root cause was identified as missing event delegation in `MainViewModel.onEvent` for viewer-specific map interactions. Resolved by integrating `MapUseCase` and `HomePointUseCase` into the event pipeline and implementing `handleMapTap`, `handleAddHomePoint`, and `handleRemoveHomePoint` handlers. This ensures full parity of map functionality (fencing, violations, tools) across both app roles (R-ID 247).

## 🟢 Resolved Issues (Sep.02.70)
*   **Idea #241: Protobuf Mapping Unification**. Consolidated mapping logic for `RealtimeStatus` (Signaling) and `TrackerStatusProto` (Persistence) into `TelemetryProtobufMapper`. Synchronized schemas to ensure 100% field parity and removed redundant serialization boilerplate from domain models (R-ID 245).
*   **Idea #240: ContextShadow Automation**. Integrated Hilt-managed `@ShadowContext` injection across all singleton services and suites. Migrated `AudioSynthesizer` to a `@Singleton` class to support injection. This eliminates manual `ContextShadow` wrapping boilerplate and ensures consistent IPC optimization (R-ID 244).
*   **Idea #239: Signaling Interface Consolidation**. Refactored `SignalingProvider` to remove redundant `emitMap` and `emitBinary` overloads. Migrated Protobuf and JSON telemetry serialization logic from `ConnectivitySuite` into `CommunicationManager.transmit()`, simplifying the communication pipeline (R-ID 239).
*   **Issue #245: "SYS" Badge Deactivation lifecycle**. Added handlers for `ConfirmStopTracking` and `ManualExit` in `MainViewModel` to ensure `isSystemActive` is toggled false upon session termination, maintaining visual parity (R-ID 246).

## 🟢 Resolved Issues (Sep.02.68)
*   **Issue #243: GlobalStatusBar isSystemActive Pass-through**. Propagated `isSystemActive` flag from `GlobalStatusBar` to `StatusBar` via `HudConnectivityState` and implemented a "SYS" status badge for visual parity. (Sep.02.68).
*   **Idea #243: UI State Flattening for StatusBar**. Refactored `StatusBar` and `GlobalStatusBar` to consume the unified `HudState` object. This removed over 40 individual parameters from the signature, improving code maintainability and JIT compilation efficiency on Samsung A15 hardware. (Sep.02.68).

## 🟢 Previously Resolved (Sep.03.18)
*   **Issue #243: GlobalStatusBar isSystemActive Pass-through**. Propagated `isSystemActive` flag from `GlobalStatusBar` to `StatusBar` and implemented a "SYS" status badge to ensure visual parity of the tracking engine's state. (Sep.03.18).

## 🟢 Previously Resolved (Sep.02.66)
*   **Issue #242: Unhandled TriggerRecovery Event**. Implemented a reactive signal-response pattern between `MainActivity`, `MainViewModel`, and `MainAppContent`. (Sep.02.66).
*   **Issue #241: Missing Mode-Selection Activation**. Migrated `setAppMode` to suspend and integrated atomic `IS_SYSTEM_ACTIVE_KEY` toggle in `SessionUseCase`. (Sep.02.66).

## 🟢 Previously Resolved (Sep.03.15)
*   **Issue #240: Tracker Telemetry Publication Failure**. Decoupled `repository.updateLocation()` from GPS fix status in `TrackerService.kt`. (Sep.03.15).
*   **Issue #118: Forensic Matrix Synchronization**. Standardized 15+ SIT and Indexing parameters across Engine, Room, and Telemetry layers. (Sep.02.62).
*   **Issue #120b: I/O Stabilization - Startup Pruning Delay**. Implemented 16s delay for DB pruning to prevent startup frame drops on A15 hardware. (Sep.02.62).
*   **Issue #005: Log Spillage Hardening**. Purged direct Log calls; implemented ForensicSanitizer in Timber pipeline. (Sep.02.62).
*   **Issue #119: Boot/Battery Integrity**. Hardened isSystemActive authority in MaintenanceWorker and service lifecycle. (Sep.02.62).
*   **Issue #180: Proto-Mirror Parity Verification**. Expanded Protobuf schema to mirror full forensic state of TrackerStatus domain model. (Sep.02.62).
