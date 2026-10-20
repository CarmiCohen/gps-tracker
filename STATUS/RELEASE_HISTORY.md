# Project History & Versioning (Oct20.32)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct20.32 (Current - UI Hardening & Navigation Fixes)
- **Back Button Termination Fix (#1424)**: Addressing critical bug where the system "return" button closes the app.
- **Version String Alignment (#1425)**: Relocating the version identifier to the absolute bottom of the UI for better branding compliance.
- **HUD Header Scrim (#1426)**: Improving button visibility in the upper HUD by adding a background scrim to prevent map occlusion.

## Oct20.31 (Stealth & Authority Baseline)
- **Tracker Stealth Audit (#Oct20.31-F1)**: Verified absolute stealth on Tracker devices (no alarms, no sirens, no local control) as per R872.
- **StatusBar Logic Hardening (#1423)**: Optimized vertical stacking and telemetry colors in portrait mode.

## Oct20.7 (Large Document Archival)
- **Forensic Document Archival (#ISSUE-OCT20.7-F1)**: Optimized `.md` file performance by migrating SOT IDs 1–774 and legacy records to `docs_history_archive.md`.
