# SOT Master Requirements (Sep.01.16)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (36 Rules)

### 1. Lifecycle & Resource Management
...
*   **1.9 IPC Optimization (R759/R876)**: **MANDATORY**. High-frequency lookups of system identifiers (e.g., Package Name, UID) must utilize `GpsApplication` shadow-caches. **Race Condition Hardening**: The `getPackageName()` override MUST query the cache directly rather than via a `lazy` delegate to ensure the shadow-cache is active immediately upon `onCreate()` population, preventing framework-level initialization race conditions on Samsung hardware. (Updated Aug.31.10).
*   **1.10 Low-Memory Eviction (R878)**: **MANDATORY**. All UI-level caches and pools must implement proactive eviction strategies. Map circle geometry MUST utilize an LRU `ShadowCache`. Furthermore, `MapOverlayManager` MUST respond to `ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW` by clearing non-essential caches and pruning pools to active-only sizes. (Added Sep.01.00).
*   **1.11 Zero-Churn Forensic Buffering (R879)**: **MANDATORY**. High-frequency telemetry capture (e.g., 100Hz bursts) MUST utilize zero-churn buffer strategies. The `ForensicSpillBuffer` MUST reuse internal `ByteArray` and `ByteBuffer` wrappers for all read/write operations to prevent heap pollution and GC-induced jitter during rapid restart cycles. (Added Sep.01.02).
*   **1.12 Hardware Disposal (R887/R888/R889)**: **MANDATORY**. All hardware callbacks (Sensors, GNSS, Network, Display) MUST be unregistered using the `ManagedHardware` synchronization pattern, utilizing the `ManagedUnregistrationHelper` for consistent hardening (R889). To prevent native `BaseEventQueue` leaks, the unregistration (both global and specific) MUST utilize a 4000ms timeout and implement a final direct fallback unregistration on the current thread if the synchronization latch expires. (Updated Sep.01.16).
...

### 2. UI & Performance Authority
...

*(Total: 36 Architectural Rules + 197 Functional R-IDs = 233 Items)*
