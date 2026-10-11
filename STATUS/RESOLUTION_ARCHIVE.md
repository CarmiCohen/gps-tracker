# Resolution Archive

## [Oct11.5] - Event Bus Backpressure Hardening
**Issue ID**: #SIMP-1011-17
**Status**: RESOLVED
**Description**: Audited and hardened the DomainEventBus to ensure critical telemetry integrity during high-frequency sensor floods.
**Root Cause**: The previous BufferOverflow.DROP_OLDEST strategy with a 512-slot buffer was susceptible to evicting CRITICAL alarms if the buffer was saturated by NORMAL/LOW priority sensor noise during UI-induced backpressure.
**Remediation**:
- **Staged Priority Gating (#SIMP-1011-17)**: Implemented priority-aware emission logic in `DomainEventBus.kt`.
- **Buffer Expansion**: Increased `DOMAIN_EVENT_BUS_CAPACITY` to 1024 in `EngineConstants.kt`.
- **Subscription Pressure Guard**: Added staged dropping (LOW events at 1x threshold, NORMAL events at 2x threshold) to guarantee buffer headroom for HIGH and CRITICAL priority events.

## [Oct11.4] - Mali Correlation Refinement
**Issue ID**: #SIMP-1011-18
**Status**: RESOLVED
**Description**: Refined Mali driver anomaly detection to suppress transient I/O and JNI jitter by requiring sustained bridge contention.
**Root Cause**: The previous implementation triggered `isMaliAnomaly` on short-lived JNI or DB spikes, leading to unnecessary UI throttling and log noise on budget hardware.
**Remediation**:
- **Mali Contention Gate (#SIMP-1011-18)**: Refined `IntegrityMonitor.checkMaliDriverAnomaly` to use `consumeMaxJniLatency`.
- **Sustained Gate**: Implemented a 30s sustained contention gate (`MALI_ANOMALY_SUSTAINED_THRESHOLD_MS`) and increased the JNI bridge threshold to 150ms.
- **Unified Alerting**: Integrated `ALERT_ID_MALI_ANOMALY` for consistent violation tracking and forensic visibility.

## [Oct11.3] - Authoritative Drift & Stability Hardening
**Issue ID**: #SIMP-1011-13, #SIMP-1011-14, #SIMP-1011-15
**Status**: RESOLVED
**Description**: Hardened geofence reliability during GNSS gaps by integrating Bayesian uncertainty growth and fixed stationary duration accumulation for cross-layer relaxation parity.
**Root Cause**: Accuracy radius remained static during location stalls, creating a "blind spot" for geofence enforcement. Additionally, the Kotlin fallback for vibration processing failed to track stationary duration, breaking 4-hour relaxation on non-JNI paths.
**Remediation**:
- **Bayesian Drift Authority (#SIMP-1011-13)**: Integrated `performBayesianExpansion` in `MonitorService.kt`. Accuracy now authoritatively grows at 15.0 m/s during GNSS stalls.
- **Ultra-Long Stability (#SIMP-1011-14)**: Implemented authoritative `stationaryDurationMs` accumulation in `HardwareSuite.kt` Kotlin fallback to ensure parity with JNI logic.
- **Circular Buffer Forensic (#SIMP-1011-15)**: Audited `CircularStateBuffer.forEachMatch` for zero-allocation safety. Verified `inline` enforcement prevents GC pressure during high-frequency sensor floods.

...
