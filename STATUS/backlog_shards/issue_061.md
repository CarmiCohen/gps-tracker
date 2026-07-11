# Issue #061: Forensic Logging Consolidation (R979)
**Status: RESOLVED (v9.3.12)**
**Component: :core:engine, :app**

## Description
Standardize the "Special Color" (Pink) forensic logging across all modules. This requires moving away from ad-hoc logging and using a unified Room-based persistence layer (`LogDao`/`LogEntity`) to ensure forensic continuity.

## Verification Path
- [x] Refactor `LogEntity` to include authoritative forensic fields.
- [x] Implement standardized DAO methods in `LogDao` for forensic event insertion.
- [x] Verify that `TelemetryAggregator` and `DashboardUseCase` events are captured in the standardized format.
- [x] Confirm "Pink" logs appear correctly in the Forensic UI and are persistent across app restarts.

## Resolution Summary
Standardization completed in v9.3.12. All modules now utilize the unified Room persistence layer for forensic events, ensuring data integrity and UI consistency.
