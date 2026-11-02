# Resolution Archive

## ✅ Resolved Issues (Nov2.0)

### 1. ISSUE-REVOL37.4-F1: Dependency Mismatch in :app-sensor
- **Symptoms**: Instrumented tests failed to compile due to unresolved references (`runTest`, `ServiceTestRule`, `Composable`) and missing libraries (`play-services-location`, `datastore-preferences`).
- **Root Cause**: The `app-sensor` module was missing explicit dependencies for Compose testing, Google Play Services, and DataStore, which were previously assumed to be transitively available but were pruned during the Revol37.3 hardening.
- **Resolution**: Synchronized the Compose BOM to `2024.02.01` and added explicit `androidTestImplementation` for `ui-test-junit4`, `rules`, `runner`, and `kotlinx-coroutines-test`. Restored `play-services-location` and `datastore-preferences` to the main implementation block. (R-ID 88).

---

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
