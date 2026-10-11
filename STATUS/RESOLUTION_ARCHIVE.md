# Resolution Archive

## [Oct11.3] - Authoritative Drift & Stability Hardening
**Issue ID**: #SIMP-1011-13, #SIMP-1011-14, #SIMP-1011-15
**Status**: RESOLVED
**Description**: Hardened geofence reliability during GNSS gaps by integrating Bayesian uncertainty growth and fixed stationary duration accumulation for cross-layer relaxation parity.
**Root Cause**: Accuracy radius remained static during location stalls, creating a "blind spot" for geofence enforcement. Additionally, the Kotlin fallback for vibration processing failed to track stationary duration, breaking 4-hour relaxation on non-JNI paths.
**Remediation**:
- **Bayesian Drift Authority (#SIMP-1011-13)**: Integrated `performBayesianExpansion` in `MonitorService.kt`. Accuracy now authoritatively grows at 15.0 m/s during GNSS stalls.
- **Ultra-Long Stability (#SIMP-1011-14)**: Implemented authoritative `stationaryDurationMs` accumulation in `HardwareSuite.kt` Kotlin fallback to ensure parity with JNI logic.
- **Circular Buffer Forensic (#SIMP-1011-15)**: Audited `CircularStateBuffer.forEachMatch` for zero-allocation safety. Verified `inline` enforcement prevents GC pressure during high-frequency sensor floods.

## [Oct11.2] - Authoritative Sensor & JNI Hardening
**Issue ID**: #SIMP-1011-10, #SIMP-1011-11, #SIMP-1011-12
**Status**: RESOLVED
**Description**: Hardened the authoritative sensor path by implementing temporal contraction for the acoustic floor, enforcing strict JNI critical section safety, and syncing HUD sampling with driver-level anomaly states.
**Root Cause**: Duty-cycled acoustic monitoring was susceptible to "baseline pegging" without temporal decay. JNI critical sections lacked explicit bounds verification, and unthrottled UI updates were compounding overhead during Mali driver stalls.
**Remediation**:
- **Acoustic Floor Hardening (#SIMP-1011-10)**: Implemented time-based decay in `jdhardware-jni.cpp` and `JdHardwareManager.kt` to ensure baseline recovery during duty-cycle off-periods.
- **JNI Resource Audit (#SIMP-1011-11)**: Hardened `n22` with bounds validation and verified zero-JNI loops within `GetPrimitiveArrayCritical` blocks.
- **HUD Throttling Sync (#SIMP-1011-12)**: Applied `HUD_STATE_SAMPLE_THROTTLED_MS` (1000ms) to UI telemetry flows during `isMaliAnomaly` states to reduce rendering pressure.

...
