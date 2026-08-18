# Simplify & Optimization Ideas (Aug.18.01)

## 1. Forensic Pipeline Efficiency
- **Off-Heap Pooling**: The `LogRepository.performForensicDrain` currently creates a new `ArrayList` and multiple `LogEntity` objects per drain. Using a reusable pool of entities could reduce GC pressure during sustained 100Hz sampling.
- **Direct ByteBuffer to Room**: Explore if `LogDao` can accept raw `ByteBuffer` chunks or if a custom SQLite extension could handle the forensic binary format directly to bypass the JVM heap entirely.

## 2. Configuration Centralization
- **Threshold Consolidation**: Move logging-related constants from `EngineConstants.kt` into a dedicated `LoggingConfig.kt` or a dynamic `RemoteConfig` to allow tuning without full rebuilds.
