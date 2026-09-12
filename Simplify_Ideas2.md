# Project Simplification Ideas

... (previous ideas)

17. **Cleanup Logic Simplification**: Evaluate removing `Tasks.await` from `ManagedHardware` unregistration sequences. If modern Play Services handle asynchronous unregistration without leaking native `BaseEventQueue` resources, switching to pure async cleanup would eliminate the need for thread-checks and synchronous wait blocks.
