# Simplification Ideas 2.0 (Aug.28.02)

*   **ManagedNetworkCallback (R750) - COMPLETE**: Specialized wrapper for `ConnectivityManager.NetworkCallback` encapsulates the `Handler` + `CountDownLatch` unregistration logic.
*   **ManagedLocationCallback (R750) - COMPLETE**: Specialized wrapper for `FusedLocationProvider` updates encapsulates the `Tasks.await` logic.
*   **Unified Hardware Repository**: Consider merging `GpsManager` and `AppSensorManager` into a single `HardwareProvider` now that disposal logic is unified.
