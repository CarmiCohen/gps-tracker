# GPS Tracker - Event Log Documentation (v8.9.37)

This document describes all events logged by the application, their priorities, colors, and trigger conditions. As of v8.9.37, logs are fully synchronized with the hardened forensic engine and feature **Log Spatial Anchors**.

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
| `[SIREN] Tracker: GPS Stalled` | Important | Red | GPS hardware fix frozen (`GPS_STALL_THRESHOLD_MS` 60s). | Yes |
| `CRITICAL: GPS_HARDWARE_LOCK` | Important | Red | 3 consecutive revival failures. Manual intervention required. | Yes |
| `[SIREN] Tracker: Charger unplugged`| Important | Red | Power disconnection detected. | Yes |
| `[SIREN] Tracker: Geofence` | Important | Red | Distance violation or predictive exit. | Yes |
| `[SIREN] Tracker: Low Battery` | Important | Red | Battery < 20% or charge deficit. | Yes |
| `[SIREN] Tracker: High Temp` | Important | Red | Thermal > 46.0°C. Engaging Cooling Mode. | Yes |
| `[SIREN] Tracker: GPS Gap` | Important | Red | GPS fix age > 60s while connected. | Yes |
| **Physical Sentinel Tier** | | | | |
| `[SIREN] Tracker: Tamper Detected`| Important | Red | Shock, Proximity, or Light violation. | Yes |
| `[SIREN] Tracker: Tilt Alert` | Important | Red | Orientation change > 15°. | Yes |
| `[SIREN] Tracker: Acoustic Alert`| Important | Red | Noise spike > 40dB above baseline. | Yes |
| `[SIREN] Tracker: Lift` | Important | Red | Height change > 0.8m. | Yes |
| `[SIREN] Tracker: Xiaomi System...`| Important | Red | Background permission or autostart missing. Includes 30s Boot Grace (Issue #190). | Yes |

### 2. Physical Violation Details (Teal / Technical)
| Event Text Pattern | Importance | Color | Technical Context |
| :--- | :--- | :--- | :--- |
| `[VIOLATION] Light: [X] lux` | Not Important| Teal | Triggered > 150 lux jump (Issue #414). |
| `[VIOLATION] Shock: [X]G` | Not Important| Teal | Triggered > 0.8g impact. |
| `[VIOLATION] Noise: [X] dB` | Not Important| Teal | Triggered > 40dB over ambient floor. |
| `GPS Stall: Revival attempt [X]/3`| Important | White| Periodic 120s hardware refresh (Issue #198). |

### 3. Resolved & Positive Transitions (Green / Bold)
| Event Text Pattern | Importance | Color | Condition | Siren |
| :--- | :--- | :--- | :--- | :--- |
| `[RESOLVED] [System] restored...` | Important | Green | Alert condition cleared. | Stop |
| `[STATUS] Cooling Mode Engaged` | Important | Green | Engaging 30s GPS polling (Issue #70). | No |
| `[STATUS] Cooling Mode Resolved` | Important | Green | Temperature < 44.0°C. | No |

---

## Technical Specifications
- **Log Spatial Anchor**: All events are automatically tagged with `lat`/`lng` coordinates using the last known telemetry position.
- **Monotonic Stability (Issue #125)**: All forensic timing and UI lockout windows use `elapsedRealtime`.
- **Ghost Mode UX (Issue #193)**: Stale events (>10s) are visualized with dimmed "Ghost" status indicators.
- **Identity Unification**: Every entry carries the mandatory `role` tag (Issue #182).
- **Fuzzy Forensic Batching**: Consecutive similar events are grouped with `(xN)` count and total duration tracking.
- **Muzzle Window (Issue #191)**: 2000ms suppression during sync I/O to prevent false tamper logs.
