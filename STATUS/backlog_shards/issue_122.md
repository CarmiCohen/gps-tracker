# Issue #122: SIT Propagation Depth & Relay Audit

## Status: Resolved (July.22.02)
## Requirement: R118 (Forensic Parity)

### Description
The forensic Sit-Detection (SIT) matrix expanded from 5 to 15+ parameters (including Vibe, Lux, Baro Delta, and Tilt). The telemetry pipeline (Protobuf and JSON) must be audited to ensure these new parameters propagate from the engine to the relay server and ultimately to the viewer HUD without truncation or type mismatch.

### Resolution
- **Protobuf Update**: Updated `RealtimeStatus.proto` to include the full forensic SIT matrix.
- **Relay Audit**: Verified that the NodeJS relay server is schema-agnostic and correctly passes the expanded binary payload.
- **Mapping Hardening**: Updated `TrackerStatus.fromEnginePoint()` and the corresponding JSON serializers to include all 15 parameters.
- **Precision Preservation**: Enforced `Double` precision for all sensor indices to prevent rounding errors during normalization.

### Verification
- [x] Verified SIT matrix parity between Tracker HUD and Viewer HUD.
- [x] Confirmed relay server does not drop expanded Protobuf packets.
