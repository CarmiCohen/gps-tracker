# Resolution Archive

## [Oct10.11] - Budget Hardware Hardening
**Issue ID**: #SIMP-1011-4, #SIMP-1011-5, #SIMP-1011-6
**Status**: RESOLVED
**Description**: Hardened the forensic path for budget hardware (Staggered tier) by tuning pressure hysteresis, optimizing JNI bridge performance, and deepening Mali anomaly detection.
**Root Cause**: Low-tier devices exhibited telemetry oscillation due to tight hysteresis and showed JNI-level micro-stutters during high GPU load that were previously invisible to JVM-side monitoring.
**Remediation**:
- **Hysteresis Tuning (#SIMP-1011-4)**: Increased base Memory/Storage hysteresis to 15.0 MB and applied a 2.0x multiplier for Staggered performance tiers in `IntegrityMonitor.kt`.
- **Acoustic Profiling (#SIMP-1011-5)**: Transitioned `n22` to zero-copy `GetPrimitiveArrayCritical` in `jdhardware-jni.cpp`. Integrated standardized `LatencyMonitor` auditing into all JNI batch paths in `JdHardwareManager.kt`.
- **Mali Forensic Audit (#SIMP-1011-6)**: Expanded `LatencyMonitor` to track `maxJniLatency`. Updated `IntegrityMonitor.kt` to correlate JNI bridge stalls (>100ms) with CPU load for Mali anomaly detection.

## [Oct10.10] - Signal Decay Audit
**Issue ID**: #SIMP-1010-3
**Status**: RESOLVED
**Description**: Aligned SNR health thresholds across Native, Engine, and Telemetry layers and remediated a critical scaling bug in the Remote Processor.
**Root Cause**: Native JNI layer used legacy hardcoded thresholds (18.0/20.0) that drifted from the engine standard (22.0). TelemetryMapper used a hardcoded 5.0x multiplier instead of the normalized 45.0x (RIBBON_SNR_SCALE_DB), causing incorrect remote rejections.
**Remediation**:
- Synchronized `jdhardware-jni.cpp` thresholds to 22.0 dB-Hz.
- Updated `app_settings.proto` to support SNR/Vibe presence detection via `optional` fields.
- Corrected `TelemetryMapper.kt` reconstruction scaling to raw dB-Hz using `RIBBON_SNR_SCALE_DB`.
- Prioritized dedicated snapshot fields over indexed fallbacks in all mapping paths.

## [Oct10.9] - Connectivity Jitter Remediation
**Issue ID**: #SIMP-1014-3
**Status**: RESOLVED
**Description**: Remediated state jitter in the Compose HUD and IO layer caused by high-frequency native JNI telemetry bursts.
**Root Cause**: Native sensor throughput (100Hz+) was triggering direct DataStore writes and unthrottled UI recomposition.
**Remediation**:
- Implemented `saveLocationUpdateDebounced` in `MainRepository` to cap persistence IO at 1Hz.
- Applied 200ms temporal sampling (`HUD_STATE_SAMPLE_MS`) to telemetry and signaling flows in `MainViewModel`.
- Integrated flyweight duplication to maintain state integrity during asynchronous debouncing.

## [Oct10.8] - JNI Consolidation
**Issue ID**: #SIMP-1014-2
**Status**: RESOLVED
**Description**: Finalized native system pressure evaluation (Memory/Storage) with hysteresis in `n24`. Consolidated GNSS (`n21`), Acoustic (`n22`), and Proximity (`n23`) JNI paths to replace Kotlin fallbacks. Increased shared state buffer to 2048 bytes for multi-sensor safety. Optimized forensic capture for zero-allocation throughput. Resolved Oct10.8.
...
