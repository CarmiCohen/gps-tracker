# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 618**

## 51. Log Infrastructure Hardening (Aug.14.07)
*   **Issue #177: Startup ANR & Heap Exhaustion**.
    - **Resolution**: Hardened the logging infrastructure to prevent main-thread stalls during high-frequency (100Hz) telemetry flow. Reduced reactive log limits to 2,000 (Standard) and 5,000 (Strict) entries. Implemented pruning support for the "Important" log category to eliminate a 100,000+ row database leak. Optimized `performForensicDrain` by tightening the signature lookback window to 1 hour, preventing OOM/GC-thrashing during deep recovery. (R177)
*   **Issue #176: Proactive Pruning ANR**.
    - **Resolution**: Optimized the database schema with composite indices on `(type, timestamp)` and `(isImportant, isSpecial, timestamp)` to eliminate full table scans during pruning. Refactored `LogRepository` to utilize transactional chunking (500-1000 rows) and offloaded all bulk mapping to `Dispatchers.Default`, eliminating 2.2s I/O stalls. (R176)

## 50. Forensic Mirror Parity Audit (Aug.14.06)
*   **Issue #172: Viewer-Side LocationProcessor State Audit**.
    - **Resolution**: Finalized full forensic SIT state parity in the viewer-side mirrored state. Enhanced `LocationProcessor` and `ViewerService` to correctly restore forensic attributes including vertical velocity timestamps (`sitVzTs`, `sitVzRt`), displacement (`sitDz`), barometric delta (`sitBaro`), tilt (`sitTilt`), and peak shock (`sitShock`) from remote telemetry. This ensures high-fidelity mirroring and "Zero-Lag" UI transitions after service restarts or handovers. (R172)

## 49. Multi-Stream Processor Contention (Aug.14.04)
*   **Issue #173: Multi-Stream Processor Contention**.
    - **Resolution**: Hardened `ViewerService` by decoupling "Self" and "Remote" location streams. Instantiated two distinct `LocationProcessor` instances to prevent filter state corruption (velocity EMA, jump detection scores) caused by interleaved coordinate streams. (R173)

## 48. Forensic Replay Latency Audit (Aug.14.05)
*   **Issue #174: Forensic Replay Latency Audit**.
    - **Resolution**: Optimized replay scrubbing performance for high-frequency (100Hz) telemetry sets. Increased trail and ribbon history limits to 10,000 points. Implemented $O(\log N)$ binary search for coordinate matching in `StateSubscriptionUseCase` and cursor positioning in `SharedUiComponents`. Hardened `MainViewModel` to use `collectLatest` for scrubbing events, eliminating coroutine churn. Verified sub-16ms latency for 10-minute forensic traces. (R174)

## 47. Forensic Multi-Stream Jitter Audit (Aug.14.03)
*   **Issue #171: Forensic Multi-Stream Jitter Audit**.
    - **Resolution**: Hardened the forensic telemetry pipeline against non-monotonic packet arrival (jitter) caused by multi-viewer streams or network delays. Relaxed `RemoteStatusRepository` to allow a 2s jitter window (`MONOTONIC_JITTER_TOLERANCE_MS`) to prevent forensic data loss. Implemented a monotonicity guard in `TelemetryAggregator` to ensure aggregators don't regress. Hardened `StateSubscriptionUseCase` to perform sorted-merging and deduplication of history buffers for stable UI ribbon visualization. Verified via artificial jitter simulation (200-800ms) in `CommunicationManager`. (R171)

... (rest of archive)
