3.  **Automated Leak Detection**: Integrate a lightweight `BaseEventQueue` monitor in debug builds to alert when hardware resources are orphaned (R747/R748).
4.  **ManagedLocationProvider**: Create a wrapper around `FusedLocationProviderClient` that forces synchronous unregistration for all internal callbacks to eliminate repeated `Tasks.await()` boilerplate (R748).
5.  **Synchronous Flow Utility**: Create a `safeCallbackFlow` helper that enforces the SOT 1.8 unregistration pattern (CountDownLatch/Tasks.await) to reduce boilerplate and prevent future leaks in SystemStatusProvider (R749).
