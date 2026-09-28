# 🏛️ Resolution Archive - Sep.28.11

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

## 🏁 Issue #1357: Build Pipeline Dependency Pruning (KSP Migration)
*   **Resolved**: Sep.28.10
*   **Root Cause**: The project relied on `kapt` for Room and Hilt, introducing build overhead and Java stub generation risks.
*   **Remediation**: Migrated to `KSP` for all annotation processing.
*   **Significance**: Low (Build Speed & Architectural Modernization).
*   **SOT ID**: 525 (Build Pipeline Dependency Pruning)

...
*(Full historical records maintained in SOT Archive)*
