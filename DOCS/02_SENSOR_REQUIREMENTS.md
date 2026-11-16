# Project Revolution: Sensor (Tracker) Requirements

## 1. Functional Overview
The Sensor is a high-precision, background-persistent engine and acts as the authority for environment monitoring.

## 2. 100% Kotlin Sensing Engine
- **Spoofing Detection**: Cross-verifies GPS velocity with IMU integrals. Triggers alert if GPS is static while IMU detects motion.
- **Orientation Independence**: Uses gravity-vector rotation for all IMU math.
- **Thermal Governance**: Scales from 10Hz to 1Hz if battery temp > 45°C.

## 3. Connectivity Reliability & Relay Configuration
- **Configurable Relay URL**: Allows full user control over the Relay Server URL via `SettingsOverlay` in `AdminActivity.kt` (`:app-sensor`). Includes `TextField` input, **APPLY RELAY URL**, and **RESET DEFAULT** actions.
- **Reactive Re-connection**: `SensorViewModel` and `SensorService` reactively monitor `sensorDataStore.relayUrl` and dynamically reconnect `SignalingClient` when the URL changes.
- **APN Auto-Heal**: If Relay is lost for > 4h, cycles through common cellular APN configurations to restore internet.
- **Metered Awareness**: Drops to 2Hz if on a metered cellular plan to save data costs.

## 4. Security & HUD Admin Console
- **Upper Menu Header**: Top control line in `AdminActivity` containing Setup (`Build`), Settings (`Settings`), and Close controls.
- **Sensor Setup**: Accessible via the Setup icon; displays Device ID, active Relay URL, App Version, and Service Status, with a **CHANGE RELAY URL** shortcut. The Info icon opens the detailed Info screen (Nov15.5, ISSUE-NOV10.8-O84 part 2), the Graphs icon the graphs of its minute samples, kept 7 days in device-protected storage and sent to a Monitor whose Graphs ask for them (Nov15.6, part 3; see `UPPER_MENU_LINE_AND_VIEWS.md`).
- **Passcode Protection**: Admin HUD requires a 4-digit PIN for access to calibration and config.
- **Setup QR**: Displays a secure pairing code containing TrackerID + SecretKey.
- **Notification Stealth**: Titles are non-descriptive (e.g., "System Maintenance") to avoid thief detection.

## 5. Persistence & Direct Boot
- **Direct Boot Aware**: Service starts and identifies itself before first user unlock.
- **Config Mirroring**: Maintains a secondary "Last Known Good" config file to survive DataStore corruption.
- **Sticky Foreground Service**: `SensorService` operates as `START_STICKY` with `BootReceiver` support for automatic restart on LMK, system boot or an app update (ISSUE-NOV12.6-O92).
