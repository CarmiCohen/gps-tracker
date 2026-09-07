# Resolution Archive (Sep.06.58)

## 🟢 Resolved Issues (Sep.06.58)
*   **Issue #935 RESOLVED: GPS Red-Lock Regression**. Remediated critical HUD signaling latency where the GPS badge remained RED despite active GNSS callbacks. The regression occurred during the monotonic clock migration (vSep.05.20) where `TrackerService` and `ViewerService` failed to populate the new `rt` (elapsedRealtime) field in local `LocationUpdate` emissions. Corrected the telemetry pipeline to ensure all role-local updates include monotonic authority for UI staleness parity (R-ID 276).

## 🟢 Verified Deployment & Soak (Sep.06.57)
*   **Deployment Session: Soak Test Initiation**. Verified A15 signaling continuity and forensic loop instrumentation. Confirmed Energy Footprint (R-ID 259) and Sensor Rate (R-ID 256) audits are functional on physical hardware. Identified Issue #935 (GPS lock latency) during log monitoring.

## 🟢 Resolved Issues (Sep.06.56)
*   **Issue #934 RESOLVED: Documentation Integrity Restoration**. Restored accidentally truncated forensic requirements (R251-R267) in `STATUS/QA_VALIDATION_STATUS.md` to maintain the high-assurance audit record.

## 🟢 Resolved Issues (Sep.06.55)
*   **Issue #933 RESOLVED: Viewer Forensic Parity (Audit & Revival)**. Implemented the `Stability Audit` loop and `Revival Event` observation in `ViewerService`. The Viewer role now captures high-resolution reliability percentages, GNSS jitter violations, and energy footprint verdicts (Revival delta mA/Temp), matching the forensic baseline of the Tracker role (R-ID 276).

## 🟢 Resolved Issues (Sep.06.50)
*   **Issue #932 RESOLVED: HUD Synchronization**. Synchronized the HUD status bar to reflect Samsung A15 hardware adaptations. Propagated `isA15Device` from the hardware layer through `MainViewModel` and `UiStateAggregator`. Added a new `A15` badge to the `StatusBar` in `SharedUiComponents.kt` to provide forensic confirmation that background "Poke" logic and `specialUse` FGS adaptations are active (R-ID 276).

## 🟢 Resolved Issues (Sep.06.45)
*   **Issue #931 RESOLVED: GPS Reception Parity (Viewer Mode)**. Remediated discrepancy where the Viewer role suffered from background GPS suppression on Samsung A15 hardware compared to Waze/Maps. Promoted `ViewerService` to include `specialUse` FGS type, implemented A15 "Poke" logic (30s hardware handshake/WakeLock), and added adaptive polling (2000ms when UI is active) to keep the GPS pipeline fresh (R-ID 276).

*(For older resolutions, see history logs.)*
