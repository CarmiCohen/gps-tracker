# Project Roadmap: Simplification & Hardening (July.22.08)

This document tracks the evolution of the `gps-tracker` architecture. 

## 🏗 Hardening Phase: Hilt & Forensic Authority [COMPLETE]
The transition to a high-assurance Hilt architecture and forensic-grade engine is now complete.

*   **R120b: Hilt Universal Authority - Issue #124/126 [COMPLETED]**: Re-implemented and standardized Hilt across the entire project. Manual `AppContainer` and `MainViewModelFactory` have been decommissioned and references scrubbed to ensure architectural purity. (Fixed July.22.08)
*   **R118: Forensic Matrix Restoration - Issue #118 [COMPLETED]**: Restored and expanded the forensic SIT indicators (Vibe, Lux, Baro, etc.). (Fixed July.22.01)
*   **R102: Temporal Forensic Integrity - Issue #102 [COMPLETED]**: Hardened the engine to use monotonic `rt` authority. (Fixed July.21.00)
*   **R104: Startup Maintenance Authority - Issue #104 [COMPLETED]**: Global proactive log pruning integrated into both UI and Background Service lifecycles to prevent I/O bottlenecks. (Fixed July.22.08)
*   **R405c: Samsung Stay-Alive Hardening - Issue #113 [COMPLETED]**: Implemented hardware stay-alive pulse (WakeLock poke) in `AppSensorManager` to prevent OS eviction on budget Samsung hardware. (Fixed July.22.08)

## 🛠 Legacy Simplification Phase (R406) [SUPERSEDED]
*   **R406a: Unified Heartbeat (2s Standard)**: Standardized tasks to a 2s cycle. (Fixed July.11.01)
*   **R406h: Low-Value Code Removal**: Successfully purged GtoEngine trajectory optimization and legacy IMU chair-sit detection.

## Next Steps
1.  **Soak Test (#031)**: 24-hour stability audit of the Hilt-managed service layer and proactive pruning logs.
2.  **Field Validation (#113)**: Monitor Samsung A15 (SM-A155F) process longevity with the new hardware pulse.
