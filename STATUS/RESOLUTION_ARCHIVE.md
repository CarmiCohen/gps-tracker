# Resolution Archive

## [Oct11.2] - Authoritative Sensor & JNI Hardening
**Issue ID**: #SIMP-1011-10, #SIMP-1011-11, #SIMP-1011-12
**Status**: RESOLVED
**Description**: Hardened the authoritative sensor path by implementing temporal contraction for the acoustic floor, enforcing strict JNI critical section safety, and syncing HUD sampling with driver-level anomaly states.
**Root Cause**: Duty-cycled acoustic monitoring was susceptible to "baseline pegging" without temporal decay. JNI critical sections lacked explicit bounds verification, and unthrottled UI updates were compounding overhead during Mali driver stalls.
**Remediation**:
- **Acoustic Floor Hardening (#SIMP-1011-10)**: Implemented time-based decay in `jdhardware-jni.cpp` and `JdHardwareManager.kt` to ensure baseline recovery during duty-cycle off-periods.
- **JNI Resource Audit (#SIMP-1011-11)**: Hardened `n22` with bounds validation and verified zero-JNI loops within `GetPrimitiveArrayCritical` blocks.
- **HUD Throttling Sync (#SIMP-1011-12)**: Applied `HUD_STATE_SAMPLE_THROTTLED_MS` (1000ms) to UI telemetry flows during `isMaliAnomaly` states to reduce rendering pressure.

## [Oct11.1] - Budget Stability & Thermal Forensic Audit
**Issue ID**: #SIMP-1011-7, #SIMP-1011-8, #SIMP-1011-9
**Status**: RESOLVED
**Description**: Finalized stability hardening for budget (Staggered) hardware by auditing thermal sampling decay, implementing adaptive I/O back-off during Mali anomalies, and verifying hysteresis effectiveness.
**Root Cause**: High-temperature transients and GPU driver stalls on low-RAM hardware required authoritative forensic visibility and aggressive I/O mitigation to prevent thermal runaway and system-level stutters.
**Remediation**:
- **Thermal Forensic Audit (#SIMP-1011-7)**: Hardened forensic metadata by adding `coolingSnapshot` to `LogEntry` and `LogEntity` (Migration 82). Verified sampling rate decay to 250ms (`FORENSIC_SAMPLING_INTERVAL_COOLING_MS`) during thermal events.
- **I/O Pressure Test (#SIMP-1011-8)**: Integrated `isMaliAnomaly` into `PersistencePolicy` and `LogRepository` pruning logic. Inhibited trail/history IO and decayed forensic sampling to 250ms during driver-level stalls to mitigate JNI bridge contention.
- **Field Stability Audit (#SIMP-1011-9)**: Implemented forensic logging of hysteresis "gate hits" in `IntegrityMonitor.kt` (both Native and Legacy paths). Verified the 30MB total gate effectively silences memory pressure jitter on budget hardware.

...
