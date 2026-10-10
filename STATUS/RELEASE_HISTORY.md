# Project History & Versioning (Oct10.1)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct10.1 (Restoration Baseline)
- **Build Recovery (#BUILD-RESTORE)**: Successfully restored project to the Oct8.1 baseline (64faffd) to resolve fatal KAPT metadata corruption and structural inconsistencies introduced during the 8.12-8.16 hardening cycles.
- **Oct10.1 Initialization**: Established new stable foundation for re-integration of native JNI enhancements.

## Oct.8.1 (Behavioral Reason Promotion)
- **Behavioral Reason Promotion (#SIMP-1007-17)**: Instrumented Sentinel-to-Telemetry health propagation and fixed coordinator key collision.
- **Sentinel Validation**: Centralized GNSS health evaluation in `SentinelValidator`.

## Oct.3.2 (HUD Stabilization & Build Recovery)
- **HUD Stabilization (#1420-S)**: Resolved multiple compilation failures across the UI layer following the HUD interface slicing refactor. Aligned call sites in `AlarmActivity` and `MainAppContent` with the new `Locatable` interface. (SOT ID 603).
- **Interface Alignment**: Synchronized property names (`locationPendingReason`) across `MainViewModel`, `UiStateCoordinator`, and `SharedUiComponents` to maintain type-safety.

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
