# Resolution Archive - Oct28.4

## ✅ Resolved Issues (Oct28.4)

### 1. SIMP-OCT28.2-S1: Pruning Telemetry Aggregation
- **Symptoms**: Purged record counts in telemetry were instantaneous per maintenance cycle, making it difficult to visualize long-term maintenance efficiency trends in forensic tools.
- **Root Cause**: Lack of a rolling average mechanism for maintenance-related data removal metrics.
- **Resolution**: Implemented an Exponential Moving Average (EMA) for purged record counts in `MainRepository.kt` using a 0.3 Alpha (`MAINTENANCE_PURGED_EMA_ALPHA`). Added `maintenancePurgedEma` to `SystemHealthState` and `IntegrityState` to ensure the smoothed metric is available in both local health audits and remote telemetry transmissions. (R-ID 756).

---

## ✅ Resolved Issues (Oct27.10)

### 1. SIMP-OCT27.8-S1: Database Transaction Timeout
- **Symptoms**: Potential permanent system stalls or deadlocks if a database transaction (e.g., during forensic recovery or maintenance) hangs indefinitely due to I/O or SQLite issues.
- **Root Cause**: Lack of a watchdog mechanism for the `recoveryLock` which gates all critical database maintenance and recovery paths.
- **Resolution**: Implemented a global `DATABASE_TRANSACTION_TIMEOUT_MS` (30s) in `EngineConstants.kt`. Wrapped all `recoveryLock.withLock` blocks in `LogRepository.kt` with `withTimeout` safety envelopes. Implemented specific logging for `TimeoutCancellationException` to ensure forensic visibility into lock timeouts without crashing the background loops. (R-ID 755).
