# Project History & Versioning (v9.3.55)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## v9.3.55 (July17.07)
- **Room Database Migration Hardening (#096)**: Resolved `IllegalStateException` on startup by harmonizing the `logs` table schema via `MIGRATION_54_55`.
- **Startup ANR Hardening (#096b)**: Offloaded database initialization and migrations to `Dispatchers.IO` in `MainViewModel`, ensuring the UI thread remains responsive during cold starts on devices like Samsung A15.

## v9.3.52 (July17.06)
- **Landing Page ANR Hardening (#092)**: Implemented explicit offloading of database-to-UI mapping operations (Logs, Trails, History) to `Dispatchers.Default`.
- **Setup Flow Deadlock (#095)**: Implemented Differential Polling and reactive auto-transitions for permission states.

## v9.3.18
- **Startup ANR Remediation (R403)**: Implemented dynamic heartbeat recovery logic (2s startup tick).
- **Relay Configuration Authority (R404)**: Standardized fallback to authoritative relay Render URL.

... [Rest of document remains unchanged]
