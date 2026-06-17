# GPS Tracker - Event Log Documentation (v8.8.35)

This document describes all events logged by the application, their priorities, colors, and trigger conditions. As of v8.8.35, logs are enhanced with escalated hardware recovery alerts, monotonic timing, and a simplified forensic model.

## Event Priority Levels
1.  **CRITICAL / ALARM (Important)**: Events that trigger a siren or describe hard system failures. Always visible.
2.  **STATUS UPDATES (Important)**: Significant system state changes (Viewer connections, start/stop, network recovery). Always visible.
3.  **INFO (Not Important)**: Routine updates, minor violations, or background maintenance.
4.  **HIDDEN**: Internal UI actions or diagnostic logs.

---

## Event List (Ordered by Priority)

### 1. Alarms & Critical Alerts (Red / Bold / Siren)
| Event Text Pattern | Importance | Color | Condition | Siren |
| :--- | :--- | :--- | :--- | :--- |
| `This device: Internet Lost` | Important | Red | Local internet failure (`INTERNET_LOSS_THRESHOLD_MS` 60s). | Yes |
| `[SIREN] This device: Relay Lost`| Important | Red | Local relay connection failure. | Yes |
| `[SIREN] Tracker: Jammer Alert` | Important | Red | Sustained GPS instability (`JAMMER_DETECTION_THRESHOLD_MS` 180s). | Yes |
| `[SIREN] Tracker: Signal Lost` | Important | Red | Peer communication timeout (180s TRK / 30s VIEW). | Yes |
| `[SIREN] Tracker: GPS Stalled` | Important | Red | GPS hardware fix frozen (`GPS_STALL_THRESHOLD_MS` 180s). | Yes |
| `CRITICAL: GPS_HARDWARE_LOCK` | Important | Red | **Issue 124**: 3 consecutive revival failures. Manual intervention required. | Yes |
| `[SIREN] Tracker: Charger unplugged`| Important | Red | Power disconnection detected. | Yes |
| `[SIREN] Tracker: Geofence` | Important | Red | Distance violation or predictive exit. | Yes |
| `[SIREN] Tracker: Low Battery` | Important | Red | Battery < 20% or charge deficit. | Yes |
| `[SIREN] Tracker: High Temp` | Important | Red | Thermal > 46.0°C. Engaging Cooling Mode. | Yes |
| `[SIREN] Tracker: GPS Gap` | Important | Red | GPS fix age > 180s while connected. | Yes |
| **Physical Sentinel Tier** | | | | |
| `[SIREN] Tracker: Tamper Detected`| Important | Red | Shock, Proximity, or Light violation. | Yes |
| `[SIREN] Tracker: Tilt Alert` | Important | Red | Orientation change > 15°. | Yes |
| `[SIREN] Tracker: Acoustic Alert`| Important | Red | Noise spike > 40dB above baseline. | Yes |
| `[SIREN] Tracker: Lift` | Important | Red | Height change > 0.8m. | Yes |
| `[SIREN] Tracker: Xiaomi System...`| Important | Red | Background permission or autostart missing. | Yes |

### 2. Physical Violation Details (Teal / Technical)
| Event Text Pattern | Importance | Color | Technical Context |
| :--- | :--- | :--- | :--- |
| `[VIOLATION] Light: [X] lux` | Not Important| Teal | Triggered > 150 lux jump. |
| `[VIOLATION] Shock: [X]G` | Not Important| Teal | Triggered > 0.8g impact. |
| `[VIOLATION] Noise: [X] dB` | Not Important| Teal | Triggered > 40dB over ambient floor. |
| `GPS Stall: Revival attempt [X]/3`| Important | White| **Issue 124**: Periodic 5m hardware refresh. |

### 3. Resolved & Positive Transitions (Green / Bold)
| Event Text Pattern | Importance | Color | Condition | Siren |
| :--- | :--- | :--- | :--- | :--- |
| `[RESOLVED] [System] restored...` | Important | Green | Alert condition cleared. | Stop |
| `[STATUS] Cooling Mode Engaged` | Important | Green | Engaging 30s GPS polling. | No |
| `[STATUS] Cooling Mode Resolved` | Important | Green | Temperature < 44.0°C. | No |

---

## Technical Specifications
- **Monotonic Stability (Issue 125)**: All forensic timing and UI lockout windows use `elapsedRealtime` to ensure immunity to system clock jumps.
- **Identity Unification**: Every entry carries the mandatory `role` tag. Legacy version tags (`ver`, `vid`) have been removed from the data stream.
- **Fuzzy Forensic Batching**: Consecutive similar events are grouped with `(xN)` count and total duration tracking.
- **Muzzle Window**: 500ms suppression during sync I/O to prevent false tamper logs.
