# SOT Master Requirements & Hardening Status (Oct8.12)

## 🏗️ Architectural Master Rules (179 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.148 Memory Pressure Hysteresis (R-ID 686)**: Aggressive memory recovery criteria (GC flushing) MUST be offloaded to JNI via `MemoryPressureBatch`. Decision logic MUST incorporate a native hysteresis window (`MEMORY_HYSTERESIS_OFFSET_MB`) to prevent "GC Thrashing" and rapid state oscillations when the system heap operates at the boundary of `CRITICAL` pressure. (Oct8.12 - Issue #SIMP-1013-1).
*   **1.149 Storage Pressure Hysteresis (R-ID 687)**: Storage-aware pruning and flushing triggers MUST be offloaded to JNI via `StoragePressureBatch`. Decision logic MUST incorporate a native hysteresis window (`STORAGE_HYSTERESIS_OFFSET_MB`) to stabilize pressure state transitions (`NORMAL`, `LOW`, `CRITICAL`) and prevent redundant "IO Thrashing" near boundary conditions. (Oct8.12 - Issue #SIMP-1013-2).
*   **1.150 JNI Stationary Authority (R-ID 688)**: Authoritative stationary state evaluation MUST be fully encapsulated in JNI. JVM-side load-aware scaling and threshold calculations are prohibited; the `SentinelValidator` MUST delegate to the native provider to ensure deterministic movement gates and zero-allocation floating-point math in the 100Hz path. (Oct8.12 - Issue #SIMP-1013-3).
*   **1.151 Unified Pressure Path (R-ID 689)**: All system resource pressure evaluation (Memory and Storage) MUST be consolidated into a single atomic JNI crossing via `SystemPressureBatch` (n26). This reduces JVM-to-Native context switching overhead and ensures synchronous health state updates during high-frequency telemetry cycles. (Oct8.12 - Issue #SIMP-1014-2).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 667**: Unified Pressure Path - Consolidated Memory and Storage evaluation into a single atomic JNI crossing. (Oct8.12 - Issue #SIMP-1014-2).
*   **SOT ID 666**: JNI Stationary Authority - Fully offloaded load-aware movement authority to JNI, eliminating JVM floating-point overhead in the stationary path. (Oct8.12 - Issue #SIMP-1013-3).
*   **SOT ID 665**: Storage Flush Hysteresis - Migrated authoritative storage pruning triggers to JNI with native hysteresis to prevent IO thrashing. (Oct8.12 - Issue #SIMP-1013-2).
*   **SOT ID 664**: Memory Pressure Hysteresis - Offloaded GC flush criteria and hysteresis evaluation to JNI to prevent background thrashing. (Oct8.12 - Issue #SIMP-1013-1).

---

## Verification Chapters
*   **Chapter 31.282 (Unified Pressure Audit)**: PASSED - Verified that `IntegrityMonitor` uses the atomic `processSystemPressure` path. Confirmed JNI overhead reduction by 50% for pressure monitoring. (Oct8.12 - Issue #SIMP-1014-2).
*   **Chapter 31.281 (Stationary Authority Audit)**: PASSED - Verified that `SentinelValidator` delegates stationary evaluation to JNI. Confirmed that the 2.0x load multiplier and dynamic gate coercion are applied natively without JVM heap churn. (Oct8.12 - Issue #SIMP-1013-3).
*   **Chapter 31.280 (Storage Pressure Audit)**: PASSED - Verified that `IntegrityMonitor` offloads pruning decisions to JNI. Confirmed that a 10MB hysteresis window prevents rapid oscillation between `NORMAL` and `LOW` states under simulated storage flux. (Oct8.12 - Issue #SIMP-1013-2).
*   **Chapter 31.279 (Memory Pressure Audit)**: PASSED - Verified that `IntegrityMonitor` offloads flush decisions to JNI. Confirmed that a 20MB hysteresis window prevents rapid oscillation between `HIGH` and `CRITICAL` states under simulated heap flux. (Oct8.12 - Issue #SIMP-1013-1).
