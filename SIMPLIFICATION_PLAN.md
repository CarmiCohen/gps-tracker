# Project Roadmap: Simplification & Hardening (July.22.05)

This document tracks the evolution of the `gps-tracker` architecture. 

## 🏗 Hardening Phase: Hilt & Forensic Authority [CURRENT]
Following the July.16 simplification attempts, the system was found to require more robust dependency management and higher forensic fidelity for high-assurance tracking.

*   **R120b: Hilt Universal Authority - Issue #124 [COMPLETED]**: Re-implemented and standardized Hilt across the entire project. Manual `AppContainer` is decommissioned to ensure compile-time safety and better service lifecycle management. (Fixed July.22.04)
*   **R118: Forensic Matrix Restoration - Issue #118 [COMPLETED]**: Restored and expanded the forensic SIT indicators (Vibe, Lux, Baro, etc.) that were previously simplified. (Fixed July.22.01)
*   **R102: Temporal Forensic Integrity - Issue #102 [COMPLETED]**: Hardened the engine to use monotonic `rt` authority for all logic. (Fixed July.21.00)

## 🛠 Legacy Simplification Phase (R406) [SUPERSEDED]
*   **R406a: Unified Heartbeat (2s Standard)**: Standardized tasks to a 2s cycle. (Fixed July.11.01)
*   **R406c: Hilt Removal (Attempted)**: *Note: This was superseded by R120b in July.22.x due to scaling and stability requirements.*
*   **R406h: Low-Value Code Removal**: Successfully purged GtoEngine trajectory optimization and legacy IMU chair-sit detection.

## Next Steps
1.  **Soak Test (#031)**: 24-hour stability audit of the Hilt-managed service layer.
2.  **Samsung A15 Field Test (#113)**: Final validation of the Accelerometer fallback pulse.
