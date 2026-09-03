# Resolution Archive (Sep.03.120)

## 🟢 Resolved Issues (Sep.03.120)
*   **Issue #899: Basic Field Test Preparation (S21FE -> A15)**. Prepared the environment for a coordinated field test between S21FE (Viewer) and A15 (Tracker). 
    1. Verified `HardwareProvider` uses `PRIORITY_HIGH_ACCURACY` and real GNSS callbacks with no active mocks.
    2. Aligned `SignalingConstants` to use default IDs ("T" and "V") for immediate pairing.
    3. Updated `versionName` to `Sep.03.120` for tracking (R899).

## 🟢 Resolved Issues (Sep.04.01)
*   **Issue #898: A15 Connectivity & GPS Hardening**. Samsung A15 devices showed intermittent "SRV" (socket) and "GPS" (staleness) red status in the UI. Root cause: Aggressive OS-level background suppression causing radio dormancy and sensor polling gaps. Resolved by:
    1. Reducing `A15_POKE_INTERVAL_MS` to 30s in `TrackerService` to keep radio active.
    2. Tightening heuristic connection recovery in `TrackerService` to 10s for A15 hardware.
    3. Forcing `SUSPICIOUS_GPS_POLLING_MS` (10s) as the baseline in `ServiceBehaviorUseCase` when the screen is off on A15 to prevent the 90s UI staleness timeout (R898).

## 🟢 Resolved Issues (Sep.03.110)
*   **Issue #897: Target SDK 35 FGS Compatibility**. Fixed `InvalidForegroundServiceTypeException` in `MaintenanceWorker` and `BootServiceStartWorker` by explicitly declaring and passing `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`. Standardized `getForegroundInfo()` across all work artifacts to ensure compliance with Android 15's stricter service type enforcement (R897).
