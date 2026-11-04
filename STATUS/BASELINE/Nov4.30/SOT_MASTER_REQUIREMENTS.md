# SOT Master Requirements & Hardening Status (Nov4.30)

## 🏗️ Architectural Master Rules (416 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.334 preserved)
*   **1.334 Post-Revolution HUD Integration & LED Status Verification (R-ID 105)**: The system MUST provide standardized HUD status bars (`HudStatusBar`) and reactive state flows (`HudState`) across both Monitor and Sensor applications to monitor SRV relay connection status, local GPS fixes, peer connection links (TRK/VWR), internet connectivity, data integrity (DAT), and service watchdog (WDG) in alignment with Requirement R960 (GAP-REVOL52.0-G1).
*   **1.335 HUD Status Bar Compact Layout Hardening & UI Refinement (R-ID 106)**: The system MUST enforce compact monospaced 2-row layouts with optimized 8.sp/9.sp status badges in `HudStatusBar` (`:core-ui`) to prevent text wrapping, collision, or layout truncation on mobile physical screens (GAP-REVOL53.0-G1).
*   **1.336 Nov4.25 Stabilization & Reciprocal Simulation (R-ID 107)**: The system MUST maintain 100% unit test coverage across all subprojects, robust version name consistency (`Nov4.25`), and verified reciprocal virtual presence simulation testing under single-device workstation configurations (GAP-REVOL54.0-G1).
*   **1.337 Relay URL Setup Option Integration in Sensor App (R-ID 108)**: The system MUST provide user-accessible Relay Server URL setup controls in `SettingsOverlay` and `Sensor Setup & Info` dialog (`:app-sensor`), and maintain reactive DataStore observation in `SensorViewModel` and `SensorService` to re-establish `SignalingClient` connections upon Relay URL changes (GAP-REVOL56.0-G1).
*   **1.338 Pre-Revolution Map Functionality & Specifications Implementation (R-ID 109)**: The system MUST implement robust pre-revolution osmdroid map rendering primitives, marker/polyline pooling (`MapOverlayManager`), imperative camera & follow logic (`MapController`), and the complete Map Tools Overlay (`MapToolsOverlay`) supporting dual trails, Home Points (ADD/DEL/FENCE), trail operations (CLEAR/SAVE/LOAD), and spatial violation markers (GAP-REVOL57.0-G1).
*   **1.339 Nov4.29 Stabilization & Test Suite Execution (R-ID 110)**: The system MUST maintain 100% unit test execution across all project modules, version name consistency (`Nov4.29`), and verified stabilization across all hardening chapters and test procedures under single-device workstation configurations (GAP-REVOL58.0-G1).
*   **1.340 Monitor Dual HUD Status Lines, GPS Colors & Connection-Aware Centering (R-ID 111)**: The system MUST display dual HUD status lines on the Monitor app (Line 1: Local Device with Cyan GPS LED `#06B6D4`; Line 2: Sensor / Tracker with Green GPS LED `#10B981`), dual centering map buttons (`SENSOR` and `LOCAL`), and connection-aware auto-centering logic (auto-centering around local device when disconnected, and around known device / sensor when connected) (GAP-REVOL59.0-G1).

## 🛡️ Core Hardening Baseline
- **Nov4.30**: Monitor Dual HUD Status Lines, GPS Colors & Connection-Aware Centering (GAP-REVOL59.0-G1) - PASSED.
- **Nov4.29**: Nov4.29 Stabilization & Test Suite Execution (GAP-REVOL58.0-G1) - PASSED.
- **Nov4.28**: Pre-Revolution Map Functionality & Specifications Implementation (GAP-REVOL57.0-G1) - PASSED.
- **Nov4.27**: Relay URL Setup Option Integration in Sensor App (GAP-REVOL56.0-G1) - PASSED.
- **Nov4.26**: Monitor UI Refinement & Map Controls (GAP-REVOL55.0-G1) - PASSED.
- **Nov4.25**: Nov4.25 Stabilization & Reciprocal Simulation (GAP-REVOL54.0-G1) - PASSED.
- **Nov4.24**: HUD Status Bar Compact Layout Hardening & UI Refinement (GAP-REVOL53.0-G1) - PASSED.
- **Nov4.22-addHUD**: Post-Revolution HUD Integration & LED Status Verification (GAP-REVOL52.0-G1) - PASSED.
- **Nov4.21**: Default OnRender Cloud Relay URL Configuration (GAP-REVOL51.0-G1) - PASSED.
- **Nov4.20-withServer**: Cloud Relay Server Revival & Relay URL Configuration (GAP-REVOL50.0-G1) - PASSED.

## ✅ Verification Chapters
*   **Chapter 5.51 (Nov4.30 Validation Audit)**: PASSED - Verification of Monitor dual HUD status lines, GPS colors (Cyan local, Green sensor), dual centering buttons (`SENSOR` and `LOCAL`), and connection-aware auto-centering logic with test procedure chapters 4.4-4.6.
*   **Chapter 5.50 (Nov4.29 Validation Audit)**: PASSED - Verification of Nov4.29 stabilization, full test suite execution across all subprojects (100% unit tests passing), version name consistency (`Nov4.29`), and reciprocal virtual presence simulation testing.
*   **Chapter 5.49 (Nov4.28 Validation Audit)**: PASSED - Verification of pre-revolution map functionality implementation in `:app-monitor`, osmdroid overlay pooling (`MapOverlayManager`), imperative camera control (`MapController`), Map Tools Overlay (`MapToolsOverlay`), Home Point boundary circles, and spatial violation rendering, with test procedure chapters 107 & 108.
*   **Chapter 5.48 (Nov4.27 Validation Audit)**: PASSED - Verification of Relay URL setup controls in `AdminActivity.kt`, reactive URL collection and automatic signaling re-connection in `SensorViewModel` and `SensorService`, and cross-app Relay URL configuration.
*   **Chapter 5.47 (Nov4.26 Validation Audit)**: PASSED - Verification of Monitor UI upper menu line, setup button, SRV/GPS LED fixes, map settings wheel (`MapSettingsToggle`), map control buttons, and map centralization.
*   **Chapter 5.46 (Nov4.25 Validation Audit)**: PASSED - Verification of Nov4.25 stabilization, unit test suite execution (`testDebugUnitTest` 100% passing across all subprojects), version naming updates, and reciprocal virtual presence simulation testing.
*   **Chapter 5.45 (Nov4.24 Validation Audit)**: PASSED - Verification of HUD status bar compact layout hardening, 8.sp/9.sp monospaced status badges, and 2-row fixed structure preventing overflow and misalignment on mobile physical devices (S21FE).
