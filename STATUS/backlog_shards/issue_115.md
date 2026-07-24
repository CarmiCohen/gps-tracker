# Issue #115: Startup Hardening (ApplicationScope)

## 🎯 Status: Resolved (Historical)
**Category**: Architectural / Lifecycle

---

## 📝 Description
The application was using `GlobalScope` for several background initialization tasks, which made it difficult to manage lifecycle cancellation and led to potential memory leaks during process death.

## 🛠️ Resolution
- Migrated all application-level background tasks to a managed `@ApplicationScope` (provided via Hilt).
- Ensured that repository initialization and pruning tasks are properly supervised.
- Improved the predictability of the cold-start sequence by anchoring long-running tasks to the application lifecycle.

## 🔗 References
- **File**: `app/src/main/java/com/gps19/app/GpsApplication.kt`
