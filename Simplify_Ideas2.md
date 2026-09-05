# Simplification Ideas 2

*   **SessionManager Clock Enforcement (Issue #918)**: Standardizing `onViewerPulse` and `onTrackerPulse` to strictly use monotonic `nowRt` allowed the removal of the `isRealtime` flag and associated branching logic in `ConnectivitySuite`.
*   **Telemetry Repository Buffer Reuse**: The current double-buffering strategy in `TelemetryRepository` successfully minimizes object churn. Consider extending this to `GnssDetail` if satellite density increases.
*   **GNSS Flyweight Unification (Issue #914)**: Now that `activeGnssDetail` is sampled in the ViewModel, we could unify `GnssDetail` into the `LocationUpdate` flyweight pool in `TelemetryRepository` to further reduce allocation churn during active satellite monitoring.
