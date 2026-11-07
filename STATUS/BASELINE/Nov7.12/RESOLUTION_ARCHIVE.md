# Resolution Archive

## ✅ Resolved Issues (Nov7.12)
- **Coarse Network Fix Filtering (ISSUE-NOV6.1-O28)**: Added `isBetterLocation` filter to `MonitorLocationSource.kt` based on standard Android location evaluation. Prevents a coarse Network location fix (e.g., accuracy ±77.6m) from overriding a fresh, more accurate GPS location fix (e.g., accuracy ±11m) on the Monitor UI and map while the GPS fix is less than 2 minutes (120s) old. Verified by unit tests in `MonitorLocationSourceTest` and full automated test suite pass across all modules. **PASSED.**
