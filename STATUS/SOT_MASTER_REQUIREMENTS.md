# SOT Master Requirements (Sep.01.03)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (35 Rules)

### 1. Lifecycle & Resource Management
...
*   **1.9 IPC Optimization (R759/R876)**: **MANDATORY**. High-frequency lookups of system identifiers (e.g., Package Name, UID) must utilize `GpsApplication` shadow-caches. **Race Condition Hardening**: The `getPackageName()` override MUST query the cache directly rather than via a `lazy` delegate to ensure the shadow-cache is active immediately upon `onCreate()` population, preventing framework-level initialization race conditions on Samsung hardware. (Updated Aug.31.10).
*   **1.10 Low-Memory Eviction (R878)**: **MANDATORY**. All UI-level caches and pools must implement proactive eviction strategies. Map circle geometry MUST utilize an LRU `ShadowCache`. Furthermore, `MapOverlayManager` MUST respond to `ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW` by clearing non-essential caches and pruning pools to active-only sizes. (Added Sep.01.00).
*   **1.11 Zero-Churn Forensic Buffering (R879)**: **MANDATORY**. High-frequency telemetry capture (e.g., 100Hz bursts) MUST utilize zero-churn buffer strategies. The `ForensicSpillBuffer` MUST reuse internal `ByteArray` and `ByteBuffer` wrappers for all read/write operations to prevent heap pollution and GC-induced jitter during rapid restart cycles. (Added Sep.01.02).
...

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758/R776/R777/R874/R875/R877/R880)**: **MANDATORY**. UI hydration and state transitions MUST be segmented and yielded to prevent Main-thread starvation.
    *   **Map Hydration**: Must be segmented into 8 levels with fine-grained yielding (batch size ≤ 5) to ensure no single step exceeds the 700ms Davey threshold.
    *   **Post-Connection Settling (R877)**: Upon relay connection, the `onConnectAction` MUST `yield()` before triggering the telemetry/room-join cascade. Furthermore, the `ConnectivitySuite` MUST implement a 500ms "settling window" before starting the initial offline telemetry sync to avoid colliding with simultaneous map hydration re-renders. (Updated Aug.31.12).
...

*(Total: 35 Architectural Rules + 197 Functional R-IDs = 232 Items)*
