# Project History & Versioning (Oct20.33)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct20.33 (Current - Stealth Hardening)
- **Tracker UI Role-Lock (#Oct20.31-F1)**: Architectural enforcement of R872. Purged all forbidden interactive elements (Alerts, Sound, Geofence management) and alarm manifestations from Tracker devices.
- **Operational Forensic Audit Sharding**: Decoupled Chapter 0 of the test procedure into `OPERATIONAL_FORENSIC_AUDIT.md` for better preservation and audit focus.

## Oct20.32 (UI Hardening & Navigation Fixes)
- **Back Button Termination Fix (#1424)**: Addressing critical bug where the system "return" button closes the app.
- **Version String Alignment (#1425)**: Relocating the version identifier to the absolute bottom of the UI for better branding compliance.
- **HUD Header Scrim (#1426)**: Improving button visibility in the upper HUD by adding a background scrim to prevent map occlusion.
- **Home Point Synchronization (#1428)**: Implemented bidirectional geofence sync.

## Oct20.31 (Stealth & Authority Baseline)
- **Tracker Stealth Audit (#Oct20.31-F1)**: Verified absolute stealth on Tracker devices (no alarms, no sirens, no local control) as per R872.
- **StatusBar Logic Hardening (#1423)**: Optimized vertical stacking and telemetry colors in portrait mode.
