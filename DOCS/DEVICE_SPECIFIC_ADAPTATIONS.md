# Device-Specific Adaptations (v9.4.0)

This document describes the specialized logic and configurations implemented to maintain background persistence across OEM-specific hardware.

## 1. Samsung Optimization (R405/R406a)
### 1.1. Unified Heartbeat
The system avoids aggressive OEM suppression by standardizing all operations to a 2000ms (`TICK_INTERVAL_MS`) heartbeat. This replaces previous device-specific high-frequency polling (10Hz) which caused excessive battery drain and thermal throttling.

### 1.2. Virtual Proximity Debouncing (Samsung A15)
The Samsung A15 utilizes a virtual proximity sensor that is prone to "flickering" during stationary tracking.
- **Window**: 5,000ms (`PROXIMITY_DEBOUNCE_STATIONARY_A15_MS`).
- **Logic**: Transitions to "Far" are ignored unless sustained for the full window. (Issue #191)

### 1.3. WakeLock Renewal
On Samsung devices, the system proactively renews its WakeLock during every heartbeat tick in `processTick` to prevent the OS from suspending the location lifecycle.

### 1.4. Identity Swap & Ghost Loads (R212/Issue #251)
To eliminate framework collisions, all legacy native references were migrated from `mbrainSDK` to `jdHardware` (R212). 
- **Behavior**: On some devices (e.g., A15), the Samsung CFMS (Configurable Floating Management Service) may attempt to load `libmbrainSDK` if it detects high-frequency JNI direct-buffer patterns formerly associated with that library name.
- **Status**: This is a **benign OS-level heuristic**. The application correctly initializes `libjdHardware` and does not require the legacy binary. A diagnostic log in `JdHardwareManager` confirms the Identity Swap is active.

## 2. Xiaomi (MIUI/HyperOS) Hardening (Issue #439)
### 2.1. Autostart Verification
The system monitors `isXiaomiAutostartGranted`. If false, a critical `XIAOMI_SYSTEM_MISSING` alert is triggered.

### 2.2. Boot Resilience
Implemented `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient "System Not Ready" alarms during the MIUI/HyperOS boot phase.

### 2.3. Heuristic Recovery Pulse
- **Detection**: Monitors tick gaps. If the gap exceeds 45s (`XIAOMI_SUPPRESSION_THRESHOLD_MS`), suppression is assumed.
- **Action**: Triggers a "Revival Pulse" (GPS refresh + WakeLock renewal). This is limited by a 60s recovery cooldown.

### 2.4. Stability Audit Suite
Implemented a GPS Stability Audit suite to verify heartbeat persistence.
- **Audit Loop**: Every 10s (`GPS_STABILITY_AUDIT_INTERVAL_MS`).
- **Forensic Escalation**: If a gap > 200ms (`GPS_STABILITY_GAP_THRESHOLD_MS`) relative to the 2s heartbeat is detected, a "STABILITY GAP" forensic log is emitted.

## 3. General OEM Throttling
### 3.1. Standby Bucket Monitoring
The `IntegrityMonitor` tracks Standby Buckets. Transitions to `RARE` or `RESTRICTED` are logged.

## 4. Detection & Watchdog
- **GPS Availability Hardening**: GPS stall detection is 60s (`GPS_STALL_THRESHOLD_MS`) and revival retry is 120s (`GPS_REVIVAL_RETRY_INTERVAL_MS`).
- **Log Spatial Anchor**: All OEM-specific transitions and stability logs are anchored with Dual-Metric spatial data (Issue #325).
- **Monotonic Timing**: Siren locks and UI overlays use `TimeProvider.elapsedRealtime()` to ensure stability under clock regression (Issue #441).
