# Resolution Archive (Sep.03.121)

## 🟢 Resolved Issues (Sep.03.121)
*   **Issue #899 RESOLVED: Multi-Device Field Test (S21FE -> A15)**. Deployed the app (vSep.03.120) to both SM-G990E and SM-A155F. End-to-end verification confirmed Viewer readiness ("WAITING FOR TELEMETRY"). Identified critical background service start regressions and lifecycle loops on the A15 hardware. Readiness prep complete; transitioning to hardening phase for budget hardware (R899/R250).

## 🟢 Recently Resolved Issues (Sep.04.01)
*   **Issue #898: A15 Connectivity & GPS Hardening**. Budget hardware (A15) showed intermittent signaling loss and GPS staleness due to aggressive OS background suppression. Implemented a multi-tier hardening strategy: Reduced A15 radio poke interval to 30s, tightened heuristic connection recovery to 10s, and forced a 10s GPS polling baseline when the screen is off (R898).

## 🟢 Recently Resolved Issues (Sep.03.110)
*   **Issue #897: Target SDK 35 FGS Compatibility**. Fixed `InvalidForegroundServiceTypeException` in `MaintenanceWorker` and `BootServiceStartWorker` by explicitly declaring and passing `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (R897).
