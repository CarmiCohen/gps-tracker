# Project Simplification Ideas

... (previous ideas)

17. **Cleanup Logic Simplification**: Evaluate removing `Tasks.await` from `ManagedHardware` unregistration sequences. If modern Play Services handle asynchronous unregistration without leaking native `BaseEventQueue` resources, switching to pure async cleanup would eliminate the need for thread-checks and synchronous wait blocks.

18. **Version Management Centralization**: Centralize `versionName` and `versionCode` declarations into `libs.versions.toml` or a shared `build.gradle` script. This would eliminate the manual requirement to update multiple files and documentation during baseline synchronization, reducing the risk of version mismatch (vSep.12.45).
