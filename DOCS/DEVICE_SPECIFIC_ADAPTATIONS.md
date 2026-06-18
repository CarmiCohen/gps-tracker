# Device-Specific Adaptations (v8.9.2)

This document describes the specialized logic and polling configurations implemented to bypass OEM-specific background restrictions on supported hardware.

## 1. Samsung Optimization
### 1.1. High-Frequency Polling (Samsung S21 FE)
To prevent the OS from suspending the location lifecycle, the `TrackerService` forces a 10Hz polling interval (`HIGH_FREQUENCY_GPS_POLLING_MS` = 100ms) on Samsung S21 FE devices. This high-frequency pulse maintains active process priority even during screen-off states.

### 1.2. Virtual Proximity Debouncing (Samsung A15)
The Samsung A15 utilizes a virtual proximity sensor that is prone to "flickering" during stationary tracking.
- **Window**: 5,000ms (`PROXIMITY_DEBOUNCE_STATIONARY_A15_MS`).
- **Logic**: Transitions to "Far" are ignored unless sustained for the full window, preventing false tamper triggers.

### 1.3. GPS Polling Stabilization (Samsung A15)
To ensure long-term stability on the A15 hardware, the system enforces a 1000ms polling interval (`A15_STABLE_GPS_POLLING_MS`). This prevents the GPS hardware from entering aggressive power-save modes that were observed when using standard moving/stationary intervals.

### 1.4. Power Tamper Hardening (General Samsung)
Issue 163: Restored power tamper detection by reconnecting callbacks to `IntegrityMonitor`. Hardened power detection in `IntegrityMonitor.pollSystemStatus` using `EXTRA_PLUGGED` to supplement broadcast receivers. Integrated `onViolationSustained` for Thermal, Battery Health, and Storage alerts to ensure reliable background reporting on Samsung devices.

## 2. Xiaomi (MIUI/HyperOS) Hardening
### 2.1. Autostart Verification
The system monitors `isXiaomiAutostartGranted`. If false, a critical `XIAOMI_SYSTEM_MISSING` alert is triggered, as the app cannot reliably recover from system reboots or background kills without this permission.

### 2.2. Manual Override (R928)
Due to the non-standard nature of Xiaomi's background management, users can toggle a "Manual Override" in the Sound Setup. This bypasses the logic-gate for "UNKNOWN" permission states, allowing the engine to remain active.

### 2.3. Background Persistence
Integrated `isXiaomiAutostartGranted` check in `TrackerService.kt` alarm loop and enabled `HIGH_FREQUENCY_GPS_POLLING_MS` (10Hz) for Xiaomi devices to ensure background stability parity with Samsung S21 FE.

### 2.4. Stability Audit Suite (Issue 168)
Implemented a GPS Stability Audit suite in `TrackerService` to verify 10Hz persistence on physical Xiaomi hardware.
- **Metrics**: Tracks fix arrival counts, inter-fix gaps (ms), and max gap observed.
- **Audit Loop**: Every 10s (`GPS_STABILITY_AUDIT_INTERVAL_MS`), the system emits a "STABILITY AUDIT" log reporting the reliability percentage.
- **Forensic Escalation**: If a gap > 1000ms (`GPS_STABILITY_GAP_THRESHOLD_MS`) is detected during 10Hz polling, a "STABILITY GAP" forensic log is emitted to signal potential system-level background throttling.

## 3. General OEM Throttling
### 3.1. Standby Bucket Monitoring
The `IntegrityMonitor` tracks the app's current Standby Bucket (via `UsageStatsManager`). Transitions to `RARE` or `RESTRICTED` buckets are logged as forensic events to explain potential telemetry gaps.

### 3.2. Power Save Mode
When the OS enters Power Save mode, the system automatically transitions to `TICK_INTERVAL_SLOW_MS` (5s) for background idle, while maintaining the `TrackerService` as a `FOREGROUND_SERVICE_TYPE_LOCATION` to ensure the tracking loop is not terminated.

## 4. Detection & Watchdog
- **Monotonic Timing (Issue 125)**: The system monitors `ALARM_OVERLAY_THROTTLE_MS` (30s) using monotonic time (`elapsedRealtime`) to ensure that OEM-induced system clock jumps do not break the UI lockout logic.
- **Polling**: 10Hz polling is automatically applied to any device identifying as `samsung` or `xiaomi` if background stability issues are detected during the `BOOTSTRAP` phase.
- **Forensic Unification**: Legacy `ver` and `vid` tags have been removed in v8.8.35 to simplify the forensic model while preserving high-availability tracking on all OEM hardware.
