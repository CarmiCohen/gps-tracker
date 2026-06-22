# GPS Tracker: Event and Alert Tables (v8.9.20)

This document summarizes all system alerts, their trigger logic, timing, and how they are presented to the user in both the Event Log and the Red Screen (Alarm Overlay).

---

## Table 1: Siren Timing & Logic (SoT Sync)

| Alert Title | Trigger Condition | Detection / Grace Period | Total Delay to Siren | Logic |
| :--- | :--- | :--- | :--- | :--- |
| **This device: Internet Lost** | Hardware reports NO internet | **60 Seconds** | **60 Seconds** | Aligned with `INTERNET_LOSS_THRESHOLD_MS`. |
| **This device: Relay Lost** | Socket FAIL | **60 Seconds** | **60 Seconds** | Aligned with `COMMUNICATION_ALARM_GRACE_PERIOD_MS`. |
| **Tracker: Offline** | Disconnected | **60 Seconds** | **60 Seconds** | Aligned with `COMMUNICATION_ALARM_GRACE_PERIOD_MS`. |
| **Tracker: Jammer Alert** | GPS jumps (Tier 1/2) | **180 Seconds** | **180 Seconds** | Aligned with `JAMMER_DETECTION_THRESHOLD_MS`. |
| **Tracker: Signal Lost** | No data packets | **180 Seconds** | **180 Seconds** | Aligned with `TRACKER_SIGNAL_LOSS_THRESHOLD_MS`. |
| **Viewer: Signal Lost** | No data packets | **30 Seconds** | **30 Seconds** | Aligned with `VIEWER_SIGNAL_LOSS_THRESHOLD_MS`. |
| **Tracker: GPS Stalled** | No movement | **60 Seconds** | **60 Seconds** | Aligned with `GPS_STALL_THRESHOLD_MS`. |
| **Tracker: GPS Gap** | Stale data (>60s) | **60 Seconds** | **60 Seconds** | Aligned with `GPS_GAP_THRESHOLD_MS`. |
| **Tracker: Geofence** | Out of fence | **60 Seconds** | **60 Seconds** | Aligned with `BOOTSTRAP_PHASE_MS`. Requires 6 samples. |
| **Tracker: Charger unplugged** | USB Power Off | **3 Seconds** | **3 Seconds** | Aligned with `POWER_DISCONNECT_DEBOUNCE_MS`. |
| **Tracker: Low Battery** | Battery < 20% | Immediate | **Immediate** | Siren triggers on first packet (`CRITICAL_BATTERY_THRESHOLD`). |
| **Tracker: High Temp** | Temp > 46.0°C | Immediate | **Immediate** | Siren triggers on first packet (`MAX_SAFE_TEMPERATURE_CELSIUS`). |
| **Tracker: Xiaomi System** | Background Restricted | Immediate | **Immediate** | Siren triggers on background or autostart restriction detection. Includes 30s Boot Grace. |

---

## Table 2: UI Messaging & Display (Alerts)

**All events in this table trigger a Red Screen Alert and Audible Siren.**

| Alert Title | Logic Source | Event Log Text | Alert Text (Red Screen) + [Friendly Explanation] |
| :--- | :--- | :--- | :--- |
| **This device: Internet Lost** | Local Hardware | Internet Connection Lost... | This device has no internet access [Your phone is not connected to the internet] |
| **This device: Relay Lost** | Socket State | Relay Connection Lost... | This device: Relay connection failed [The connection to the tracking server failed] |
| **Tracker: Offline** | Peer Stream | Offline... | Remote device is not connected to relay [The tracker device has lost its connection] |
| **Tracker: Jammer Alert** | Integrity | Jump Alert... | Device data is erratic or jumping [GPS interference detected] |
| **Tracker: Signal Lost** | Watchdog | Signal Lost... | No data received from device for >180s [The tracker has stopped sending updates] |
| **Viewer: Signal Lost** | Watchdog | Viewer: Signal Lost... | No data received from viewer for >30s [The monitoring viewer has stopped responding] |
| **Tracker: GPS Stalled** | Integrity | GPS Stalled... | Device GPS location has not updated [The tracker's position has frozen] |
| **Tracker: GPS Gap** | Telemetry Age | GPS Gap... | Device GPS fix is older than 60s [Tracker location data is stale] |
| **Tracker: Charger unplugged** | Status Flag | Tracker: Charger unplugged... | External power source removed from device [Tracker power cable disconnected] |
| **Tracker: Low Battery** | Status Flag | Tracker: Low Battery... | Device battery level is at X% [Tracker battery is critical] |
| **Tracker: High Temp** | Status Flag | Tracker: High Temp... | Device temperature reached X°C [Tracker is overheating] |
| **Tracker: Geofence** | Location | Tracker: Geofence... | Device is Xm away from home [Tracker moved out of the safe area] |
| **Tracker: Xiaomi System**| Status Flag | Tracker: Xiaomi System... | MIUI restrictions detected [MIUI permissions or autostart denied] |

---

## Table 2.1: Combined Resolution Logic

When an alert is resolved, the app combines the start and end events into a single duration-based log.

| Original Log | New Combined Resolution Log | Friendly Explanation |
| :--- | :--- | :--- |
| [SIREN] Tracker: Offline | **[RESOLVED] Tracker: Offline restored (Duration: 5s)** | [The device was disconnected for 5 seconds but is now back online.] |

---

## Table 3: User Action Logging

These logs track intentional user interactions. They **do not** trigger sirens or red alerts.

| Log Message Pattern | Visibility | Friendly Description [Explanation] |
| :--- | :--- | :--- |
| **USER ACTION: Log button clicked** | **Hidden** | User opened the Logs view. |
| **USER ACTION: Info button clicked** | **Hidden** | User opened the technical dashboard. |
| **USER ACTION: Settings button clicked** | **Hidden** | User opened settings. |
| **USER ACTION: Session Terminated** | Important (Bold) | User stopped tracking manually. |
| **USER ACTION: Alert acknowledged** | Important (Bold) | User acknowledged a Red Screen alert. |
| **USER ACTION: App mode set to [mode]** | Important (Bold) | Selection of device role established. |
| **USER ACTION: Trails cleared** | Visible | User manually erased map history. |
| **USER ACTION: UI Language set to [lang] | Important (Bold) | Interface language changed by user. |
| **USER ACTION: Logs cleared** | Important (Bold) | Event history wiped from database. |

---

## Table 4: Maintenance & Heartbeat Logs

| Log Message Pattern | Role / Component | Explanation |
| :--- | :--- | :--- |
| **Session Heartbeat (Duration: X)** | App Internal | Measures the continuous tracking time for this session. |
| **Global System Heartbeat** | App Internal | Measures the total installation lifetime (`HEARTBEAT_INTERVAL_MS`). |
| **MAINTENANCE: Service is healthy** | Tracker/Viewer | Confirms the service is running correctly (`SYSTEM_WATCH_DOG_INTERVAL_MS`). |
| **MAINTENANCE: RECOVERY triggered** | Watchdog | The service was stopped; the watchdog is restarting it. |
| **MAINTENANCE: Muzzle Active** | Tracker | Sensor triggers suppressed during sync (`MUZZLE_WINDOW_DURATION_MS`). |
| **MAINTENANCE: Log Anchor Applied** | LogManager | Confirms coordinate attachment to a system log (v8.9.10). |
