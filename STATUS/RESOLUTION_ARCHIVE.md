# 📜 Resolution Archive

## 🟢 Resolved in Oct7.11
*   **Issue #SIMP-1007-17: Strategic Simplification.**
    *   **Consolidation**: Centralized GNSS health evaluation (Signal Loss, Gaps, Stalls) and behavioral anomalies (Jamming, Acoustic Violations) into `SentinelValidator.evaluateLocationPendingReason`.
    *   **Logic Migration**: Purged redundant manual status evaluation from `HardwareSuite.kt`, replacing it with delegation to the centralized evaluator.
    *   **Pipeline Promotion**: Instrumented `LocationProcessor.processGpsPoint` to promote behavioral rejections from the sentinel result into the unified `LocationPendingReason`.
    *   **Priority Resolution**: Integrated `getHigherPriorityReason` into `TelemetryAggregator` and `TelemetryMapper` to ensure the most critical health issue is signaled when multiple conditions overlap (e.g., Jamming vs. Signal Loss).

## 🟢 Resolved in Oct7.10
*   **Issue #SIMP-1010-3: SNR Decay Modeling.**
    *   **Native Correlation**: Implemented native SNR-Vibration correlation in `jdhardware-jni.cpp` (n19). The model now distinguishes between mechanical interference (high vibe) and electronic jamming (low vibe) during SNR drops (< 18 dB).
    *   **JNI Expansion**: Updated `VibrationBatch` and `JdHardwareManager` to propagate the `isJammingCandidate` flag from native to JVM.
    *   **Sentinel Integration**: Updated `LocationSentinel.checkPhysicalTamper` to transition the engine to `JAMMER_SUSPICION` status when the native candidate flag is set.
    *   **Telemetry Parity**: Integrated the jamming flag into `ForensicSnapshot` and the global `LocationUpdate` container for remote visibility.

## 🟢 Resolved in Oct7.9
*   **Issue #SIMP-1010-2: Muzzle Hysteresis Native Offloading.**
    *   **Native Logic**: Migrated stationary duration tracking and muzzle reset triggers to `jdhardware-jni.cpp`. The native layer now evaluates the 2000ms muzzle window during the 100Hz vibration batch.
    *   **JNI Expansion**: Updated `VibrationBatch` and `JdHardwareManager` to carry `nowRt` into JNI and read back `stationaryDuration` and `muzzleResetTriggered`.
    *   **JVM Purge**: Eliminated `stationaryStartRt` timestamp arithmetic from `HardwareSuite` and `LocationSentinel`, replacing it with direct usage of native-provided duration.
    *   **Resource Optimization**: Reduced JVM overhead in the high-frequency sensor path by consolidating temporal state evaluation into the existing native math block.

## 🟢 Resolved in Oct7.8
*   **Issue #SIMP-1010-1: Adaptive Acoustic Gating.**
    *   **Native Suppression**: Implemented native `n20` logic in `jdhardware-jni.cpp` to scale `ACOUSTIC_EMA` alpha based on `vibrationRollingSum`. This prevents mechanical vibrations from triggering false acoustic alarms.
    *   **Bridge Integration**: Updated `JdHardwareManager.kt` to expose the adaptive alpha calculation with JVM fallbacks for architectural resilience.
    *   **Loop Hardening**: Integrated the adaptive alpha into the high-priority `AcousticMonitor` thread in `HardwareSuite.kt`.
    *   **Visibility**: Propagated the `isSuspiciousNoise` flag through the telemetry pipeline to the Viewer HUD, adding a specific badge for JNI-detected anomalies.

## 🟢 Resolved in Oct7.7
*   **Issue #SIMP-1007-16: Native Anomaly Propagation.**
    *   **Flag Convergence**: Successfully propagated `isSuspiciousNoise` and `isMemoryPressureThrottled` from JNI `VibrationBatch` into the `ForensicSnapshot` and the global `LocationUpdate` monolith.
    *   **Telemetry Hardening**: Integrated anomaly flags into Protobuf (`app_settings.proto`) and JSON mappings to ensure Viewer-side visibility of hardware-level interference and memory stress.
    *   **HUD Integration**: Exposed anomaly state in `DashboardHealthState` and `HudHealthState` for real-time diagnostic reporting.
*   **Issue #SIMP-1007-17: Native Memory Pressure Throttling.**
    *   **Sentinel Guard**: Updated `LocationSentinel.shouldThrottlePolling` to force a throttled GPS interval whenever native heap pressure is detected, preempting background OOM events.

## 🟢 Resolved in Oct7.6
*   **Issue #SIMP-1007-16: JNI FastPath Expansion.**
...
