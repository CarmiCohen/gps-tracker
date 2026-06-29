# Handover Document - Hardening Phase (v8.9.60)

## 🎯 Recent Changes Summary
This document tracks high-assurance changes and forensic alignment fixes applied during the current session.

### v8.9.60
- **Forensic Ghost Mode Restoration (Issue #458)**: Fixed a bug where Tracker-role devices showed local status as stale. Corrected timestamp propagation in `SharedUiComponents.kt`.
- **Telemetry Freshness Logic (Issue #460)**: Relaxed `isLocalTelemetryFresh` check to support sensor-only telemetry existence, preventing "Ghost Mode" before the first GPS fix.
- **Unicode Label Fix (Issue #459)**: Corrected double-escaped thin-space characters in `StatusBar` and `GlobalStatusBar` labels.

### v8.9.56
- **Release Baseline**: Completed hardening phase for sensor contradictions and unified Ghost Mode status.

## ⚠️ Open Technical Debt
- **Issue #461**: Settings uniqueness enforcement is implemented in the repository but needs verified UI feedback for the error string.

## 📊 Compliance Status
- **R325 (Spatial Anchoring)**: Side-by-side accuracy visualization implemented. Pending validation on small-screen devices.
- **R338 (Ghost Mode)**: Thresholds unified at 15s across all UI components.
