# Handover Status: R325 Authoritative Spatial Anchoring & Forensic Hardening (v8.9.42)

## 🎯 High-Level Requirement: R325 (Dual-Metric Authority)
The system now maintains both raw `accuracy` and engine-calculated `maxAccuracy` (authoritative uncertainty) across all layers. **`maxAccuracy` is the exclusive authority** for all logic-based evaluations (Geofences, Deduplication).

## 🛠️ Work Completed (Forensic Hardening Sequence)

### 1. Database & Persistence (v47 ➔ v50)
- **v47-v49**: Added dual-metric accuracy to `logs`, `violations`, and `connection_history`.
- **v50 (NEW)**: Added `accuracy` and `maxAccuracy` to `trail_points` table.
- **Model Parity**: `TrailPoint` and `TrailEntity` now support dual-metric storage, enabling high-fidelity path reconstruction.

### 2. Logic & Hindsight Parity
- **Rubber-Band Interpolation**: `PhysicsUtils.interpolateSegment` now performs linear interpolation for `accuracy` and `maxAccuracy`.
- **Location Engine**: `LocationProcessor.kt` successfully propagates trajectory uncertainty context to interpolated points during hindsight promotion (Issue #334).
- **Service Alignment**: `TrackerService` and `ViewerService` updated to utilize the hardened `onTrailPointSaved` signature.

### 3. Forensic Import/Export Parity
- **Import Logic**: `MainFileHelper.kt` updated to preserve `accuracy` and `maxAccuracy` when restoring trails from JSON backups.
- **Export Logic**: Verified that unified forensic backups include the full spatial uncertainty context for every point in the path.

### 4. UI & Visualization
- **Historical Reconstruction**: `OsmMap` renders historical uncertainty circles for violation markers using the precision captured at the exact moment of the alert.
- **Ghost Mode**: Propagated `isTelemetryFresh` to `LogOverlay`. Forensic logs now correctly dim to `Slate500` at the 10s threshold.

## 📊 Current Status: FORENSIC HARDENED
- **Database Schema**: v50 Stable.
- **Logic Pipeline**: Authoritative Uncertainty fully integrated from Engine -> Repo -> UI -> Export, including interpolated segments.
- **Build Status**: Stable.

## ⚠️ Next Steps for Next Session
1. **Live Validation**: Perform a Geofence violation on hardware and verify that the red marker's circle on the map matches the `maxAccuracy` forensic snapshot.
2. **Import Audit**: Use a legacy JSON trail backup (pre-v50) to ensure the import logic handles missing accuracy fields gracefully (defaulting to 0).
3. **Ghost Mode Check**: Verify the 10s dimming pulse in the HUD and Log Overlay during a real telemetry dropout.

**Status**: R325 Hindsight Parity complete. Baseline synchronized.
