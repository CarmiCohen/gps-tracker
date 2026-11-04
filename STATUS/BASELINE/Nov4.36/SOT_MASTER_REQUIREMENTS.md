# SOT Master Requirements & Hardening Status (Nov4.36)

## 🏗️ Architectural Master Rules (423 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.334 preserved)
*   **1.341 Alarming Setup & Alert Management Revival (R-ID 112)**: The system MUST provide fully revived Alarming Setup overlays (`AlertManagementOverlay` and `AlarmSoundOverlay` in `:core-ui`) supporting global mute, bulk toggles, communication/geofence/device/integrity/sentinel motion alarms, vibration/tilt sensitivity sliders, siren wave type selection, override DND/silent mode, force max volume, custom app volume controls, and persistent DataStore serialization across both Sensor and Monitor applications (R-ID 112).
*   **1.342 Device-Specific Setup & Hardware Hardening Revival (R-ID 113)**: The system MUST provide device-specific setup and hardware hardening (`DeviceProfileManager`, `DeviceHardeningStrategy`, `HardwareCapabilities`, and `PerformanceTier` in `:app-sensor`), automatically profiling hardware performance tiers (`STAGGERED` for budget tier hardware such as Samsung A15 vs `STANDARD`), enforcing vendor power management overrides, and applying the 60-second background transition hardening window (R-ID 113).
*   **1.343 System Tools & Diagnostics Menu Integration (R-ID 114)**: The system MUST provide a dedicated **Tools** menu option (refresh/tools icon button) positioned in the top menu bar alongside Setup and Settings gear icons in both Sensor and Monitor applications, opening `ToolsOverlay` (`:core-ui`) for 100Hz burst simulation, database pruning, test violation triggering, and watchdog resetting (R-ID 114).
*   **1.344 Revived Sound & Setup Options Integration (R-ID 115)**: The system MUST provide fully integrated `CleanSetupOverlay` and `PhoneSetupOverlay` components in `:core-ui`, wired into configuration menus in both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications to support system maintenance (clearing home points, resetting stats, triggering full initialization) and comprehensive device capabilities & permissions guides (R-ID 115).
*   **1.345 Upper Menu Line Expansion (R-ID 116)**: The system MUST provide an expanded upper menu line (top control bar) across both Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications incorporating direct one-tap navigation for Setup (`Build`), Settings (`Settings`), Tools (`Refresh`), Map (`Place`), Events (`Menu`), Graphs (`Star`), and Info (`Info`) (R-ID 116).
*   **1.346 Monitor Hardware Deployment & Single-Line Fitting Audit (R-ID 117)**: The system MUST maintain single-line text layout constraints (`maxLines = 1`, `TextOverflow.Ellipsis`, compact button sizing of 28dp) across the upper menu bar (`AdminActivity` and `MonitorActivity`) and HUD status bars (`HudStatusBar`), ensuring zero line-wrapping distortion, full visibility of telemetry age metadata (`AGE:$ageStr`), and stable hardware deployment performance on S21FE and staggered tier devices (R-ID 117).
*   **1.347 Dual Device Hardware Deployment & Telemetry Audit (R-ID 118)**: The system MUST support simultaneous deployment and execution of `:app-monitor` on flagship tier hardware (S21FE) and `:app-sensor` on budget/staggered tier hardware (A155), maintaining background telemetry sampling intervals (10,000ms), zero-crash service lifecycle execution, and clean logcat event logging across both endpoints (R-ID 118).

## 🛡️ Core Hardening Baseline
- **Nov4.36**: Dual Device Hardware Deployment & Telemetry Audit (GAP-REVOL66.0-G1) - PASSED.
- **Nov4.35**: Monitor Hardware Deployment & Single-Line Fitting Audit (GAP-REVOL65.0-G1) - PASSED.
- **Nov4.34**: Upper Menu Line Expansion (GAP-REVOL64.0-G1) - PASSED.
- **Nov4.33**: Revived Sound & Setup Options Integration (GAP-REVOL63.0-G1) - PASSED.
- **Nov4.32**: System Tools & Diagnostics Menu Integration (GAP-REVOL62.0-G1) - PASSED.
- **Nov4.31**: Device-Specific Setup & Hardware Hardening Revival (GAP-REVOL61.0-G1) - PASSED.
- **Nov4.30**: Monitor Dual HUD Status Lines, GPS Colors & Connection-Aware Centering & Alarming Setup Revival (GAP-REVOL59.0-G1, GAP-REVOL60.0-G1) - PASSED.
- **Nov4.29**: Nov4.29 Stabilization & Test Suite Execution (GAP-REVOL58.0-G1) - PASSED.
- **Nov4.28**: Pre-Revolution Map Functionality & Specifications Implementation (GAP-REVOL57.0-G1) - PASSED.
- **Nov4.27**: Relay URL Setup Option Integration in Sensor App (GAP-REVOL56.0-G1) - PASSED.
- **Nov4.26**: Monitor UI Refinement & Map Controls (GAP-REVOL55.0-G1) - PASSED.
- **Nov4.25**: Nov4.25 Stabilization & Reciprocal Simulation (GAP-REVOL54.0-G1) - PASSED.
- **Nov4.24**: HUD Status Bar Compact Layout Hardening & UI Refinement (GAP-REVOL53.0-G1) - PASSED.
- **Nov4.22-addHUD**: Post-Revolution HUD Integration & LED Status Verification (GAP-REVOL52.0-G1) - PASSED.

## ✅ Verification Chapters
*   **Chapter 5.58 (Nov4.36 Dual Device Hardware Deployment Audit)**: PASSED - Verification of simultaneous deployment of Monitor on S21FE and Sensor on A155, active 10,000ms telemetry generation, logcat event logging, and 100% test execution across all subprojects.
*   **Chapter 5.57 (Nov4.35 Monitor Hardware Deployment & Single-Line Fitting Audit)**: PASSED - Verification of hardware deployment on S21FE / R58X40GV2AR, upper header bar single-line fitting, HUD telemetry age metadata visibility, and 100% test execution across all subprojects.
*   **Chapter 5.56 (Nov4.34 Upper Menu Line Audit)**: PASSED - Verification of expanded upper menu line quick navigation buttons (Map, Events, Graphs, Info, Setup, Settings, Tools) in Sensor and Monitor applications, and comprehensive unit test execution.
*   **Chapter 5.55 (Nov4.33 Sound & Setup Options Audit)**: PASSED - Verification of revived `CleanSetupOverlay` and `PhoneSetupOverlay` in `:core-ui`, wiring in `AdminActivity` and `MonitorActivity`, and comprehensive test execution.
*   **Chapter 5.54 (Nov4.32 System Tools Audit)**: PASSED - Verification of dedicated Tools menu option under top menu bar, `ToolsOverlay` component, 100% test execution, and action dispatch.
*   **Chapter 5.53 (Nov4.31 Device Hardening Audit)**: PASSED - Verification of revived `DeviceProfileManager`, `DeviceHardeningStrategy`, `HardwareCapabilities`, performance tier profiling (Samsung A15 staggered tier vs S21FE standard tier), and 60s background transition hardening window.
*   **Chapter 5.52 (Nov4.30 Alarming Setup Audit)**: PASSED - Verification of revived `AlertManagementOverlay` and `AlarmSoundOverlay`, DataStore persistence, bulk toggles, sensitivity sliders, siren wave selection, and audio testing.