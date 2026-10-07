# 📜 Resolution Archive

## 🟢 Resolved in Oct7.2
*   **Issue #QA-1006-12: Diagnostic Hardening & Forensic Audit.**
    *   **UI Fix**: Corrected permission label mapping in `DiagnosticsScreen.kt` for Exact Alarms (Rule 1.123).
    *   **Verification**: Confirmed signaling efficiency metrics correctly reflect conflation savings (~99% for repeated logs) during 100Hz pressure tests.
    *   **A15 Compliance**: Verified `safeDouble` protection and FGS Special Use attributes for Android 15 compatibility.

## 🟢 Resolved in Oct7.1
*   **Issue #SIMP-1006-14: Telemetry Field Pruning.**
    *   **Root Cause Remediation**: Separated engine-internal evaluation state from transmission state in the `LocationUpdate` monolith.
    *   **Architectural Hardening**: Marked scratchpad fields (`snrSnapshot`, `vibeSnapshot`, `suppressionNote`) and tick-local synchronization fields (`nowRt`, `nowTs`, `isMuzzled`, etc.) as `@Transient`. This ensures they are excluded from JSON serialization (used in signaling and secondary logging), reducing wire payload size and clarifying the API boundary for external consumers.

## 🟢 Resolved in Oct6.23
*   **Issue #QA-1006-12: Signaling Efficiency Hardening.**
    *   **Root Cause Remediation**: Identified that log conflation windows were fixed and did not adapt to burst pressure, causing premature flushes.
    *   **Architectural Hardening**: Refactored `SmartSignalingDispatcher` to implement dynamic conflation window scaling for logs, synchronized with the telemetry bucket strategy. This ensures 100Hz bursts are correctly conflated into high-density packets.
*   **Issue #SIGN-1006-13: Conflation Strategy Consolidation.**
...
