# Issue #061: Forensic Logging Consolidation
**Status**: Open
**Priority**: Medium
**Requirement**: #061

## Description
Create a `ForensicLogUseCase` to standardize "Special Color" (Pink) logging across the app and engine modules.

## Tasks
- Define `ForensicLogUseCase` interface.
- Migrate manual pink logging calls in `TrackerService` and `LocationProcessor` to the new UseCase.
- Ensure consistent metadata (lat/lng/accuracy) is attached to all forensic events.
- Validation tracked in #065.
