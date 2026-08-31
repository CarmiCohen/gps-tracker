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

## 🟢 Aug.31.04 (vAug.31.04)
*   **Issue #779 Validated**: **Forensic Replay & Metadata Hardening**. Extended the `ForensicSanitizer` policy to the telemetry mapping and historical audit layers.

## 🟢 Aug.31.03 (vAug.31.03)
*   **Issue #762 Validated**: **Acoustic Duty-Cycle & [ULTRA] Badge Correlation**. Hardened end-to-end propagation of the `isUltraLongStationary` state to ensure definitive hardware transparency.

## 🟢 Aug.31.02 (vAug.31.02)
*   **Issue #782 Validated**: **UI Performance Hardening (History Sampling)**. Hardened the forensic ribbon pipeline in `MainViewModel.kt` to ensure Davey immunity during high-frequency stress scenarios.
    *   **Throttling**: Integrated the `sample()` operator into all forensic history flows.
    *   **Hardware Baseline**: Applied a 3000ms sampling window for Samsung A15 hardware.

## 🟢 Aug.31.00 (vAug.31.00)
*   **Issue #782 Resolved**: **Protocol Audit - Binary Schema Expansion & Forensic Audit**. Expanded the `RealtimeStatus` Protobuf schema to carry `violationUptimeMs` and `isUltraLongStationary`.
    *   **Binary Hardening**: Updated `TrackerStatus.writeTo` and `toMap` to serialize expanded metrics.
    *   **Database v75 Migration**: Implemented `MIGRATION_74_75` to add violation metrics.

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
