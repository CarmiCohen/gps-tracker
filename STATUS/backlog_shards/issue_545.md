# Issue #545: Production Logging Leak (`StackLog`)

## 🎯 Status: Resolved (July.25.12)
**Category**: Performance / Diagnostic Cleanup

---

## 📝 Description
Unidentified `StackLog` traces were flooding Logcat during network initialization. These appeared to be full stack traces prefixed with `StackLog` printed during `registerNetworkCallback`.

## 🔍 Observations
- **Root Cause**: Redundant registrations of `ConnectivityManager.NetworkCallback` in the `ConnectivitySuite` singleton. On Samsung A15 devices, the platform's connectivity stack triggers diagnostic stack traces (prefixed with `StackLog`) when a callback is registered multiple times without being unregistered.
- **Impact**: Logcat flooding, unnecessary I/O overhead, and potential performance degradation during network transitions.

## 🛠️ Resolution
- Implemented idempotent lifecycle management in `ConnectivitySuite.kt`.
- Added an `isStarted` atomic flag to ensure `registerNetworkCallback` and internal maintenance loops are initialized exactly once per application session.
- Guarded `stop()` logic to reset the `isStarted` flag, allowing clean re-initialization if the service is fully decommissioned and restarted.

## 🔗 References
- **Requirement**: Noise Cleanup
- **Cycle**: July.25.12
