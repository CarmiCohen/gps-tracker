# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.30.13 (vAug.30.13)
*   **Issue #779 Resolved**: **Forensic Metadata Leak Cleanup**. Implemented a centralized `ForensicSanitizer` utility to scrub absolute internal paths (e.g., `/data/user/0/...`) and normalize hardware identifiers (e.g., `Build.MODEL`) from all exported logs, trails, and telemetry payloads.
    *   **Logging Hardening**: Integrated sanitization into the global `Timber` tree in `GpsApplication.kt` to ensure stack traces and error messages are scrubbed before persistence.
    *   **Export Hardening**: Updated `MainFileHelper.kt` to sanitize file I/O error messages and `LogEntry.toJSONObject()` to ensure that exported JSON snapshots are forensically clean.
    *   **SOT Integration**: Added Architectural Rule R779 to enforce mandatory sanitization at the logging edge.

## 🟢 Aug.30.12 (vAug.30.12)
*   **Concern #781 Resolved**: **Forensic Documentation Restoration**. Completed the root-cause restoration of the "Source of Truth" (SOT). Reconstructed `SOT_MASTER_REQUIREMENTS.md` by retrieving and listing all 148 Functional Requirements (R101-R999) from historical logs, eliminating all placeholders. Expanded `Simplify_Ideas2.md` with full evaluative logic for Issue #778 (Stationary Derivation) and hardware-specific remediations (Samsung A15 stalls). Hardened `RESOLUTION_ARCHIVE.md` to ensure full descriptive integrity for all technical concerns from Aug.28 onwards.

## 🟢 Aug.30.09 (vAug.30.09)
*   **Concern #778 Evaluated**: **Stationary Derivation Logic**. Evaluated the feasibility of deriving the "Ultra-Long" stationary state on the Viewer side using coordinate monotonic timestamps. Determination: Flag MUST be retained in the telemetry payload. Derive-on-Viewer would introduce high state-mismatch risk during 5-minute relaxed polling intervals and lacks the IMU fidelity available to the Tracker. Transparency and State Parity prioritized over negligible payload savings.

## 🟢 Aug.30.08 (vAug.30.08)
*   **Concern #759 Validated**: **Logcat IPC Spam Remediation**. Confirmed via comprehensive codebase audit that all direct `getPackageName()` and `Process.myUid()` calls have been migrated to the `GpsApplication` shadow-caches. This successfully removes the IPC binder overhead that previously triggered Samsung-specific diagnostic log flooding on A15 hardware (R759).

## 🟢 Aug.30.07 (vAug.30.07)
*   **Concern #777 Resolved**: **Marker Hydration Segmentation (Optimization)**. Applied segmented coroutine pattern with `yield()` to `updateHomePoints` in `MapOverlayManager.kt`. This ensures that home point marker instantiation is interleaved with UI frames, preventing Main-thread stalls when large numbers of home points are present (R777).

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
