# Issue #121: Circular Dependency Resolution (LogManager <-> ConnectivitySuite)

## Status: Resolved (July.22.01)
## Requirement: Hilt Universal Authority

### Description
A circular dependency was identified during the Hilt migration: `LogManager` required `ConnectivitySuite` to determine network state for log offloading, while `ConnectivitySuite` required `LogManager` to record connection diagnostic events. This prevented the Hilt dependency graph from compiling.

### Resolution
- **Provider Pattern**: Resolved the cycle by using Dagger's `Provider<ConnectivitySuite>` inside `LogManager`. This allows `LogManager` to be instantiated first and retrieves the `ConnectivitySuite` instance only when needed at runtime.
- **Interface Segregation**: Moved core logging interfaces to a leaf module to further decouple the reporting logic from the connectivity implementation.

### Verification
- [x] Hilt graph compiles successfully.
- [x] Logs are correctly tagged with network state without causing stack overflow or initialization deadlocks.
