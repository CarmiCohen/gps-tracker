# Alarming Requirements (Source of Truth) - Audit Oct.01.24

## 🧠 1. Logic & Trigger Requirements
*   **Central Authority**: `MainAlarmLogic` (Engine) detects violations; `AppAlarmManager` (App) manages siren state.
*   **Trigger Conditions**:
    *   **Geofence**: Distance > `maxDistance` + (Accuracy Buffer). Requires 6 samples (`DISTANCE_ALARM_SAMPLES_REQUIRED`) or Predictive Exit.
    *   **Tamper**: Combined check of Shock (>0.8G), Tilt (>15°), Light (>150 lux), Proximity (Near=false), Lift (>0.8m), and Power.
    *   **Connectivity (Special Status)**: Local Internet, Relay Offline, Peer Offline, Signal Loss, GPS Stall, GPS Gap.
    *   **System Health**: Critical Battery (<10%), Low Battery (<20%), Steep Discharge, High Temp (>46°C), Critical Storage (<10MB).
*   **Suppression Rules**:
    *   **Bootstrap/Warmup**: No Geofence or Signal Loss alarms during first 60s (`BOOTSTRAP_PHASE_MS`).
    *   **Connectivity Alarms (Issue #1401)**: MUST NOT trigger siren or full-screen red alerts (Notification-only).
    *   **Stealth Enforcement (R872)**: Tracker Mode MUST be strictly silent and dark (No local UI/Audio).

## 🔊 2. Siren & Audio Requirements
*   **Behavioral Authority**: `SirenLockoutUseCase` is the central domain authority for silences.
*   **Siren Duration**: Auto-stop after 30s (`SIREN_AUTO_STOP_MS`).
*   **Cooldowns**:
    *   **Auto-Stop Cooldown**: 15s (`SIREN_RESUME_COOLDOWN_MS`) before re-triggering.
    *   **Manual Mute Lockout**: SOT (`ALARM_AND_SIREN_MECHANISM.md`) specifies **30s** lockout after dismissal/mute.
*   **Override**: Viewer mode forces audio output regardless of DND/Silent settings.

## 📺 3. Screen & UI Requirements
*   **Red Alert Overlay**: High-priority full-screen red overlay for "Special" (Siren-triggering) alarms.
*   **System-Wide Overlay (Issue #1402-B)**: Must overlay whatever is on the screen, even if another app is in the foreground.
*   **Z-Index (Issue #1402)**: Must obscure all other internal UI elements (Logs, Settings, Ribbons).
*   **Dismissal**: Manual "Stop" acknowledges the alert and starts the lockout period.
*   **Stealth**: UI suppressed in Tracker mode.

## 📝 4. Events & Persistence
*   **Forensic Anchoring**: Every trigger/resolution must be geo-anchored with `accuracy` and `maxAccuracy`.
*   **Monotonic Timing**: All lockout and duration checks MUST use `TimeProvider.elapsedRealtime()`.
*   **State Persistence**: `AppAlarmManager` must persist `lastSirenStopRt` and `activeAlarms` across restarts/reboots via `BootLifecycleAuthority`.
