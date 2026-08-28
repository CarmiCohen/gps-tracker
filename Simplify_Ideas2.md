3.  **Automated Leak Detection**: Integrate a lightweight `BaseEventQueue` monitor in debug builds to alert when hardware resources are orphaned (R747/R748).
4.  **ManagedLocationProvider**: Create a wrapper around `FusedLocationProviderClient` that forces synchronous unregistration for all internal callbacks to eliminate repeated `Tasks.await()` boilerplate (R748).
