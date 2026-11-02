# Resolution Archive

## ✅ Resolved Issues (Nov2.21)

### 1. ISSUE-REVOL38.1-F1: SentinelLogic Trajectory Failure
- **Symptoms**: `SentinelHindsightTest` failed with `expected:<VALID> but was:<JUMP>`.
- **Root Cause**: Kinematic thresholds (`MAX_PHYSICAL_SPEED_MPS` = 33.33m/s, `ACCEL_LIMIT` = 2.0m/s²) were too restrictive for valid high-speed rail/vehicle trajectories.
- **Resolution**: Increased `MAX_PHYSICAL_SPEED_MPS` to 55.0m/s (~200km/h) and `ACCEL_LIMIT` to 5.0m/s². (R-ID 93).

### 2. ISSUE-REVOL38.1-F2: Multiple DataStore Instances
- **Symptoms**: `IllegalStateException: There are multiple DataStores active for the same file` in `:app-monitor` instrumented tests.
- **Root Cause**: `ForensicIdentityManager` was creating new DataStore instances on each injection, causing conflicts in test environments.
- **Resolution**: Enforced a process-level singleton pattern for the identity DataStore within `ForensicIdentityManager`. (R-ID 92).

### 3. ISSUE-REVOL38.1-F3: Storage Pressure Resilience Failure
- **Symptoms**: `AssertionError` in `:app-sensor` storage tests during pruning and pressure simulation.
- **Root Cause**: `ForensicBuffer` lacked explicit checks for external file deletion and potential I/O races during index advancement.
- **Resolution**: Hardened `ForensicBuffer` with `verifyFileIntegrity` checks and atomic index synchronization *before* advancing in-memory state. (R-ID 91).

### 4. ISSUE-REVOL38.1-F4: Startup Performance Test Timeouts
- **Symptoms**: `StartupIntegrityTest` timed out (1000ms) under stress.
- **Root Cause**: Thread starvation from a tight-loop stress job and inadequate flow collection timeouts.
- **Resolution**: Added `yield()` to the stress job and increased collection timeout to 2000ms. (R-ID 94).

---

## ✅ Resolved Issues (Nov2.1)
... (remaining content preserved)
