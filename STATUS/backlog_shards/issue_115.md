# Issue #115: Startup Scope Hardening

## Status: Resolved (July.20.07)
## Requirement: Performance & Startup Authority

### Description
Core third-party libraries (`osmdroid` and `WorkManager`) were being initialized inside the `MainActivity`, causing initialization delays if the app was started via a background service.

### Resolution
- **Application Scope**: Migrated the configuration and setup of `osmdroid` and the `HiltWorkerFactory` to the `@ApplicationScope` within `GpsApplication`.
- **Pre-emptive Init**: Ensures that map tiles and background workers are ready to function regardless of whether the UI has been created.
- **Memory Optimization**: Centralized the singleton instances to prevent redundant allocations during service-to-activity transitions.

### Verification
- [x] Verified that map tiles load instantly upon opening the app from a notification.
- [x] Background workers successfully execute even if the UI has never been opened.
