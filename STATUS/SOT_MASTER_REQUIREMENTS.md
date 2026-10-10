# SOT Master Requirements & Hardening Status (Oct10.8)

## 🏗️ Architectural Master Rules (177 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.146 Native GNSS Batching (R-ID 615)**: GNSS health evaluation (satellites used, average SNR) MUST be centralized in the native batching layer (or its Kotlin fallback in `JdHardwareManager`) to decouple the JVM from hardware state logic. (Oct10.5 - Issue #SIMP-1011-1).
*   **1.147 Acoustic Decoupling (R-ID 616)**: Acoustic health evaluation (dB calculation, adaptive alpha, and spike detection) MUST be centralized in `JdHardwareManager` to complete the decoupling of sensor logic from the engine's application layer. (Oct10.6 - Issue #SIMP-1011-2).
*   **1.148 Proximity Decoupling (R-ID 617)**: Proximity debouncing and health index calculation fallback MUST be centralized in `JdHardwareManager` to finalize the decoupling of environmental heuristics from `HardwareSuite.kt`. (Oct10.7 - Issue #SIMP-1011-3).
*   **1.149 Unified Pressure Path (R-ID 618)**: System pressure evaluation (Memory and Storage) MUST be consolidated into a single JNI crossing (`n24`) with native-side hysteresis to ensure high-performance environmental gating. (Oct10.8 - Issue #SIMP-1014-2).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 665**: Unified Pressure Path - Consolidated Memory/Storage evaluation into JNI `n24` with native hysteresis. (Oct10.8 - Issue #SIMP-1014-2).
*   **SOT ID 664**: Proximity Decoupling - Centralized proximity debouncing and index calculation fallback in `JdHardwareManager.processProximityBatchNative`. (Oct10.7 - Issue #SIMP-1011-3).
*   **SOT ID 663**: Acoustic Decoupling - Centralized Acoustic health evaluation fallback in `JdHardwareManager.processAcousticBatchNative`. (Oct10.6 - Issue #SIMP-1011-2).
...

---

## 🏁 Verification Chapters
*   **Chapter 31.280 (Unified Pressure Audit)**: PASSED - Verified that `IntegrityMonitor` utilizes the consolidated `processSystemPressure` JNI path. Native hysteresis stability confirmed. (Oct10.8 - Issue #SIMP-1014-2).
*   **Chapter 31.279 (Proximity Decoupling Audit)**: PASSED - Verified that `HardwareSuite` delegates proximity health logic to `JdHardwareManager` and no longer contains manual debouncing math. (Oct10.7 - Issue #SIMP-1011-3).
...
