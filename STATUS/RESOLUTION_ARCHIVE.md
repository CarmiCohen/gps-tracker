# Resolution Archive

## ✅ Resolved Issues (Nov1.7)

### 1. ISSUE-REVOL37.1-F1: Foreground Service Permission Regression
- **Symptoms**: `SensorService` crashed on startup on Android 14 (API 34) with `SecurityException` during FGS activation.
- **Root Cause**: On API 34+, starting a Foreground Service of type `location` requires active `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` *and* `FOREGROUND_SERVICE_LOCATION` runtime permissions.
- **Resolution**: Refactored `SensorService` to defer all sensitive component initialization (Sensors, Location, Connectivity) until `onStartCommand` and only if `hasRequiredPermissions()` returns true. Hardened `AdminActivity`'s `PermissionGuard` to explicitly verify and request all API 34+ mandatory permissions. (R-ID 10/86).

### 2. ISSUE-REVOL37.1-F2: Forensic Audit Signature Leak
- **Symptoms**: `StackLog` signatures (system-generated stack traces) were leaking into Logcat when the service called `ConnectivityManager.registerDefaultNetworkCallback`.
- **Root Cause**: The A15 hardware audit profile hooks into specific Framework connectivity APIs and dumps the stack trace to Logcat.
- **Resolution**: Removed the redundant Framework-level network callback registration from `SensorService`, relying instead on the internal heartbeats of the signaling protocol. (R-ID 86).

### 3. ISSUE-REVOL37.2-F1: Hilt Test Injection Failure in `:app-sensor`
- **Symptoms**: `java.lang.IllegalStateException: The component was not created` during instrumented hardening tests.
- **Root Cause**: Use of `var` for `HiltAndroidRule` in Kotlin tests, which can lead to delayed or failed initialization of the Dagger component during the JUnit lifecycle.
- **Resolution**: Stabilized test infrastructure by transitioning to `val` for `HiltAndroidRule` in all sensor-side instrumented tests, ensuring deterministic injection before `@Before` and `@Test` execution. (R-ID 87).

---

## ✅ Resolved Issues (Nov1.6)
... (remaining content preserved)
