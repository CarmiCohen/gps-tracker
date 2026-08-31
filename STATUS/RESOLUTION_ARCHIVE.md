# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.31.03 (vAug.31.03)
*   **Issue #762 Validated**: **Acoustic Duty-Cycle & [ULTRA] Badge Correlation**. Hardened end-to-end propagation of the `isUltraLongStationary` state to ensure definitive hardware transparency.
    *   **Integrity Monitoring**: Updated `IntegrityMonitor.kt` to observe `isUltraLongStationaryFlow` from `HardwareProvider`, enabling local health state synchronization (R765).
    *   **Forensic Mapping**: Hardened `TelemetryUseCase.kt` and `HistoryManager.kt` to carry the relaxation flag across all ingestion and persistence paths (R778).
    *   **Service Coordination**: Updated `TrackerService.kt` to propagate the definitive hardware state to both the signaling pipeline (Viewer HUD) and the analytical history streams (Ribbons).

## 🟢 Aug.31.02 (vAug.31.02)
*   **Issue #782 Validated**: **UI Performance Hardening (History Sampling)**. Hardened the forensic ribbon pipeline in `MainViewModel.kt` to ensure Davey immunity during high-frequency stress scenarios.
    *   **Throttling**: Integrated the `sample()` operator into all forensic history flows (`history4MFlow` through `history7DFlow`).
    *   **Hardware Baseline**: Applied a 3000ms sampling window for Samsung A15 hardware and 1000ms for standard hardware, eliminating Main-thread congestion during synthetic DB saturation (R650, R312).
    *   **SOT Integration**: Added Architectural Rule **2.11 (History Sampling Authority)** to mandate flow-throttling for high-density UI components.

## 🟢 Aug.31.00 (vAug.31.00)
*   **Issue #782 Resolved**: **Protocol Audit - Binary Schema Expansion & Forensic Audit**. Expanded the `RealtimeStatus` Protobuf schema to carry `violationUptimeMs` and `isUltraLongStationary`.
    *   **Binary Hardening**: Updated `TrackerStatus.writeTo` and `toMap` in `Models.kt` to serialize expanded metrics.
    *   **Database v75 Migration**: Implemented `MIGRATION_74_75` to add violation metrics to `pending_status_updates` and `connection_history`.
    *   **Forensic Mapping**: Hardened `TelemetryMapper.kt` history mapping functions to ensure full state parity during historical replay and analytical ribbon rendering.
    *   **UI Transparency**: Integrated `[ULTRA]` status badges into the Dashboard and StatusBar to provide deterministic feedback for low-power GNSS relaxation modes (R782).

## 🟢 Aug.30.13 (vAug.30.13)
*   **Issue #779 Resolved**: **Forensic Metadata Leak Cleanup**. Implemented a centralized `ForensicSanitizer` utility to scrub absolute internal paths (e.g., `/data/user/0/...`) and normalize hardware identifiers (e.g., `Build.MODEL`) from all exported logs, trails, and telemetry payloads.
    *   **Logging Hardening**: Integrated sanitization into the global `Timber` tree in `GpsApplication.kt` to ensure stack traces and error messages are scrubbed before persistence.
    *   **Export Hardening**: Updated `MainFileHelper.kt` to sanitize file I/O error messages and `LogEntry.toJSONObject()` to ensure that exported JSON snapshots are forensically clean.
    *   **SOT Integration**: Added Architectural Rule R779 to enforce mandatory sanitization at the logging edge.
*   **Bug Fix**: Resolved a critical logic error in `TrackerStatus.toMap()` where `baro_idx` was incorrectly reassigned to a local variable instead of being mapped, ensuring forensic data parity in JSON payloads.

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
