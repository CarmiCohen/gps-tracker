# 🏛️ Resolution Archive - Sep.28.13

## 🏁 Issue #1361: Forensic Reliability Math Hardening
*   **Resolved**: Sep.28.13
*   **Root Cause**: Standard `Double` math in the `LogRepository` reliability EMA calculation was prone to cumulative precision loss during high-frequency forensic trace bursts (100Hz+). This could lead to drifting metrics and unreliable "stuck" alerts during long-run soak simulations.
*   **Remediation**:
    *   **LogRepository.kt**: Refactored `liveReliability` and `RELIABILITY_EMA_ALPHA` to use `BigDecimal` with a fixed scale of 8 and `HALF_UP` rounding.
    *   **Health Integration**: Added a high-precision decimal-to-double conversion step when pushing metrics to the `TelemetryRepository`.
*   **Significance**: Medium (Forensic Integrity & Precision).
*   **SOT ID**: 528 (Forensic Reliability Math Hardening)

## 🏁 Issue #1360: Mismatched Unregistration Method Signatures
*   **Resolved**: Sep.28.12
*   **Root Cause**: Following the migration to centralized `TimeProvider` in `Sep.28.11`, the helper methods for hardware listener unregistration (`ManagedNetworkCallback.unregister`, `ManagedLocationCallback.unregister`, etc.) were updated to require a `TimeProvider` instance for latency auditing. However, several production call sites were left with legacy signatures, causing compilation failures.
*   **Remediation**:
    *   **AndroidNetworkProvider.kt**: Injected `TimeProvider` and updated `performUnregistration` to pass the authority to `ManagedNetworkCallback.unregister`.
    *   **HardwareSuite.kt**: Updated all deferred teardown and revival pulse unregistration paths to pass the injected `timeProvider`.
    *   **SystemStatusProvider.kt**: Updated the `awaitClose` block in `observeInternetStatus` to pass `timeProvider` to the network callback unregistration.
*   **Significance**: Medium (Temporal Integrity & Build Stability).
*   **SOT ID**: 527 (Signature Harmonization)

## 🏁 Issue #1359: Temporal Precision & Service Logic Hardening
*   **Resolved**: Sep.28.11
*   **Root Cause**: While the `BootLifecycleAuthority` was centralized, several high-level background components (`MonitorService`, `ConnectivitySuite`, `SystemStatusProvider`) still relied on direct `SystemClock.elapsedRealtime()` or `System.currentTimeMillis()` calls. This created potential for logic drift and hindered the ability to mock time in behavioral simulation tests.
*   **Remediation**:
    *   **MonitorService.kt**: Audited and verified all tick logic and forensic captures use injected `timeProvider` for monotonic and wall-clock anchors.
    *   **BaseMonitorService.kt**: Migrated foreground service update throttling and tick loop interval calculations to `timeProvider`.
    *   **ConnectivitySuite.kt**: Refactored network re-join backoffs, signaling teardown duration measurements, and RTT evaluations to use centralized temporal logic.
    *   **SystemStatusProvider.kt**: Migrated internet status caching and hardware permission refresh TTL logic to `timeProvider.elapsedRealtime()`.
*   **Significance**: Medium (Temporal Integrity & Testability).
*   **SOT ID**: 526 (Temporal Logic Hardening)

...
*(Full historical records maintained in SOT Archive)*
