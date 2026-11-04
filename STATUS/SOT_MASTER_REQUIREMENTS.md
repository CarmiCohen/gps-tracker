# SOT Master Requirements & Hardening Status (Nov4.27)

## 🏗️ Architectural Master Rules (413 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.334 preserved)
*   **1.334 Post-Revolution HUD Integration & LED Status Verification (R-ID 105)**: The system MUST provide standardized HUD status bars (`HudStatusBar`) and reactive state flows (`HudState`) across both Monitor and Sensor applications to monitor SRV relay connection status, local GPS fixes, peer connection links (TRK/VWR), internet connectivity, data integrity (DAT), and service watchdog (WDG) in alignment with Requirement R960 (GAP-REVOL52.0-G1).
*   **1.335 HUD Status Bar Compact Layout Hardening & UI Refinement (R-ID 106)**: The system MUST enforce compact monospaced 2-row layouts with optimized 8.sp/9.sp status badges in `HudStatusBar` (`:core-ui`) to prevent text wrapping, collision, or layout truncation on mobile physical screens (GAP-REVOL53.0-G1).
*   **1.336 Nov4.25 Stabilization & Reciprocal Simulation (R-ID 107)**: The system MUST maintain 100% unit test coverage across all subprojects, robust version name consistency (`Nov4.25`), and verified reciprocal virtual presence simulation testing under single-device workstation configurations (GAP-REVOL54.0-G1).
*   **1.337 Relay URL Setup Option Integration in Sensor App (R-ID 108)**: The system MUST provide user-accessible Relay Server URL setup controls in `SettingsOverlay` and `Sensor Setup & Info` dialog (`:app-sensor`), and maintain reactive DataStore observation in `SensorViewModel` and `SensorService` to re-establish `SignalingClient` connections upon Relay URL changes (GAP-REVOL56.0-G1).

## 🛡️ Core Hardening Baseline
- **Nov4.27**: Relay URL Setup Option Integration in Sensor App (GAP-REVOL56.0-G1) - PASSED.
- **Nov4.26**: Monitor UI Refinement & Map Controls (GAP-REVOL55.0-G1) - PASSED.
- **Nov4.25**: Nov4.25 Stabilization & Reciprocal Simulation (GAP-REVOL54.0-G1) - PASSED.
- **Nov4.24**: HUD Status Bar Compact Layout Hardening & UI Refinement (GAP-REVOL53.0-G1) - PASSED.
- **Nov4.22-addHUD**: Post-Revolution HUD Integration & LED Status Verification (GAP-REVOL52.0-G1) - PASSED.
- **Nov4.21**: Default OnRender Cloud Relay URL Configuration (GAP-REVOL51.0-G1) - PASSED.
- **Nov4.20-withServer**: Cloud Relay Server Revival & Relay URL Configuration (GAP-REVOL50.0-G1) - PASSED.
- **Nov4.3**: Advanced Relay Partition Tolerance & Telemetry Edge Synchronization (GAP-REVOL49.0-G1) - PASSED.

## ✅ Verification Chapters
*   **Chapter 5.48 (Nov4.27 Validation Audit)**: PASSED - Verification of Relay URL setup controls in `AdminActivity.kt`, reactive URL collection and automatic signaling re-connection in `SensorViewModel` and `SensorService`, and cross-app Relay URL configuration.
*   **Chapter 5.47 (Nov4.26 Validation Audit)**: PASSED - Verification of Monitor UI upper menu line, setup button, SRV/GPS LED fixes, map settings wheel (`MapSettingsToggle`), map control buttons, and map centralization.
*   **Chapter 5.46 (Nov4.25 Validation Audit)**: PASSED - Verification of Nov4.25 stabilization, unit test suite execution (`testDebugUnitTest` 100% passing across all subprojects), version naming updates, and reciprocal virtual presence simulation testing.
*   **Chapter 5.45 (Nov4.24 Validation Audit)**: PASSED - Verification of HUD status bar compact layout hardening, 8.sp/9.sp monospaced status badges, and 2-row fixed structure preventing overflow and misalignment on mobile physical devices (S21FE).
*   **Chapter 5.44 (Nov4.22-addHUD Validation Audit)**: PASSED - Verification of `HudState` and `HudStatusBar` integration across Monitor and Sensor applications, checking SRV relay connection status LEDs, local GPS fix LEDs, peer connection link LEDs (TRK/VWR), internet status, data integrity (DAT), and service watchdog (WDG).
*   **Chapter 5.43 (Nov4.21 Validation Audit)**: PASSED - Verification of default fallback Relay URL set to `https://gps-relay-server.onrender.com` in `MonitorSessionService` for out-of-the-box cloud connectivity.
