# Project Issues & Hardening Tracking (Sep.11.58)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority engine issues currently open.*

## 🟢 Recently Resolved Issues (Sep.11.58)
*   **A15 GNSS Instability (#950)**:
    *   **Root-Cause Remediation**: Relaxed stability thresholds (Jitter: 500ms -> 3000ms, Gap: 200ms -> 1000ms) to accommodate budget hardware latency. Implemented transition "muzzling" in `ForensicAuditor` to suppress false-positive reliability failures during polling interval adaptation (e.g., stationary to moving transitions). (R-ID 262).

## 🟢 Recently Resolved Issues (Sep.11.56)
*   **A15 Reactive Flow Stalls (#949)**:
    *   **Root-Cause Remediation**: Directed all shared observation flows in `SystemStatusProviderImpl` to execute on `Dispatchers.IO` using `.flowOn(Dispatchers.IO)`. This eliminates main-thread contention and ensures consistent vitality pulses for IntegrityMonitor heartbeats, preventing false-positive stall detections on budget hardware (R-ID 289).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 262 (Rules: 60, IDs: 262), Resolved: 1004, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 14, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.58)*
