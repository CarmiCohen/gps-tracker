# SOT Master Requirements & Hardening Status (Oct8.15)

## 🏗️ Architectural Master Rules (180 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.148 Memory Pressure Hysteresis (R-ID 686)**: Aggressive memory recovery criteria (GC flushing) MUST be offloaded to JNI via `MemoryPressureBatch`. Decision logic MUST incorporate a native hysteresis window (`MEMORY_HYSTERESIS_OFFSET_MB`) to prevent "GC Thrashing" and rapid state oscillations when the system heap operates at the boundary of `CRITICAL` pressure. (Oct8.15 - Issue #SIMP-1013-1).
*   **1.149 Storage Pressure Hysteresis (R-ID 687)**: Storage-aware pruning and flushing triggers MUST be offloaded to JNI via `StoragePressureBatch`. Decision logic MUST incorporate a native hysteresis window (`STORAGE_HYSTERESIS_OFFSET_MB`) to stabilize pressure state transitions (`NORMAL`, `LOW`, `CRITICAL`) and prevent redundant "IO Thrashing" near boundary conditions. (Oct8.15 - Issue #SIMP-1013-2).
*   **1.150 JNI Stationary Authority (R-ID 688)**: Authoritative stationary state evaluation MUST be fully encapsulated in JNI. JVM-side load-aware scaling and threshold calculations are prohibited; the `SentinelValidator` MUST delegate to the native provider to ensure deterministic movement gates and zero-allocation floating-point math in the 100Hz path. (Oct8.15 - Issue #SIMP-1013-3).
*   **1.151 Unified Pressure Path (R-ID 689)**: All system resource pressure evaluation (Memory and Storage) MUST be consolidated into a single atomic JNI crossing via `SystemPressureBatch` (n26). This reduces JVM-to-Native context switching overhead and ensures synchronous health state updates during high-frequency telemetry cycles. (Oct8.15 - Issue #SIMP-1014-2).
*   **1.152 Non-Nullable Native Authority (R-ID 690)**: The `SentinelValidator` MUST utilize a non-nullable `NativeFastPathProvider` to eliminate branching and null-checks in high-frequency sensor paths. Redundant JVM math fallbacks MUST be encapsulated within a `DefaultNativeFastPathProvider` and injected at the engine level to ensure architectural parity regardless of JNI availability. (Oct8.15 - Issue #SIMP-IDEA-3).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 668**: Non-Nullable Native Authority - Standardized on `@NotNull` providers to eliminate redundant JVM fallback logic in high-frequency paths. (Oct8.15 - Issue #SIMP-IDEA-3).
*   **SOT ID 667**: Unified Pressure Path - Consolidated Memory and Storage evaluation into a single atomic JNI crossing. (Oct8.15 - Issue #SIMP-1014-2).
*   **SOT ID 666**: JNI Stationary Authority - Fully offloaded load-aware movement authority to JNI, eliminating JVM floating-point overhead in the stationary path. (Oct8.15 - Issue #SIMP-1013-3).

---

## Verification Chapters
*   **Chapter 31.283 (Native Authority Parity)**: PASSED - Verified that `SentinelValidator` uses a non-nullable provider. Confirmed that `DefaultNativeFastPathProvider` correctly implements JVM fallbacks using `EngineConstants`, eliminating code duplication and branching in hot paths. (Oct8.15 - Issue #SIMP-IDEA-3).
*   **Chapter 31.282 (Unified Pressure Audit)**: PASSED - Verified that `IntegrityMonitor` uses the atomic `processSystemPressure` path. Confirmed JNI overhead reduction by 50% for pressure monitoring. (Oct8.15 - Issue #SIMP-1014-2).
