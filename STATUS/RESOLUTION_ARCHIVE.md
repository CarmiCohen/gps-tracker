# Resolution Archive (Sep.05.16)

## 🟢 Resolved Issues (Sep.05.16)
*   **Issue #918 RESOLVED: VWR Badge Persistence Leak**. Remediated the "sticky green" badge issue where the `VWR` (Viewer) indicator remained active after the peer app was closed. Restricted `lastRemoteActivityTs` updates to high-assurance telemetry (Location/Health) packets and pruned generic signaling heartbeat resets (pulses/pongs). Enabled binary Protobuf telemetry processing in Tracker mode to ensure accurate two-way presence monitoring (R-ID 258).

## 🟢 Resolved Issues (Sep.05.15)
*   **Issue #917 RESOLVED: Exact Actual Colors (HUD LED Synchronization)**. Remediated hardcoded "always-true" telemetry freshness logic on the Tracker side. Synchronized all UI staleness gates to **35s** (`TELEMETRY_UI_STALE_THRESHOLD_MS`) to match the definitive HUD Specification (R338/R972). Standardized `DAT` and `WATCHDOG` LEDs to reflect real-time service heartbeats and peer presence (R-ID 257).

## 🟢 Resolved Issues (Sep.05.12)
*   **Issue #915 RESOLVED: Mapnik Tile Latency (Samsung A15)**. Remediated map interaction lag on budget hardware by implementing a specialized resource profile (R915). Throttled tile download concurrency to 2 threads to reduce Helio G99 context switching and expanded disk cache to 600MB/Memory cache to 64 tiles. Verified elimination of I/O-induced UI stalls during high-frequency map panning (R915).

## 🟢 Resolved Issues (Sep.05.10)
*   **Issue #910 RESOLVED: Forensic Stall Simulation (Service Termination Race)**. Remediated a race condition during UI hydration where a transient `null` `appMode` triggered a navigation retreat to the `Landing` route. This activated the `BackHandler` which incorrectly invoked `onCleanupAndExit()`. Hardened `MainAppContent.kt` with a guard preventing Landing navigation if `isSystemActive` is true. Added forensic stack trace logging in `MainActivity.kt` and state transition auditing in `MainViewModel.kt` (R910/R255).
