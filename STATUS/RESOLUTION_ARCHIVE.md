# Resolution Archive (Sep.05.12)

## 🟢 Resolved Issues (Sep.05.12)
*   **Issue #915 RESOLVED: Mapnik Tile Latency (Samsung A15)**. Remediated map interaction lag on budget hardware by implementing a specialized resource profile (R915). Throttled tile download concurrency to 2 threads to reduce Helio G99 context switching and expanded disk cache to 600MB/Memory cache to 64 tiles. Verified elimination of I/O-induced UI stalls during high-frequency map panning (R915).

## 🟢 Resolved Issues (Sep.05.10)
*   **Issue #910 RESOLVED: Forensic Stall Simulation (Service Termination Race)**. Remediated a race condition during UI hydration where a transient `null` `appMode` triggered a navigation retreat to the `Landing` route. This activated the `BackHandler` which incorrectly invoked `onCleanupAndExit()`. Hardened `MainAppContent.kt` with a guard preventing Landing navigation if `isSystemActive` is true. Added forensic stack trace logging in `MainActivity.kt` and state transition auditing in `MainViewModel.kt` (R910/R255).

## 🟢 Resolved Issues (Sep.04.40)
*   **Issue #911 RESOLVED: Audit Baseline Synchronization**. Remediated discrepancies in SOT ID counts and Open issue tracking between `issues.md`, `Handover.md`, and `SOT_MASTER_REQUIREMENTS.md`. Established a locked baseline of [SOT: 260, Rules: 41, IDs: 219].
*   **Issue #908 RESOLVED: A15 Lifecycle & Deployment Hardening**. Remediated the "Teardown-Loop Anomaly" on budget hardware by implementing asynchronous, restart-aware thread termination in `HardwareProvider`. Established **R-ID 254** for periodic (60s) signaling identity re-broadcast in `ConnectivitySuite`, ensuring zero-interaction peer discovery during rolling deployments (R908/R254).
