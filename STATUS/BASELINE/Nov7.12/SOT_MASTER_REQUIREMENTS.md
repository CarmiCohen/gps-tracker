# SOT Master Requirements & Hardening Status (Nov7.12)

## 🏗️ Architectural Master Rules (442 Rules)
...
*   **1.372 Coarse Network Fix Filtering (R-ID 143)**: `MonitorLocationSource` MUST filter incoming location updates using `isBetterLocation` to prevent a coarse Network location fix (e.g., accuracy ±77.6m) from replacing a fresh, more accurate GPS location fix (e.g., accuracy ±11m) on the Monitor UI and map. A coarse Network fix MUST only replace a GPS fix if the GPS fix is older than 2 minutes (120s) (R-ID 143).

## 🛡️ Core Hardening Baseline
- **Nov7.12**: Coarse Network Fix Filtering (ISSUE-NOV6.1-O28) - PASSED.
- **Nov7.11**: Thread-Safe SignalingQueue & Failed Flush Re-queuing (ISSUE-NOV6.0-O24) - PASSED.
