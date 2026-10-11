# Project History & Versioning (Oct11.2)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct11.2 (Authoritative Sensor & JNI Hardening)
- **Acoustic Floor Hardening (#SIMP-1011-10)**: Implemented temporal baseline decay (Leaky Integrator) in `jdhardware-jni.cpp` (`n11`, `n22`) and `JdHardwareManager.kt` to ensure baseline recovery during duty-cycle "off" periods. (SOT ID 674).
- **JNI Resource Audit (#SIMP-1011-11)**: Hardened `n22` with explicit array bounds verification and zero-JNI loop enforcement within `GetPrimitiveArrayCritical` blocks. (SOT ID 675).
- **HUD Throttling Sync (#SIMP-1011-12)**: Integrated `HUD_STATE_SAMPLE_THROTTLED_MS` (1000ms) in `MainViewModel.kt` to reduce rendering and I/O pressure on budget hardware during GPU driver anomalies. (SOT ID 676).

## Oct11.1 (Budget Stability & Thermal Forensic Audit)
- **Thermal Forensic Audit (#SIMP-1011-7)**: Hardened forensic metadata and verified sampling rate decay to 250ms during thermal events.
- **I/O Pressure Test (#SIMP-1011-8)**: Integrated `isMaliAnomaly` into persistence gating and pruning logic to mitigate driver-level contention.
- **Field Stability Audit (#SIMP-1011-9)**: Implemented forensic logging of hysteresis "gate hits" to verify jitter suppression effectiveness.

## Oct10.10 (Signal Decay Audit)
- **Signal Decay Audit (#SIMP-1010-3)**: Aligned SNR thresholds in `jdhardware-jni.cpp` to the engine standard (22.0 dB-Hz). Remediated a critical scaling bug in `TelemetryMapper.kt` and hardened the Protobuf schema with `optional` snapshot fields. (SOT ID 667).

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
