# SOT Master Requirements & Hardening Status (Oct10.10)

## 🏗️ Architectural Master Rules (179 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.146 Native GNSS Batching (R-ID 615)**: GNSS health evaluation (satellites used, average SNR) MUST be centralized in the native batching layer (or its Kotlin fallback in `JdHardwareManager`) to decouple the JVM from hardware state logic. (Oct10.5 - Issue #SIMP-1011-1).
*   **1.147 Acoustic Decoupling (R-ID 616)**: Acoustic health evaluation (dB calculation, adaptive alpha, and spike detection) MUST be centralized in `JdHardwareManager` to complete the decoupling of sensor logic from the engine's application layer. (Oct10.6 - Issue #SIMP-1011-2).
*   **1.148 Proximity Decoupling (R-ID 617)**: Proximity debouncing and health index calculation fallback MUST be centralized in `JdHardwareManager` to finalize the decoupling of environmental heuristics from `HardwareSuite.kt`. (Oct10.7 - Issue #SIMP-1011-3).
*   **1.149 Unified Pressure Path (R-ID 618)**: System pressure evaluation (Memory and Storage) MUST be consolidated into a single JNI crossing (`n24`) with native-side hysteresis to ensure high-performance environmental gating. (Oct10.8 - Issue #SIMP-1014-2).
*   **1.150 Temporal HUD Dampening (R-ID 619)**: Reactive UI telemetry flows MUST apply temporal sampling (HUD_STATE_SAMPLE_MS) and debounced persistence (saveLocationUpdateDebounced) to stabilize the HUD and IO layer during high-frequency native sensor bursts. (Oct10.9 - Issue #SIMP-1014-3).
*   **1.151 Signal Health Consistency (R-ID 620)**: Signal health thresholds (SNR) MUST be consistent across Native (JNI), Engine (Sentinel), and Telemetry (Signaling) layers, using raw dB-Hz standards (JUMP_GATE_LOW_SNR_THRESHOLD) and unified scaling (RIBBON_SNR_SCALE_DB) to prevent false-positive multipath rejections. (Oct10.10 - Issue #SIMP-1010-3).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 667**: Signal Decay Alignment - Synchronized SNR thresholds in JNI and remediated telemetry scaling bugs to ensure consistent multipath mitigation. (Oct10.10 - Issue #SIMP-1010-3).
*   **SOT ID 666**: Connectivity Jitter Remediation - Implemented temporal HUD sampling and debounced persistence to stabilize the UI/IO during native sensor floods. (Oct10.9 - Issue #SIMP-1014-3).
*   **SOT ID 665**: Unified Pressure Path - Consolidated Memory/Storage evaluation into JNI `n24` with native hysteresis. (Oct10.8 - Issue #SIMP-1014-2).
...

---

## 🏁 Verification Chapters
*   **Chapter 31.282 (Signal Decay Audit)**: PASSED - Verified that `jdhardware-jni.cpp` uses the 22.0 threshold. Confirmed `TelemetryMapper` correctly reconstructs raw SNR using `RIBBON_SNR_SCALE_DB`. (Oct10.10 - Issue #SIMP-1010-3).
*   **Chapter 31.281 (Connectivity Jitter Audit)**: PASSED - Verified that `MainViewModel` telemetry flows utilize `sample(HUD_STATE_SAMPLE_MS)`. Confirmed `MainRepository` uses async debouncing for `DataStore` writes. (Oct10.9 - Issue #SIMP-1014-3).
...
