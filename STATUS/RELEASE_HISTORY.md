# Project History & Versioning (July.18.01)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## July.18.01 (v9.3.57)
- **Room Identity Hash Stabilization (#097)**: Resolved `IllegalStateException` integrity error on version 56 by adding `MIGRATION_56_57`. This migration re-harmonizes all table schemas to match Entity definitions exactly.
- **Version Baseline**: Updated versioning to July.18.01 across `build.gradle` and documentation.

## v9.3.56 (July.18.00)
- **Room Database Migration Hardening (#096)**: Harmonized all `Double` column default values to `"0"` (integer string) to resolve `IllegalStateException` on startup.

## v9.3.55
- **Startup ANR Hardening (#096b)**: Offloaded database initialization and migrations to `Dispatchers.IO` in `MainViewModel`.

... [Rest of document remains unchanged]
