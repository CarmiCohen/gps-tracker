# Issue #099: Cold-Start Hardening (R955b)

## Status: Resolved (July.20.00)
## Requirement: R955b

### Description
Low-end hardware (Samsung A15) experienced ANRs when the application attempted to initialize all hardware monitors and database connections simultaneously upon cold start.

### Resolution
- **Staggered Delay**: Implemented a mandatory 500ms delay in the `MainViewModel` and `TrackerService` startup sequence before initiating secondary observations.
- **Resource Prioritization**: Critical connectivity checks are performed immediately, while forensic sensor registration is deferred.
- **Boot Sequence Optimization**: Decoupled the notification channel creation from the heavy service initialization path.

### Verification
- [x] Verified that startup frame time on Samsung A15 remains under 16ms.
- [x] Confirmed no ANR reports during device reboot stress tests.
