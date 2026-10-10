# Project History & Versioning (Oct10.10)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct10.10 (Signal Decay Audit)
- **Signal Decay Audit (#SIMP-1010-3)**: Aligned SNR thresholds in `jdhardware-jni.cpp` to the engine standard (22.0 dB-Hz). Remediated a critical scaling bug in `TelemetryMapper.kt` and hardened the Protobuf schema with `optional` snapshot fields. (SOT ID 667).
- **Schema Hardening**: Enabled presence detection for SNR/Vibe snapshots to ensure reliable forensic reconstruction on remote viewers.

## Oct10.9 (Connectivity Jitter Remediation)
- **Connectivity Jitter (#SIMP-1014-3)**: Remediated state jitter in the Compose HUD and IO layer caused by high-frequency native telemetry bursts.
- **IO Stabilization**: Implemented debounced persistence in `MainRepository` to cap DataStore writes at 1Hz during floods.

## Oct10.1 (Restoration Baseline)
- **Build Recovery (#BUILD-RESTORE)**: Successfully restored project to the Oct8.1 baseline (64faffd) to resolve fatal KAPT metadata corruption.
- **Oct10.1 Initialization**: Established new stable foundation for re-integration of native JNI enhancements.

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
