# Simplicity Ideas 2 (Hardening Phase)

1.  **Peer Mapping**: Simplify ID matching logic by using a unified `SignalingIdentity` object instead of separate tracker/viewer ID checks.
2.  **State Consolidation**: Move `trackerGpsStallStartTs` and similar transient peer states from `ConnectivitySuite` to `RemoteStatusRepository`.
3.  **Heartbeat Uniformity**: Standardize `TICK_INTERVAL_MS` across all service loops to reduce timing drift in uptime calculations.
4.  **Log Throttling**: Implement a generic `ThrottledForensicLogger` to replace manual timestamp checks for signaling drops.
5.  **Status Atomic Updates**: Flatten the `TrackerStatus` copy chain to reduce allocation pressure during high-frequency updates.
6.  **Unified Power Consolidation**: Move Doze-state listeners from services into `UnifiedPowerPolicy`.
7.  **Signaling Interface**: Remove legacy JSON emit methods in favor of the unified `transmit(TrackerStatus)` for all telemetry.
8.  **Offline Buffer Chunking**: Unify pruning logic across all Room-backed repositories.
9.  **Jitter Logic**: Centralize random jitter calculation to ensure uniform distribution across all backoff implementations.
10. **Context Shadowing**: Remove remaining manual package name references now that application-level shadowing is active.
11. **Signaling Priority**: Simplify `SignalingPriority` to a boolean `isCritical` to reduce branching logic.
12. **Telemetry Mapping**: Automate `PendingStatusEntity` mapping using a lightweight reflection-free transformer.
13. **Doze awareness**: Integrate `PowerManager.isDeviceIdleMode()` directly into the `SignalingProvider` transmission gate.
14. **Metric Aggregation**: Simplify `SessionManager.updateTick` by using a dedicated `UptimeTracker` component.
15. **Batch Sizing**: Move dynamic batch size calculations from `ConnectivitySuite` to `EngineConstants`.
16. **Forensic Aggression**: Simplify signaling delay scaling by using a linear interpolation based on violation intensity.
17. **Forensic Audit Provider**: Decouple violation uptime tracking into a dedicated component to reduce SessionManager complexity.
18. **Version Management**: Move `safeVersionName` and `safeVersionCode` logic from `app/build.gradle` to a separate `versioning.gradle` plugin to simplify the primary build script.
19. **Capability Consolidation**: Merge redundant `HardwareCapabilities` booleans (`isStaggeredTier`, `requiresAdaptationMuzzle`, `useStaggeredHydration`) into a single `PerformanceTier` enum to simplify behavioral branching logic. (COMPLETED - Sep.16.02)
