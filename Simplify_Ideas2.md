# Simplification Ideas 2.0 (Aug.28.03)

*   **ManagedNetworkCallback (R750/R752) - COMPLETE**: Specialized wrapper for `ConnectivityManager.NetworkCallback` encapsulates the `Handler` + `CountDownLatch` unregistration logic with Main Looper deadlock protection.
*   **ManagedLocationCallback (R750) - COMPLETE**: Specialized wrapper for `FusedLocationProvider` updates encapsulates the `Tasks.await` logic.
*   **Unified Hardware Repository**: Consider merging `GpsManager` and `AppSensorManager` into a single `HardwareProvider` now that disposal logic is unified.
*   **ManagedBroadcastReceiver (Proposed)**: Create a `ManagedBroadcastReceiver` abstraction similar to `ManagedNetworkCallback` to unify synchronous unregistration for Battery and Power status listeners in `SystemStatusProvider`.
