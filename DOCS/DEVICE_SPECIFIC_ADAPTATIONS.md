# Device-Specific Adaptations (v8.9.52)

This document describes the specialized logic and polling configurations implemented to bypass OEM-specific background restrictions on supported hardware.

## 1. Samsung Optimization
### 1.1. High-Frequency Polling (Samsung S21 FE)
To prevent the OS from suspending the location lifecycle, the `TrackerService` forces a 10Hz polling interval (`HIGH_FREQUENCY_GPS_POLLING_MS` = 100ms) on Samsung S21 FE devices. (Issue #432)

### 1.2. Virtual Proximity Debouncing (Samsung A15)
The Samsung A15 utilizes a virtual proximity sensor that is prone to "flickering" during stationary tracking.
- **Window**: 5,000ms (`PROXIMITY_DEBOUNCE_STATIONARY_A15_MS`).
- **Logic**: Transitions to "Far" are ignored unless sustained for the full window. (Issue #191)

### 1.3. GPS Polling Stabilization (Samsung A15)
The system enforces a 1000ms polling interval (`A15_STABLE_GPS_POLLING_MS`) to prevent the GPS hardware from entering aggressive power-save modes. (Issue #363)

## 2. Xiaomi (MIUI/HyperOS) Hardening (Issue #439)
### 2.1. Autostart Verification
The system monitors `isXiaomiAutostartGranted`. If false, a critical `XIAOMI_SYSTEM_MISSING` alert is triggered.

### 2.2. Boot Resilience
Implemented `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient "System Not Ready" alarms during the MIUI/HyperOS boot phase.

### 2.3. Heuristic Recovery Pulse
- **Detection**: Monitors tick gaps. If the gap exceeds 15s (`XIAOMI_SUPPRESSION_THRESHOLD_MS`), suppression is assumed.
- **Action**: Triggers a "Revival Pulse" (GPS refresh + WakeLock renewal). This is limited by a 60s recovery cooldown.

### 2.4. Stability Audit Suite
Implemented a GPS Stability Audit suite in `TrackerService` to verify 10Hz persistence.
- **Audit Loop**: Every 10s (`GPS_STABILITY_AUDIT_INTERVAL_MS`).
- **Forensic Escalation**: If a gap > 200ms (`GPS_STABILITY_GAP_THRESHOLD_MS`) is detected during 10Hz polling, a "STABILITY GAP" forensic log is emitted.

## 3. General OEM Throttling
### 3.1. Standby Bucket Monitoring
The `IntegrityMonitor` tracks Standby Buckets. Transitions to `RARE` or `RESTRICTED` are logged.

### 3.2. Power Save Mode
System transitions to `TICK_INTERVAL_SLOW_MS` (5s) for background idle, while maintaining the `TrackerService` as a `FOREGROUND_SERVICE_TYPE_LOCATION`.

## 4. Detection & Watchdog
- **GPS Availability Hardening**: GPS stall detection is 60s (`GPS_STALL_THRESHOLD_MS`) and revival retry is 120s (`GPS_REVIVAL_RETRY_INTERVAL_MS`).
- **Log Spatial Anchor**: All OEM-specific transitions and stability logs are anchored with Dual-Metric spatial data (Issue #325).
- **Monotonic Timing**: Siren locks and UI overlays use `TimeProvider.elapsedRealtime()` to ensure stability under clock regression (Issue #441).
