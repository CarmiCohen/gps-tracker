# Alarming & Siren Mechanism (v8.8.35)

This document describes the critical alert system of the GPS Tracker, which uses software-synthesized audio, full-screen system intents, and visual overlays to ensure theft or failure events are noticed immediately.

## 1. Alarm Trigger Logic (`MainAlarmLogic.kt`)
The system continuously evaluates tracking data against a set of security rules. An alarm is triggered when one or more of the following conditions are met:
- **Distance Violation**: Tracker moves beyond the fence radius plus a dynamic buffer.
- **Tracker Tamper**: Real-time monitoring of physical hardware integrity:
    - **Muzzle Window**: 500ms suppression during sync to prevent false triggers.
    - **Tilt**: > 15° change.
    - **Light**: > 150 lux jump.
    - **Proximity**: Transition to Clear (Far) state.
    - **Shock**: > 0.8g impact.
- **Other Physical Sentinels**:
    - **Acoustic**: > 40dB jump with a 50dB absolute floor.
    - **Lift**: > 0.8m altitude change.
- **System Integrity**:
    - **Storage Watchdog**: Dual-tier alerts (< 50MB Low, < 10MB Critical).
    - **Jammer Alert**: Sustained signal instability for > 180s.
    - **GPS Stalled**: Hardware chip freeze detected after 180s.
        - **Escalated Revival (Issue 124)**: System retries hardware refresh every 5m and escalates to CRITICAL after 3 failures.
    - **Xiaomi Ready**: Autostart and background restriction monitoring.
- **Low Battery**: Level < 20% or steep discharge (5% in 10m).

## 2. Audio Synthesis (`AudioSynthesizer.kt`)
Real-time PCM generation for high-stress alerts:
- **Siren**: Frequency-modulated wail (600Hz to 1400Hz).
- **Global Mute**: Users can inhibit audio while preserving visual/forensic logging.
- **Silent Override**: System forces audio output regardless of device Do Not Disturb or Silent settings.

## 3. Full-Screen Alert & UI Hardening
When a violation is detected in Viewer mode, the system launches a high-priority Red Alert overlay:
- **Unified Titles**: Remote alerts use "Tracker:", local alerts use "This device:".
- **Monotonic Timing (Issue 125)**: A 30-second lockout (`ALARM_OVERLAY_THROTTLE_MS`) applies after dismissal. This timer uses `elapsedRealtime` to ensure stability across system clock jumps.
- **Violation Bypass**: The lockout prevents redundant activity launches but does *not* suppress data propagation. New violation types always update the UI immediately.

## 4. Forensic Continuity
- **Acoustic Lockout**: A 1-second silence window prevents redundant slow-path triggers following a fast-path event.
- **Identity Integrity**: Every alert and resolution carries the mandatory `role` field. Identity is preserved at the emission point (LogManager/SyncManager) via `BuildConfig.VERSION_NAME`. The legacy `ver` field has been removed.

## 5. Silencing & Acknowledgment
- **Auto-Stop**: Siren stops after 45s (`SIREN_AUTO_STOP_MS`) to protect hardware.
- **Cooldown**: 15s wait period (`SIREN_RESUME_COOLDOWN_MS`) before re-triggering audio.
- **Manual Ack**: Stopping audio via UI also acknowledges the alert and starts the 30s lockout.
