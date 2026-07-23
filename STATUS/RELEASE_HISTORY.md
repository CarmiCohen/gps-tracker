# Project History & Versioning (July.23.10)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## July.23.10 (Hardware Hardening & Status Consistency)
- **SRV Status Consistency (#533)**: Modified `CommunicationManager.kt` to proactively update `TelemetryRepository` on socket connection state changes (Connect, Disconnect, Reconnect, Error). This ensures the UI "SRV" badge reflects real-time status immediately.
- **Step Detector Hardening (#098)**: Implemented explicit `ACTIVITY_RECOGNITION` permission check in `AppSensorManager.kt` before registering the Step Detector on Android 10+. This prevents hardware-level `fail(2)` (Permission Denied) errors.

## July.23.09 (Validation & Engine Hardening)
- **AnchorEvaluator Validation (#533b)**: Implemented comprehensive unit tests verifying coordinate averaging, urban multipath suppression, and Safety Valve breakout behavior.
- **Anchor Logic Hardening (R990c)**: Hardened the coordinate averaging logic to strictly exclude points outside the breakout threshold, preventing anchor "chase" and preserving escape sensitivity.
- **Test Suite Remediation**: Fixed compilation and logic regressions in `AdaptationMuzzleTest`, `ForensicIdentityTest`, `SignalingTest`, `TelemetryAggregatorTest`, and `LocationSentinelHindsightTest`.
- **Telemetry Fix (#523)**: Corrected logic inversion in `mergeWorstCase` aggregation for forensic indices and signal strength.

## July.23.08 (Architectural Simplification)
- **AnchorEvaluator Extraction (#533b)**: Decoupled stationary anchor logic from `LocationProcessor.kt` into a dedicated component.
- **Safety Valve Hardening**: Implemented an automated breakout path to prevent "sticky" anchors on faulty hardware or in extreme vibration scenarios.
- **Code Cleanup**: Removed redundant state variables and centralized anchor management.

... [See historical logs for full records]
