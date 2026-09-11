# Hardening Resolution Archive (Sep.11.58)

## 🟢 Resolved in Sep.11.58
*   **A15 GNSS Instability (#950)**:
    *   **Remediation**: Relaxed GNSS stability thresholds to accommodate Samsung A15 hardware scheduling latency (Jitter: 500ms -> 3000ms, Gap: 200ms -> 1000ms). Implemented transition "muzzling" in `ForensicAuditor` to suppress false-positive stability gaps during polling interval adaptation (e.g., stationary to moving transitions). Propagated `isAdaptationMuzzled` flag from `TrackerService` and `ViewerService` to the auditor (R-ID 262).

## 🟢 Resolved in Sep.11.56
*   **A15 Reactive Flow Stalls (#949)**:
    *   **Remediation**: Directed all shared observation flows in `SystemStatusProviderImpl` to execute on `Dispatchers.IO` using `.flowOn(Dispatchers.IO)`. This eliminates main-thread contention and ensures consistent vitality pulses for IntegrityMonitor heartbeats, preventing false-positive stall detections on budget hardware (R-ID 289).

## 🟢 Resolved in Sep.11.54
*   **Build Failure: Type Mismatch in MainViewModel (#947)**:
    *   **Remediation**: Lambda parameter mismatch resolved by nesting `combine` calls for `dashboardHealthState`. Standard `combine` is limited to 5 heterogeneous flows; nesting 3 flows inside a sub-combine brought the main arity to 4, satisfying the compiler (R-ID 289).
*   **Build Failure: HUD State Type Mismatch (#948)**:
    *   **Remediation**: Removed duplicate `HudConnectivityState`, `HudTelemetryState`, and `HudHealthState` from `:core:engine`. These UI-facing models are now exclusively managed in the `:app` module to prevent import ambiguity and ensure consistency (R-ID 286).

## 🟢 Resolved in Sep.11.52
*   **Recurring GNSS Jitter on A15 (#945)**:
    *   **Remediation**: Elevated `GNSSThread` priority to `THREAD_PRIORITY_URGENT_DISPLAY` in `HardwareProvider`. Budget hardware (A15) cores were causing scheduling starvation for background threads during sensor-heavy logic pulses, leading to ~9000ms jitter. Priority alignment ensures GNSS status callbacks are processed within the urgent scheduling window (R-ID 260).

---
*For older records, see historical git logs. (vSep.11.58)*
