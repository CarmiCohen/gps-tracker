# Alarming & Siren Mechanism (v8.9.52)

This document describes the critical alert system of the GPS Tracker, which uses software-synthesized audio, full-screen system intents, and visual overlays to ensure theft or failure events are noticed immediately.

## 1. Alarm Trigger Logic (`MainAlarmLogic.kt`)
The system continuously evaluates tracking data against a set of security rules. An alarm is triggered when one or more of the following conditions are met:
- **Distance Violation**: Tracker moves beyond the fence radius plus a dynamic buffer.
- **Bayesian Uncertainty Expansion**: Thresholds expand at **15m/s** (Moving) or **1.5m/s** (Stationary) during fix gaps, capped at **33.3m/s** (Issue #431).
- **Tracker Tamper**: Real-time monitoring of physical hardware integrity:
    - **Muzzle Window**: 2000ms suppression during sync to prevent false triggers (Issue #191).
    - **Tilt**: > 15° change (`TILT_THRESHOLD_DEGREES`).
    - **Light**: > 150 lux jump (`LIGHT_THRESHOLD_LUX_JUMP`).
    - **Proximity**: Transition to Clear (Far) state.
    - **Shock**: > 0.8g impact (`VIBRATION_SHOCK_THRESHOLD_G`).
- **Other Physical Sentinels**:
    - **Acoustic**: > 40dB jump with a 50dB absolute floor (`ACOUSTIC_FLOOR_MIN_DB`).
    - **Lift**: > 0.8m altitude change (`BARO_LIFT_THRESHOLD_METERS`).
- **System Integrity**:
    - **Storage Watchdog**: Dual-tier alerts (< 50MB Low, < 10MB Critical). (Issue #316)
    - **Signal Loss (Jammer)**: Sustained signal instability for > 180s (`JAMMER_DETECTION_THRESHOLD_MS`).
    - **GPS Stalled**: Hardware chip freeze detected after 60s (`GPS_STALL_THRESHOLD_MS`).
    - **Xiaomi Ready**: Autostart and background restriction monitoring. Includes `XIAOMI_BOOT_GRACE_MS` (30s). (Issue #439)
- **Low Battery**: Level < 20% or steep discharge (5% in 10m).

## 2. Audio Synthesis (`AudioSynthesizer.kt`)
Real-time PCM generation for high-stress alerts:
- **Siren**: Frequency-modulated wail (600Hz to 1400Hz).
- **Time Integrity**: All silence latches use monotonic `elapsedRealtime` to prevent clock-tamper bypass (Issue #441).
- **Silent Override**: System forces audio output regardless of device Do Not Disturb or Silent settings.

## 3. Full-Screen Alert & UI Hardening
When a violation is detected, the system launches a high-priority Red Alert overlay:
- **Standardized Titles (R747)**: Local alerts use "This device:", remote alerts use the simplified ID or "Offline". (Issue #424)
- **Monotonic Timing**: A 30-second lockout applies after dismissal using `TimeProvider.elapsedRealtime()`.

## 4. Forensic Continuity
- **Log Spatial Anchor**: Every alarm trigger and resolution is geographically anchored with `accuracy` and authoritative `maxAccuracy` (Issue #325).
- **Acoustic Lockout**: A 1-second silence window (`ACOUSTIC_LOCKOUT_MS`) prevents redundant slow-path triggers following a fast-path event.
- **Ghost Mode UX**: When telemetry is stale (>10s), the dashboard and markers enter a dimmed state (Issue #338).

## 5. Silencing & Acknowledgment
- **Auto-Stop**: Siren stops after **30s** (`SIREN_AUTO_STOP_MS`) to protect hardware and match recovery delays (Issue #429).
- **Cooldown**: 15s wait period (`SIREN_RESUME_COOLDOWN_MS`) before re-triggering audio.
- **Manual Ack**: Stopping audio via UI also acknowledges the alert and starts the 30s lockout.
