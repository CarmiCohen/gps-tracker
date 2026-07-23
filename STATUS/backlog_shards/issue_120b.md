# Issue #120b: I/O Stabilization - Startup Pruning Delay

## Status: Resolved (Implementation) / Pending Field Validation
## Requirement: R104b

### Description
Budget hardware (Samsung A15) experienced "UI ERROR" and frame drops during the first 5 seconds of startup due to the `deepPruneLogs` operation competing for I/O bandwidth with the Room database initialization.

### Resolution
- **Staggered Execution**: Implemented a 2000ms delay for the proactive log pruning task in `MainViewModel.kt` using `viewModelScope.launch`.
- **Resource Priority**: Deferred non-critical background maintenance until the `SystemMonitor` confirms that the primary location provider is active.

### Verification (Field Test Pending)
- [ ] Cold-boot test on Samsung A15.
- [ ] Verify absence of "UI ERROR" or visible stutter during the first 10 seconds of app interaction.
