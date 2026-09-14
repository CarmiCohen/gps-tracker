# Project Issues & Hardening Tracking (Sep.14.10)

## 🎯 Current Resumption Focus: Forensic Integrity & A15 Compliance
Monitoring signaling pipeline stability and sensor-to-relay latency on budget Samsung hardware.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*No open issues for this release cycle.*

## 🟢 Recently Resolved Issues (Sep.14.10)
*   **IPC Shadow Coverage (#1019)**:
    *   **Root-Cause Remediation**: Migrated all remaining data-path and configuration repositories (`SettingsRepository`, `AppAlarmManager`, `ConfigManager`, `ForensicSpillBuffer`, `HistoryManager`) to `@ShadowContext`. This ensures 100% `ShadowCache` coverage for package name lookups, mitigating high-frequency IPC overhead during system service interactions (R-ID 324).
*   **Identity Sync Verification (#1021)**:
    *   **Root-Cause Remediation**: Verified the 60s forced sync loop in `ConnectivitySuite.kt`. Confirmed it successfully manages relay room occupancy during stationary periods by handling server-side socket timeouts through proactive identity re-joining.
*   **Forensic Drop Analysis (#1020)**:
    *   **Root-Cause Remediation**: Refactored signaling pipeline to ensure authoritative forensic visibility. Simplified `CommunicationManager.kt` to a transport-only role and centralized validation in `ConnectivitySuite.kt`. Rejection warnings now reliably log descriptive reasons (e.g., "Echo suppression", "Unauthorized Viewer") for all signaling packets (R-ID 323).
*   **Cleanup Logic Simplification (#1022)**:
    *   **Root-Cause Remediation**: Converted hardware unregistration in `ManagedHardware.kt` to fire-and-forget asynchronous mode. Removed `CountDownLatch` and `Tasks.await` blocks to prevent synchronous stalls that delayed signaling teardown during rapid mode transitions (Idea #17, R-ID 322).
*   **Build Restoration (#1024)**:
    *   **Root-Cause Remediation**: Resolved `Unresolved reference` errors in `TrackerService` and `ViewerService` by re-consolidating `ConnectivityEvent` as a top-level sealed class in `ConnectivitySuite.kt`. (R-ID 321).
*   **Forensic Visibility Enhancement (#1019)**:
    *   **Root-Cause Remediation**: Integrated `SignalingValidator.getDropReason` into `ConnectivitySuite` logs. Rejection warnings now include descriptive reasons (e.g., "Unauthorized Viewer", "Echo suppression") instead of just raw IDs, enabling faster triage of signaling stalls. (R-ID 320).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 324 (Rules: 64, IDs: 324), Resolved: 1030, Open: 0, Testing: 0, Ideas: 17, QA: 277]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.14.10)*
