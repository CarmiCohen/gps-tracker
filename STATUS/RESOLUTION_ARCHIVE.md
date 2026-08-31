# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.31.06 (vAug.31.06)
*   **Issue #873 Validated**: **Repetitive getPackageName Log Spam (R759 violation)**. Overrode `getPackageName()` in `GpsApplication` to return the shadow-cache value.
    *   **Shadow-Cache Enforcement**: Ensured that all system service calls (e.g., `AppOpsManager`, `Settings`) using the `ApplicationContext` bypass redundant native IPC lookups.
    *   **Diagnostic Log Suppression**: Confirmed that the fix effectively silences Samsung-specific diagnostic logs on A15 hardware, reducing Binder overhead and Logcat saturation.
    *   **Architectural Alignment**: Updated **Rule 1.9 (R759)** to mandate the `getPackageName()` override as the authoritative caching pattern for system identifiers.

## 🟢 Aug.31.05 (vAug.31.05)
*   **Issue #810-M Validated**: **Acoustic Floor Calibration Audit**. Verified adaptive floor recovery logic via `AcousticCalibrationTest`.
    *   **Recovery Verification**: Confirmed that the floor correctly recovers from 90dB saturation to the 50dB baseline within expected forensic timeframes.
    *   **Contraction Logic**: Validated that the time-based contraction factor (`ACOUSTIC_FLOOR_CONTRACTION_EMA`) ensures recovery even during duty-cycle off-periods.
    *   **Baseline Alignment**: Confirmed that `SentinelValidator` maintains the `ACOUSTIC_FLOOR_MIN_DB` floor regardless of input noise levels.

## 🟢 Aug.31.04 (vAug.31.04)
*   **Issue #779 Validated**: **Forensic Replay & Metadata Hardening**. Extended the `ForensicSanitizer` policy to the telemetry mapping and historical audit layers.
    *   **Telemetry Hardening**: Updated `TrackerStatus.toMap()` in `Models.kt` to scrub technical network identifiers (`net_interface`) before transmission to viewers or JSON export.
    *   **Audit Sanitization**: Hardened `HistoryManager.kt` to sanitize continuity audit and backfilling logs at the source, ensuring no internal paths leak into the event stream (R779).
    *   **Consistency Audit**: Confirmed that all `MainFileHelper` export paths and `LogEntry` JSON serializations are rigorously utilizing the centralized scrubbing utility.

## 🟢 Aug.31.03 (vAug.31.03)
*   **Issue #762 Validated**: **Acoustic Duty-Cycle & [ULTRA] Badge Correlation**. Hardened end-to-end propagation of the `isUltraLongStationary` state to ensure definitive hardware transparency.
    *   **Integrity Monitoring**: Updated `IntegrityMonitor.kt` to observe `isUltraLongStationaryFlow` from `HardwareProvider`, enabling local health state synchronization (R765).
    *   **Forensic Mapping**: Hardened `TelemetryUseCase.kt` and `HistoryManager.kt` to carry the relaxation flag across all ingestion and persistence paths (R778).
    *   **Service Coordination**: Updated `TrackerService.kt` to propagate the definitive hardware state to both the signaling pipeline (Viewer HUD) and the analytical history streams (Ribbons).

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
