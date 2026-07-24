# Issue #108: Startup Recovery Race Hardening

## 🎯 Status: Resolved (Historical)
**Category**: Service Reliability / Lifecycle

---

## 📝 Description
During automatic restoration from the landing page, the system could enter a race condition where multiple service start intents were sent before the first one could claim its "Alive" status in the repository.

## 🛠️ Resolution
- Implemented immediate "Service Alive" claim in `onCreate` of both `TrackerService` and `ViewerService`.
- Added a `LAST_SERVICE_TICK_TS_KEY` refresh as the first action in the service lifecycle.
- Hardened the `MaintenanceWorker` check to respect this claim immediately upon service startup.

## 🔗 References
- **Requirement**: R406b (Foreground Service Immediacy)
- **Files**: `ViewerService.kt`, `TrackerService.kt`, `MaintenanceWorker.kt`
