# Project Issues & Hardening Tracking (Sep.11.56)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **A15 GNSS Instability (#950)**: STABILITY AUDIT: Reliability 0.0%. GNSS Jitter: 7944ms. Stability Gap: 13851ms detected during logic pulse.

## 🟢 Recently Resolved Issues (Sep.11.56)
*   **A15 Reactive Flow Stalls (#949)**:
    *   **Root-Cause Remediation**: Directed all shared observation flows in `SystemStatusProviderImpl` to execute on `Dispatchers.IO` using `.flowOn(Dispatchers.IO)`. This eliminates main-thread contention and ensures consistent vitality pulses for IntegrityMonitor heartbeats, preventing false-positive stall detections on budget hardware (R-ID 289).

## 🟢 Recently Resolved Issues (Sep.11.54)
*   **Build Failure: Type Mismatch in MainViewModel (#947)**:
    *   **Root-Cause Remediation**: Lambda parameter mismatch resolved by nesting `combine` calls for `dashboardHealthState`. Standard `combine` is limited to 5 heterogeneous flows; nesting 3 flows inside a sub-combine brought the main arity to 4, satisfying the compiler (R-ID 289).
*   **Build Failure: HUD State Type Mismatch (#948)**:
    *   **Root-Cause Remediation**: Removed duplicate `HudConnectivityState`, `HudTelemetryState`, and `HudHealthState` from `:core:engine`. These UI-facing models are now exclusively managed in the `:app` module to prevent import ambiguity and ensure consistency (R-ID 286).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 260 (Rules: 59, IDs: 260), Resolved: 1003, Open: 1, Testing: 100% (Sub-items: 51), Ideas: 12, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.56)*
