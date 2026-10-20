# Project History & Versioning (Oct20.7)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct20.7 (Large Document Archival)
- **Forensic Document Archival (#ISSUE-OCT20.7-F1)**: Optimized `.md` file performance by migrating SOT IDs 1–774 and all legacy hardening era resolutions (July.2024 and earlier) into `STATUS/docs_history_archive.md`. 
- **Rule 8 Compliance**: Strictly followed Large File Protection rules during the migration to ensure zero data loss.

## Oct20.3 (Hardware Profile Regression)
- **Hardware Profile Test Hardening (#OCT20.3-F1)**: Fixed compilation in `HardwareSuiteProfileTest.kt` by providing mock stubs for the unified network provider.

## Oct20.2 (Simulation Hardening)
- **Viewer Simulation Determinism (#OCT19.0-F1)**: Hardened mode restoration bypass in `MainViewModel`.

## Oct20.0 (Integrity Baseline)
- **Document Integrity Baseline (#OCT20.0-F1)**: Established versioned baseline directory (`STATUS/BASELINE/`).
- **Forensic Doc-Check (#OCT19.20-F2)**: Integrated Rule 8 verification into the completion sequence.

## Oct19.20 (Forensic Document Recovery)
- **SOT Master Requirements Recovery (#OCT19.20-F1)**: Restored rules 1.207-1.224 and baseline entries 738-772 from historical commits.
