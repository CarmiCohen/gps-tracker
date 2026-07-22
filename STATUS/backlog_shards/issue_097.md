# Issue #097: Database Migration Integrity (Room Identity Hash Stabilization)

## Status: Resolved (July.18.01)
## Requirement: R956b

### Description
A schema mismatch was detected after adding forensic fields to the database. The Room identity hash did not match the expected value, causing `IllegalStateException` on startup for existing users.

### Resolution
- **Identity Hash Restoration**: Corrected the `@Entity` field order and type alignment to match the stable v56/v57 baseline.
- **Migration Path**: Implemented `MIGRATION_56_57` to explicitly add the `sit_vz` and `sit_shock` columns without wiping user history.
- **Automated Validation**: Integrated `Room.databaseBuilder().fallbackToDestructiveMigrationFrom()` as a secondary safety measure for pre-release builds.

### Verification
- [x] Verified successful migration from v56 to v57 on test hardware.
- [x] Confirmed that `IllegalStateException` no longer occurs during startup.
