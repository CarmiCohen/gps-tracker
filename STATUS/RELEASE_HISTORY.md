# Project History & Versioning (Sep.12.45)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Sep.12.45 (Production Readiness & Stability Audit)
- **Version Centralization (#1018)**: Implemented Gradle Version Catalog (`libs.versions.toml`) to unify dependency management across `:app` and `:core:engine`. (SOT ID 315).
- **Stability Audit (#1017)**: Forensic audit confirmed high-assurance role transitions. Verified mutual exclusivity in `MainActivity` and state sanitation in `SessionUseCase`.
- **State Restoration Integrity**: Fixed accuracy window leakage in `LocationProcessor.loadState()` by filling the buffer with restored baseline values. (R-ID 316).
- **Signaling Resumption Hardening (#1015)**: Hardened services to proactively initiate operational tick loops on every peer pulse if inactive. (R-ID 314).
- **Connectivity & Telemetry Audit (#1016)**: Verified dual-device handshake stability and telemetry parity between S21FE and A15.

## Sep.12.31 (Signaling Handshake Fix)
- **Viewer Connection Handshake Bug (#1013)**: Fixed a logic error in `ConnectivitySuite.handleJsonUpdate` where heartbeat pulses were being consumed without emitting a `PeerPulse` event, stalling role synchronization. (R-ID 314).

## Sep.12.20 (Main-Thread Safety Hardening)
- **Main-Thread Task Await Regression (#1011)**: Prevented `Tasks.await` from executing on the Main thread during hardware unregistration sequences to eliminate `IllegalStateException` risks. (R-ID 291).

## Sep.12.12 (Display Power Management)
- **Samsung Display Volatility (#1010)**: Refined flickering detection to suppress 삼성 AOD (S21FE) noise between DOZE and DOZE_SUSPEND states. (R-ID 290).

## Sep.12.02 (Architecture Simplification)
- **HUD Mapping Centralization (#286)**: Unified HUD and Dashboard state mapping into a stateless `UiStateMapper` to eliminate flow arity issues and simplify ViewModel orchestration. (Idea #13).

## Aug.31.04 (Forensic Replay Hardening)
- **Forensic Replay & Metadata Hardening (#779)**: Extended `ForensicSanitizer` policy to the telemetry mapping and historical audit layers. Scrubbed `net_interface` and audit logs at the source to ensure no hardware-bound identifiers leak during historical replay or export.

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
