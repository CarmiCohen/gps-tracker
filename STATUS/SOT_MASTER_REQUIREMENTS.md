# SOT Master Requirements & Hardening Status (Nov4.31)

## 🏗️ Architectural Master Rules (418 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.334 preserved)
*   **1.340 Monitor Dual HUD Status Lines, GPS Colors & Connection-Aware Centering (R-ID 111)**: The system MUST display dual HUD status lines on the Monitor app (Line 1: Local Device with Cyan GPS LED `#06B6D4`; Line 2: Sensor / Tracker with Green GPS LED `#10B981`), dual centering map buttons (`SENSOR` and `LOCAL`), and connection-aware auto-centering logic (R-ID 111).
*   **1.341 Alarming Setup & Alert Management Revival (R-ID 112)**: The system MUST provide fully revived Alarming Setup overlays (`AlertManagementOverlay` and `AlarmSoundOverlay` in `:core-ui`) supporting global mute, bulk toggles, communication/geofence/device/integrity/sentinel motion alarms, vibration/tilt sensitivity sliders, siren wave type selection, override DND/silent mode, force max volume, custom app volume controls, and persistent DataStore serialization across both Sensor and Monitor applications (R-ID 112).
*   **1.342 Device-Specific Setup & Hardware Hardening Revival (R-ID 113)**: The system MUST provide device-specific setup and hardware hardening (`DeviceProfileManager`, `DeviceHardeningStrategy`, `HardwareCapabilities`, and `PerformanceTier` in `:app-sensor`), automatically profiling hardware performance tiers (`STAGGERED` for budget tier hardware such as Samsung A15 vs `STANDARD`), enforcing vendor power management overrides, and applying the 60-second background transition hardening window (R-ID 113).

## 🛡️ Core Hardening Baseline
- **Nov4.31**: Device-Specific Setup & Hardware Hardening Revival (GAP-REVOL61.0-G1) - PASSED.
- **Nov4.30**: Monitor Dual HUD Status Lines, GPS Colors & Connection-Aware Centering & Alarming Setup Revival (GAP-REVOL59.0-G1, GAP-REVOL60.0-G1) - PASSED.
- **Nov4.29**: Nov4.29 Stabilization & Test Suite Execution (GAP-REVOL58.0-G1) - PASSED.
- **Nov4.28**: Pre-Revolution Map Functionality & Specifications Implementation (GAP-REVOL57.0-G1) - PASSED.
- **Nov4.27**: Relay URL Setup Option Integration in Sensor App (GAP-REVOL56.0-G1) - PASSED.
- **Nov4.26**: Monitor UI Refinement & Map Controls (GAP-REVOL55.0-G1) - PASSED.
- **Nov4.25**: Nov4.25 Stabilization & Reciprocal Simulation (GAP-REVOL54.0-G1) - PASSED.
- **Nov4.24**: HUD Status Bar Compact Layout Hardening & UI Refinement (GAP-REVOL53.0-G1) - PASSED.
- **Nov4.22-addHUD**: Post-Revolution HUD Integration & LED Status Verification (GAP-REVOL52.0-G1) - PASSED.
- **Nov4.21**: Default OnRender Cloud Relay URL Configuration (GAP-REVOL51.0-G1) - PASSED.

## ✅ Verification Chapters
*   **Chapter 5.53 (Nov4.31 Device Hardening Audit)**: PASSED - Verification of revived `DeviceProfileManager`, `DeviceHardeningStrategy`, `HardwareCapabilities`, performance tier profiling (Samsung A15 staggered tier vs S21FE standard tier), and 60s background transition hardening window.
*   **Chapter 5.52 (Nov4.30 Alarming Setup Audit)**: PASSED - Verification of revived `AlertManagementOverlay` and `AlarmSoundOverlay`, DataStore persistence, bulk toggles, sensitivity sliders, siren wave selection, and audio testing.
*   **Chapter 5.51 (Nov4.30 Validation Audit)**: PASSED - Verification of Monitor dual HUD status lines, GPS colors (Cyan local, Green sensor), dual centering buttons (`SENSOR` and `LOCAL`), and connection-aware auto-centering logic with test procedure chapters 4.4-4.6.
