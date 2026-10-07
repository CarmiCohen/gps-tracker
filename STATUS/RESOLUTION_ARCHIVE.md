# 📜 Resolution Archive

## 🟢 Resolved in Oct7.5
*   **Issue #SIMP-1007-15: Unified Snapshot Container (Completion).**
    *   **Engine Parity**: Completed migration of `EngineConnectionPoint` and `AlarmEvent` to use the unified `ForensicSnapshot` container.
    *   **App Parity**: Refactored `ConnectionPoint` and `LogEntry` in `Models.kt` to utilize the unified container for diagnostic probes.
    *   **Logic Consolidation**: Updated `contentEquals`, `reset`, and `duplicate` methods across all telemetry models to ensure atomic snapshot handling and prevent data loss during pipeline emission.
    *   **Persistence Mapping**: Verified Room entity mapping in `LogRepository` and `TelemetryMapper` to ensure flat database columns correctly interface with the domain container.

## 🟢 Resolved in Oct7.4
*   **Issue #SIMP-1007-15: Unified Snapshot Container (Architecture).**
    *   **Architecture**: Introduced `ForensicSnapshot` data class in `LocationUpdate.kt` to group `snr`, `vibe`, `thermal`, and `heap` probes.
    *   **IntegrityState Hardening**: Refactored `IntegrityState` to own a `forensic: ForensicSnapshot` property, simplifying `copyFrom()` and `reset()` logic.
    *   **Monolith Delegation**: Updated `LocationUpdate` property delegates to route through `integrity.forensic`, maintaining API compatibility for the engine and UI while reducing internal complexity.

## 🟢 Resolved in Oct7.3
*   **Issue #QA-1007-1: Telemetry Forensic Expansion & Radio Soak Validation.**
    *   **Protobuf Expansion**: Promoted 12 internal engine flags (muzzled, siren, hardware health, environmental lockouts, snapshots) to Protobuf for remote diagnostics.
    *   **Dispatcher Hardening**: Implemented first-entry starvation cap in `SmartSignalingDispatcher` to prevent indefinite conflation delays under high-pressure bursts.
    *   **Data Integrity**: Fixed `LocationUpdate.duplicate()` defect where body-defined properties were lost during pipeline emission.

## 🟢 Resolved in Oct7.2
*   **Issue #QA-1006-12: Diagnostic Hardening & Forensic Audit.**
    *   **UI Fix**: Corrected permission label mapping in `DiagnosticsScreen.kt` for Exact Alarms (Rule 1.123).
    *   **Verification**: Confirmed signaling efficiency metrics correctly reflect conflation savings (~99% for repeated logs) during 100Hz pressure tests.
    *   **A15 Compliance**: Verified `safeDouble` protection and FGS Special Use attributes for Android 15 compatibility.

## 🟢 Resolved in Oct7.1
...
