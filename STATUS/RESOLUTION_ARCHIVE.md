# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 280**

## 1. Landing Page ANR & Data Flow Hardening (v9.3.52)
*   **Requirement R952**: Landing Page Responsiveness. Eliminated main-thread starvation by offloading heavy database-to-UI mapping operations to `Dispatchers.Default`.
*   **Log Flow Optimization**: Refactored `LogRepository.eventLogsFlow` to perform `LogEntry` mapping on a background dispatcher.
*   **Trail & Violation Optimization**: Refactored `MainRepository` trail and violation flows to use `Dispatchers.Default`, preventing UI stutter during cold starts with active modes.
*   **State Subscription Hardening**: Offloaded history list reconciliation and integrity JSON parsing in `StateSubscriptionUseCase`.

## 2. Startup ANR & Relay Authority (v9.3.18)
*   **Requirement R403**: Startup ANR Remediation. Implemented dynamic heartbeat recovery logic. The system now uses a 2s heartbeat during the first 60 seconds of operation to reduce CPU pressure.

## 3. Architectural Synchronization & Service Hardening (v9.3.16)
*   **Issue #080**: Lift Detection Logic Inconsistency. Remediated `MainAlarmLogic` in `:core:engine` to compute violations using the relative barometer delta.

... [See historical logs for full resolutions]
