# Project Revolution: Sensor (Tracker) Requirements

## 1. Functional Overview
The Sensor is a high-precision, background-persistent engine and acts as the authority for environment monitoring.

## 2. 100% Kotlin Sensing Engine
- **Spoofing Detection**: Cross-verifies GPS velocity with IMU integrals. Triggers alert if GPS is static while IMU detects motion.
- **Orientation Independence**: Uses gravity-vector rotation for all IMU math.
- **Thermal Governance**: Scales from 10Hz to 1Hz if battery temp > 45°C.

## 3. Connectivity Reliability
- **APN Auto-Heal**: If Relay is lost for > 4h, cycles through common cellular APN configurations to restore internet.
- **Meteered Awareness**: Drops to 2Hz if on a metered cellular plan to save data costs.

## 4. Security & HUD
- **Passcode Protection**: Admin HUD requires a 4-digit PIN for access to calibration and config.
- **Setup QR**: Displays a secure pairing code containing TrackerID + SecretKey.
- **Notification Stealth**: Titles are non-descriptive (e.g., "System Maintenance") to avoid thief detection.

## 5. Persistence & Direct Boot
- **Direct Boot Aware**: Service starts and identifies itself before first user unlock.
- **Config Mirroring**: Maintains a secondary "Last Known Good" config file to survive DataStore corruption.
