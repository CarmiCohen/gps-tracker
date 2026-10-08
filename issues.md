# Project Issues & Hardening Tracking (Rigorous Audit) - Oct8.11

## 🎯 Current Resumption Focus: Strategic Hardening & Architectural Consolidation.

## 🔴 Open Gaps & Unfinished Integration Points

### 🔴 High Priority (None)

---

## 💡 Strategic Simplification Ideas (Ideas: 3)
*   **ID: SIMP-IDEA-1**: Standardize on `inline` callback patterns for all `CircularStateBuffer` retrieval to permanently eliminate `Sequence` and `Iterator` allocations in the high-frequency path. (Significance: Medium).
*   **ID: SIMP-IDEA-2**: Consolidate `MemoryPressureBatch` and `StoragePressureBatch` into a unified `SystemPressureBatch` to reduce JNI overhead and bridge complexity. (Significance: Low).
*   **ID: SIMP-IDEA-3**: Standardize on `@NotNull` native providers in `SentinelValidator` and `HardwareSuite` to eliminate redundant JVM fallback logic once the native library is confirmed loaded. (Significance: Low).

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1013-3: JNI Stationary Authority.** Fully offloaded load-aware movement authority to JNI, eliminating JVM floating-point overhead in the stationary path. Implemented dynamic gate coercion and CPU-load scaling (2.0x) natively in `jdhardware-jni.cpp`. Resolved Oct8.11.
*   **Issue #SIMP-1013-2: Storage Flush Hysteresis.** Migrated authoritative storage pruning triggers to JNI with a 10MB hysteresis window (`STORAGE_HYSTERESIS_OFFSET_MB`). Stabilized IO throughput during boundary oscillations of available space, ensuring authoritative triggers drive aggressive maintenance. Resolved Oct8.11.
*   **Issue #SIMP-1013-1: Memory Pressure Hysteresis.** Offloaded aggressive memory recovery criteria to JNI via `MemoryPressureBatch`. Implemented a 20MB native hysteresis window (`MEMORY_HYSTERESIS_OFFSET_MB`) to prevent "GC Thrashing" and state oscillation at the `CRITICAL` pressure boundary. Resolved Oct8.11.
*   **Issue #SIMP-1012-3: Forensic Stability Audit.** Implemented SNR-based jammer discrimination in `ForensicAuditor`. Leveraged zero-allocation forensic SNR trails to distinguish between active jamming and signal blockage during recovery phases, refining the `LocationPendingReason` authority. Resolved Oct8.11.
*   **Issue #SIMP-1012-2: Native Proximity Scaling.** Migrated environment-aware proximity debouncing and index calculation to JNI via `ProximityBatch`. Centralized proximity health authority, ensuring `HardwareSuite` strictly follows native decisions for stationary duration and thermal load scaling. Resolved Oct8.11.
*   **Issue #SIMP-1012-1: Forensic Retrieval Optimization.** Refactored `CircularStateBuffer` and `HardwareSuite` to use `inline` callback-based iteration (`forEachMatch`, `forEachSnrSample`, etc.) for high-frequency telemetry retrieval. Achieved zero-allocation parity (R-ID 392) by eliminating `Sequence` and `Iterator` overhead. Resolved Oct8.11.
*   **Issue #SIMP-1011-3: Forensic Buffer Consolidation.** Consolidated `EngineSnrSample`, `EngineAcousticSample`, and `EngineSensorSnapshot` into a single `ForensicSample` container. Reduced memory fragmentation and allocation pressure by using a unified circular buffer (`forensicBuffer`) for all high-frequency telemetry data. Resolved Oct8.11.
*   **Issue #SIMP-1011-2: Acoustic JNI Offloading.** Migrated `AudioRecord` iterative math (RMS and Peak) to JNI via `AcousticBatch`. This reduces JVM mathematical overhead and interrupt frequency during 44.1kHz audio monitoring. Resolved Oct8.11.
*   **Issue #SIMP-1011-1: Native GNSS Batching.** Migrated satellite status evaluation and SNR averaging to JNI via `GnssHealthBatch` to minimize JVM overhead and ensure deterministic hardware state evaluation. Resolved Oct8.11.
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.** Instrumented `LocationProcessor` and `MonitorService` to ensure behavioral rejections (Acoustic, Jamming, Tamper) are promoted into the `LocationPendingReason` and propagated through the telemetry pipeline. Resolved Oct8.11.

---

## 📊 Hardening Progress Dashboard
- **Oct8.11: [SOT Count: 331 (Rules: 178), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:2, Testing: 74 (Sub-items: 370), QA: 661]**
- **Oct8.11: [SOT Count: 329 (Rules: 176), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 72 (Sub-items: 360), QA: 651]**
- **Oct8.11: [SOT Count: 328 (Rules: 175), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 71 (Sub-items: 355), QA: 646]**
- **Oct8.11: [SOT Count: 327 (Rules: 174), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:0, Testing: 70 (Sub-items: 350), QA: 641]**
- **Oct8.11: [SOT Count: 325 (Rules: 172), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 68 (Sub-items: 340), QA: 631]**
- **Oct8.11: [SOT Count: 323 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 67 (Sub-items: 335), QA: 626]**
- **Oct8.11: [SOT Count: 322 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 66 (Sub-items: 330), QA: 621]**
- **Oct8.11: [SOT Count: 321 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 65 (Sub-items: 325), QA: 616]**
- **Oct8.11: [SOT Count: 319 (Rules: 170), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 64 (Sub-items: 320), QA: 611]**
- **Oct8.11: [SOT Count: 318 (Rules: 169), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 63 (Sub-items: 315), QA: 606]**
- **Oct8.11: [SOT Count: 317 (Rules: 168), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 62 (Sub-items: 310), QA: 601]**
- **Oct8.11: [SOT Count: 316 (Rules: 167), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 61 (Sub-items: 305), QA: 596]**
