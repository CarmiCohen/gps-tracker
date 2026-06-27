# Alarming & Siren Mechanism (v8.9.37)

This document describes the critical alert system of the GPS Tracker, which uses software-synthesized audio, full-screen system intents, and visual overlays to ensure theft or failure events are noticed immediately.

## 1. Alarm Trigger Logic (`MainAlarmLogic.kt`)
The system continuously evaluates tracking data against a set of security rules. An alarm is triggered when one or more of the following conditions are met:
- **Distance Violation**: Tracker moves beyond the fence radius plus a dynamic buffer.
- **Tracker Tamper**: Real-time monitoring of physical hardware integrity:
    - **Muzzle Window**: 2000ms suppression during sync to prevent false triggers (Issue #191).
    - **Tilt**: > 15° change (`TILT_THRESHOLD_DEGREES`).
    - **Light**: > 150 lux jump (`LIGHT_THRESHOLD_LUX_JUMP`).
    - **Proximity**: Transition to Clear (Far) state.
    - **Shock**: > 0.8g impact (`VIBRATION_SHOCK_THRESHOLD_G`).
- **Other Physical Sentinels**:
    - **Acoustic**: > 40dB jump with a 50dB absolute floor (`ACOUSTIC_MIN_THRESHOLD_DB`).
    - **Lift**: > 0.8m altitude change (`BARO_LIFT_THRESHOLD_METERS`).
- **System Integrity**:
    - **Storage Watchdog**: Dual-tier alerts (< 50MB Low, < 10MB Critical). (Issue #71)
    - **Jammer Alert**: Sustained signal instability for > 180s (`JAMMER_DETECTION_THRESHOLD_MS`).
    - **GPS Stalled**: Hardware chip freeze detected after 60s (`GPS_STALL_THRESHOLD_MS`). (Issue #198)
        - **Escalated Revival**: System retries hardware refresh every 120s and escalates to CRITICAL after 3 failures. (Issue #124)
    - **Xiaomi Ready**: Autostart and background restriction monitoring. Includes `XIAOMI_BOOT_GRACE_MS` (30s). (Issue #190)
- **Low Battery**: Level < 20% or steep discharge (5% in 10m). (Issue #353)

## 2. Audio Synthesis (`AudioSynthesizer.kt`)
Real-time PCM generation for high-stress alerts:
- **Siren**: Frequency-modulated wail (600Hz to 1400Hz).
- **Global Mute**: Users can inhibit audio while preserving visual/forensic logging.
- **Silent Override**: System forces audio output regardless of device Do Not Disturb or Silent settings.

## 3. Full-Screen Alert & UI Hardening
When a violation is detected in Viewer mode, the system launches a high-priority Red Alert overlay:
- **Unified Titles**: Remote alerts use "Tracker:", local alerts use "This device:". (Issue #230)
- **Monotonic Timing**: A 30-second lockout (`ALARM_OVERLAY_THROTTLE_MS`) applies after dismissal. This timer uses `TimeProvider.elapsedRealtime()`. (Issue #125)
- **Violation Bypass**: The lockout prevents redundant activity launches but does *not* suppress data propagation.

## 4. Forensic Continuity
- **Log Spatial Anchor**: Every alarm trigger and resolution is geographically anchored. The event log includes the exact location where the siren was engaged.
- **Acoustic Lockout**: A 1-second silence window (`ACOUSTIC_LOCKOUT_MS`) prevents redundant slow-path triggers following a fast-path event.
- **Identity Integrity**: Every alert and resolution carries the mandatory `role` field. (Issue #182)
- **Ghost Mode UX**: When telemetry is stale (>10s), the dashboard and markers enter a dimmed state. (Issue #193)

## 5. Silencing & Acknowledgment
- **Auto-Stop**: Siren stops after 45s (`SIREN_AUTO_STOP_MS`) to protect hardware.
- **Cooldown**: 15s wait period (`SIREN_RESUME_COOLDOWN_MS`) before re-triggering audio.
- **Manual Ack**: Stopping audio via UI also acknowledges the alert and starts the 30s lockout.
