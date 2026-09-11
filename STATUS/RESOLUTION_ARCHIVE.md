# Hardening Resolution Archive (Sep.11.54)

## 🟢 Resolved in Sep.11.54
*   **Build Failure: Type Mismatch in MainViewModel (#947)**:
    *   **Remediation**: Lambda parameter mismatch resolved by nesting `combine` calls for `dashboardHealthState`. Standard `combine` is limited to 5 heterogeneous flows; nesting 3 flows inside a sub-combine brought the main arity to 4, satisfying the compiler (R-ID 289).
*   **Build Failure: HUD State Type Mismatch (#948)**:
    *   **Remediation**: Removed duplicate `HudConnectivityState`, `HudTelemetryState`, and `HudHealthState` from `:core:engine`. These UI-facing models are now exclusively managed in the `:app` module to prevent import ambiguity and ensure consistency (R-ID 286).

## 🟢 Resolved in Sep.11.52
*   **Recurring GNSS Jitter on A15 (#945)**:
    *   **Remediation**: Elevated `GNSSThread` priority to `THREAD_PRIORITY_URGENT_DISPLAY` in `HardwareProvider`. Budget hardware (A15) cores were causing scheduling starvation for background threads during sensor-heavy logic pulses, leading to ~9000ms jitter. Priority alignment ensures GNSS status callbacks are processed within the urgent scheduling window (R-ID 260).

## 🟢 Resolved in Sep.11.48
*   **Persistent Reactive Flow Stalls (#946)**:
    *   **Remediation**: Standardized the "vitality pulse" pattern by injecting `systemPulseRt` into all segmented dashboard and HUD flows in `MainViewModel`. Updated `UiStateAggregator` and `DashboardStateProvider` to include this pulse in the output states, bypassing `distinctUntilChanged` stalls during power-state transitions (R-ID 289).

---
*For older records, see historical git logs. (vSep.11.54)*
