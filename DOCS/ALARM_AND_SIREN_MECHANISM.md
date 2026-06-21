# Alarming & Siren Mechanism (v8.9.10)

This document describes the critical alert system of the GPS Tracker, which uses software-synthesized audio, full-screen system intents, and visual overlays to ensure theft or failure events are noticed immediately.

## 1. Alarm Trigger Logic (`MainAlarmLogic.kt`)
The system continuously evaluates tracking data against a set of security rules. An alarm is triggered when one or more of the following conditions are met:
- **Distance Violation**: Tracker moves beyond the fence radius plus a dynamic buffer.
- **Tracker Tamper**: Real-time monitoring of physical hardware integrity:
    - **Muzzle Window**: 2000ms suppression during sync to prevent false triggers.
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
    - **GPS Stalled**: Hardware chip freeze detected after 60s (`GPS_STALL_THRESHOLD_MS`).
        - **Escalated Revival**: System retries hardware refresh every 120s and escalates to CRITICAL after 3 failures.
    - **Xiaomi Ready**: Autostart and background restriction monitoring. Includes `XIAOMI_BOOT_GRACE_MS` (30s).
- **Low Battery**: Level < 20% or steep discharge (5% in 10m).

## 2. Audio Synthesis (`AudioSynthesizer.kt`)
Real-time PCM generation for high-stress alerts:
- **Siren**: Frequency-modulated wail (600Hz to 1400Hz).
- **Global Mute**: Users can inhibit audio while preserving visual/forensic logging.
- **Silent Override**: System forces audio output regardless of device Do Not Disturb or Silent settings.

## 3. Full-Screen Alert & UI Hardening
When a violation is detected in Viewer mode, the system launches a high-priority Red Alert overlay:
- **Unified Titles**: Remote alerts use "Tracker:", local alerts use "This device:".
- **Monotonic Timing**: A 30-second lockout applies after dismissal. This timer uses `elapsedRealtime`.
- **Violation Bypass**: The lockout prevents redundant activity launches but does *not* suppress data propagation.

## 4. Forensic Continuity
- **Log Spatial Anchor (v8.9.10)**: Every alarm trigger and resolution is geographically anchored. The event log now includes the exact location where the siren was engaged, aiding in asset recovery.
- **Acoustic Lockout**: A 1-second silence window prevents redundant slow-path triggers following a fast-path event.
- **Identity Integrity**: Every alert and resolution carries the mandatory `role` field. 
- **Ghost Mode UX**: When telemetry is stale (>10s), the dashboard and markers enter a dimmed state.

## 5. Silencing & Acknowledgment
- **Auto-Stop**: Siren stops after 45s to protect hardware.
- **Cooldown**: 15s wait period before re-triggering audio.
- **Manual Ack**: Stopping audio via UI also acknowledges the alert and starts the 30s lockout.
