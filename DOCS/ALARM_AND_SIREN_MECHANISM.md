# Alarming & Siren Mechanism (July.23.11)

This document describes the critical alert system of the GPS Tracker, which uses software-synthesized audio, full-screen system intents, and visual overlays to ensure theft or failure events are noticed immediately.

## 1. Alarm Trigger Logic (`MainAlarmLogic.kt`)
The system continuously evaluates tracking data against a set of security rules. An alarm is triggered when one or more of the following conditions are met:
- **Distance Violation**: Tracker moves beyond the fence radius plus a dynamic buffer.
- **Tracker Tamper**: Real-time monitoring of physical hardware integrity (Tilt, Light, Proximity, Shock).
- **System Integrity**: Storage Watchdog, Signal Loss (Jammer), and GPS Stalled detection.
- **Low Battery**: Level < 20% or steep discharge (5% in 10m).

## 2. Audio Synthesis & Role-Based Silence (R872)
Real-time PCM generation for high-stress alerts:
- **Siren**: Frequency-modulated wail (600Hz to 1400Hz).
- **Stealth Requirement (Tracker Mode)**: When operating in Tracker mode, all local audio synthesis MUST be suppressed via `AppAlarmManager.shouldPlaySiren()`. The device remains strictly silent.
- **Viewer Mode**: System forces audio output regardless of device Do Not Disturb or Silent settings.

## 3. Full-Screen Alert & UI Hardening
When a violation is detected, the system launches a high-priority Red Alert overlay:
- **Stealth Requirement (Tracker Mode)**: Visual alerts are suppressed in Tracker mode. The device remains dark. Violations are transmitted to the Viewer only.
- **Standardized Titles (R747)**: Local alerts use "This device:", remote alerts use the simplified ID or "Offline".
- **Monotonic Timing**: A 30-second lockout applies after dismissal using `TimeProvider.elapsedRealtime()`.

## 4. Forensic Continuity
- **Log Spatial Anchor**: Every alarm trigger and resolution is geographically anchored with `accuracy` and authoritative `maxAccuracy`.
- **Acoustic Lockout**: A 1-second silence window prevents redundant triggers.

## 5. Silencing & Acknowledgment
- **Auto-Stop**: Siren stops after **30s** (`SIREN_AUTO_STOP_MS`) to protect hardware.
- **Cooldown**: 15s wait period (`SIREN_RESUME_COOLDOWN_MS`) before re-triggering audio.
- **Manual Ack**: Stopping audio via UI also acknowledges the alert and starts the 30s lockout.
