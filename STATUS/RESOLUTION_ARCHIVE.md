# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 708**

## 103. MbrainSDK Integration & Ghost Load Neutralization (Aug.22.08)
*   **Issue #251: Integration Failure (mbrainSDK)**.
    - **Resolution**: Identified the `Can't load libmbrainSDK` Logcat error as a benign "Ghost Load" triggered by Samsung's CFMS (Configurable Floating Management Service) on A15 hardware. The OS attempts to load legacy vendor libraries when detecting specific JNI direct-buffer patterns. 
    - **Action**: Confirmed the R212 Identity Swap (mbrainSDK -> jdHardware) is fully implemented. Added forensic diagnostic logs to `JdHardwareManager` and updated `DEVICE_SPECIFIC_ADAPTATIONS.md` to document the heuristic.
    - **Verification**: Verified native bridge functionality via `punchHardware` and `syncState` audit.

## 102. Storage Pressure Hardening & prioritization (Aug.22.05)
*   **Audit Chapter 12.3: Sustained Storage Pressure**.
    - **Resolution**: Implemented storage simulation hooks in `IntegrityMonitor` and `CommandRouter`. Connected these to `DiagnosticsScreen` to verify `PersistencePolicy` prioritization logic. Verified that normal logs/trails are gated while `isSpecial` forensic data persists under critical pressure (R197).
    - **Verification**: Verified via `StoragePressureAuditTest` (7 tests passing).

## 101. Shadow-Cache Hardening & Stress Stability (Aug.22.04)
*   **Issue #280: Shadow-Cache LRU Race Condition**.
    - **Resolution**: Refactored `ShadowCache` in `core:engine` to use `ReentrantLock` instead of intrinsic synchronization. Optimized the underlying `LinkedHashMap` initial capacity to prevent structural re-hashing stalls. This eliminates race conditions during the 100Hz saturation bursts required for Chapter 12.2 compliance (R280).
    - **Verification**: Verified via `ShadowCacheTest` and `ForensicStressAuditTest`.
*   **Issue #140/12.2**: **Database Stress Audit (100Hz)**. Restored stress hooks and verified `ForensicSpillBuffer` stability. Resolved build blockers in `Models.kt` and `ViewerScreen.kt` related to unified telemetry naming.
*   **Issue #308**: **Restored Core Engine Definitions**. Re-implemented `AlarmEvaluationState`, `ProcessedLocation`, `SpatialAnchor`, and `RejectedPoint` in `EngineModels.kt`, unblocking the build and verifying Chapter 11.2 tests.

## 100. Offline Storage Hardening & R197 Compliance (Aug.22.03)
*   **Issue #197: Offline Storage Hardening**.
    - **Resolution**: Aligned `PendingStatusDao` and `OfflineRepository` with R197 chunked pruning standards. Replaced monolithic `prune()` with `getPruneThreshold` and `pruneByThreshold` calls.
    - **Verification**: Verified logic via source audit. Prevents I/O stalls during large accumulation recovery on budget hardware (Samsung A15).

*(Older resolutions preserved in Git history)*
