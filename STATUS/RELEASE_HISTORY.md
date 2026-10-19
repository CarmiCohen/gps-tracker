# Project History & Versioning (Oct19.20)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct19.20 (Forensic Document Recovery)
- **SOT Master Requirements Recovery (#OCT19.20-F1)**: Fully restored `SOT_MASTER_REQUIREMENTS.md` after accidental shortening. Recovered rules 1.207-1.224 and baseline entries 738-772 from historical stable commits while preserving new Oct19.19 additions.
- **Rule Count Synchronization**: Verified and locked the architectural rule count at 299.

## Oct19.19 (Mode Restoration Bypass)
- **Test Mode Restoration Bypass (#OCT19.19-F1)**: Implemented a persistent bypass for automatic service resumption (R-ID 242). This allows testers to force the app to stay at the Landing Screen on relaunch even if a prior mode was active.
- **Diagnostics Expansion**: Added a toggle for the bypass hook in the Diagnostics UI.
- **ViewModel Lifecycle Hardening**: Refactored cold-start initialization to conditionally skip `UiEffect.StartService` based on simulation state.

## Oct18.2 (Test Mode DI Hardening)
- **Test Mode Dependency Injection (#SIMP-OCT17.11-S1)**: Eliminated static state leakage in `LogRepository` by migrating to a Hilt-injected `TestModeProvider`.
... [Remaining historical entries preserved] ...
