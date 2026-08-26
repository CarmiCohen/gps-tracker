# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 719**

## 113. Hardware SOT Architectural Decoupling (Aug.25.05)
*   **Issue #317: Hardware SOT Architectural Decoupling**.
    - **Resolution**: Migrated hardware detection signatures from `:app:Utils.kt` to `:core:engine:HardwareSot.kt` (R313/R212).
    - **Action**: Established `HardwareSot` object in the engine module as the central authority for environment identification. Refactored `SystemStatusProviderImpl.kt`, `TrackerService.kt`, and `ViewerService.kt` to consume this decoupled source directly.
    - **Result**: Core engine and background services are now "Hardware Neutral" and independently aware of their execution environment, eliminating architectural leaks and dependency on application-layer utilities for critical gating logic.

## 112. Hardware Detection SOT & Deployment Hardening (Aug.25.04)
*   **Issue #313: Multi-Device Deployment Failure**.
    - **Resolution**: Unified and hardened hardware detection signatures (R313).
    - **Action**: Consolidated detection logic for Samsung and budget A15 variants using `MODEL`, `PRODUCT`, and `DEVICE` strings. 
    - **Result**: Ensures consistent A15-specific optimizations (R314/R312) across all system layers.

## 111. Shadow-Cache LRU & Forensic Hardening (Aug.25.03)
*   **Issue #316: Shadow-Cache LRU Documentation Gap (#721)**.
    - **Resolution**: Formalized R280 logic for `ShadowCache` and verified LRU eviction strategy.
    - **Action**: Verified `ShadowCache.kt` implementation of `accessOrder = true`. Added `testLruEvictionOrder` to `ShadowCacheTest.kt`.
    - **Result**: Shadow-Cache stability is now formally documented and verified.

## 110. GPS Stabilization & Warm-up Hardening (Aug.25.01)
*   **Issue #315: Immediate Signal Loss False Positive**.
    - **Resolution**: Implemented GPS_WARMUP_GRACE_MS (30s) in `MainAlarmLogic` (R315).

## 109. Startup Fluidity & Budget Hardware Hardening (Aug.25.00)
*   **Issue #314: Startup UI Stall (Davey)**.
    - **Resolution**: Implemented Staggered Hydration (R314).

*(Older resolutions preserved in Git history)*
