# Device-Specific Adaptations (v8.9.18)

This document describes the specialized logic and polling configurations implemented to bypass OEM-specific background restrictions on supported hardware.

## 1. Samsung Optimization
### 1.1. High-Frequency Polling (Samsung S21 FE)
To prevent the OS from suspending the location lifecycle, the `TrackerService` forces a 10Hz polling interval (`HIGH_FREQUENCY_GPS_POLLING_MS` = 100ms) on Samsung S21 FE devices. This high-frequency pulse maintains active process priority even during screen-off states.

### 1.2. Virtual Proximity Debouncing (Samsung A15)
The Samsung A15 utilizes a virtual proximity sensor that is prone to "flickering" during stationary tracking.
- **Window**: 5,000ms (`PROXIMITY_DEBOUNCE_STATIONARY_A15_MS`).
- **Logic**: Transitions to "Far" are ignored unless sustained for the full window, preventing false tamper triggers.

### 1.3. GPS Polling Stabilization (Samsung A15)
To ensure long-term stability on the A15 hardware, the system enforces a 1000ms polling interval (`A15_STABLE_GPS_POLLING_MS`). This prevents the GPS hardware from entering aggressive power-save modes.

### 1.4. Power Tamper Hardening (General Samsung)
Issue 163: Restored power tamper detection by reconnecting callbacks to `IntegrityMonitor`. Hardened power detection using `EXTRA_PLUGGED` to supplement broadcast receivers. Integrated `onViolationSustained` for Thermal, Battery Health, and Storage alerts.

## 2. Xiaomi (MIUI/HyperOS) Hardening
### 2.1. Autostart Verification
The system monitors `isXiaomiAutostartGranted`. If false, a critical `XIAOMI_SYSTEM_MISSING` alert is triggered.

### 2.2. Boot Resilience (Issue 190)
Implemented `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient "System Not Ready" or "Denied" alarms during the MIUI/HyperOS boot transition phase.

### 2.3. Heuristic Recovery Pulse (Issue #218)
To counter aggressive background suppression in MIUI 14+, the system implements a heuristic watchdog.
- **Detection**: Monitors the gap between service ticks using monotonic time. If the gap exceeds 15s (`XIAOMI_SUPPRESSION_THRESHOLD_MS`), suppression is assumed.
- **Action**: Triggers an aggressive "Revival Pulse" consisting of GPS hardware refresh, WakeLock renewal, and a Foreground Service Type update to force OS re-prioritization.

### 2.4. Manual Override (R928)
Users can toggle a "Manual Override" in the Sound Setup to bypass the logic-gate for "UNKNOWN" permission states, allowing the engine to remain active.

### 2.5. Background Persistence
Integrated `isXiaomiAutostartGranted` check in `TrackerService.kt` alarm loop and enabled `HIGH_FREQUENCY_GPS_POLLING_MS` (10Hz) for Xiaomi devices to ensure background stability parity with Samsung S21 FE.

### 2.6. Stability Audit Suite (Issue 168)
Implemented a GPS Stability Audit suite in `TrackerService` to verify 10Hz persistence on physical Xiaomi hardware.
- **Metrics**: Tracks fix arrival counts, inter-fix gaps (ms), and max gap observed.
- **Audit Loop**: Every 10s (`GPS_STABILITY_AUDIT_INTERVAL_MS`), the system emits a "STABILITY AUDIT" log reporting the reliability percentage.
- **Forensic Escalation**: If a gap > 1000ms (`GPS_STABILITY_GAP_THRESHOLD_MS`) is detected during 10Hz polling, a "STABILITY GAP" forensic log is emitted.

## 3. General OEM Throttling
### 3.1. Standby Bucket Monitoring
The `IntegrityMonitor` tracks the app's current Standby Bucket (via `UsageStatsManager`). Transitions to `RARE` or `RESTRICTED` buckets are logged as forensic events.

### 3.2. Power Save Mode
When the OS enters Power Save mode, the system automatically transitions to `TICK_INTERVAL_SLOW_MS` (5s) for background idle, while maintaining the `TrackerService` as a `FOREGROUND_SERVICE_TYPE_LOCATION`.

## 4. Detection & Watchdog
- **GPS Availability Hardening (Issue 198)**: Shortened GPS stall detection to 60s (`GPS_STALL_THRESHOLD_MS`) and revival retry to 120s (`GPS_REVIVAL_RETRY_INTERVAL_MS`) to ensure high-availability tracking on restricted hardware.
- **Log Spatial Anchor (v8.9.10)**: All OEM-specific status transitions and stability audit logs are now automatically anchored with `lat`/`lng` coordinates to enable map reconstruction.
- **Monotonic Timing (Issue 125)**: The system monitors `ALARM_OVERLAY_THROTTLE_MS` (30s) using monotonic time (`elapsedRealtime`) to ensure that OEM-induced system clock jumps do not break the UI lockout logic.
- **Forensic Unification**: Legacy `ver` and `vid` tags have been removed to simplify the forensic model while preserving high-availability tracking on all OEM hardware.
