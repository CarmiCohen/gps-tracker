# Issue #528: DashboardUseCase Tombstone

## Status: Resolved (July.23.04)
## Requirement: R524 (Architectural Purity)

### Description
`DashboardUseCase.kt` became an orphaned "middle-man" after logic was migrated to `DashboardStateProvider`. Its presence increased cognitive load and violated the goal of ViewModel-Provider decoupling.

### Resolution
- **Decommissioned**: The file was marked as a tombstone using `@Deprecated` (due to environment tooling constraints on physical deletion).
- **Scrubbing**: All references in `MainViewModel.kt` and `AppModule.kt` were removed.
- **Migration**: Finalized the transfer of formatting logic (Speed/Altitude/Battery ribbons) to `DashboardStateProvider.kt`.

### Verification
- [x] Project compiles with zero references to `DashboardUseCase`.
- [x] UI rendering remains identical using the new provider-based logic.
