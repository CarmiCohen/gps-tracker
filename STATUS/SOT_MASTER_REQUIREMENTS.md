# SOT Master Requirements & Hardening Status (Oct10.6)

## 🏗️ Architectural Master Rules (175 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.134 Conflation Starvation Protection (R-ID 511-V)**: Signaling dispatchers MUST implement a starvation cap for dynamic conflation windows. (Oct7.3 - Issue #QA-1007-1).
*   **1.135 Unified Diagnostic Snapshots (R-ID 651)**: Diagnostic sensor probes (SNR, Vibration, Thermal, Heap) MUST be grouped into a unified immutable-friendly container (e.g., `ForensicSnapshot`). (Oct7.5 - Issue #SIMP-1007-15).
*   **1.136 Multi-Sensor Native Correlation (R-ID 610)**: High-frequency sensor hot-paths MUST consolidate diverse diagnostic probes into a single JNI batch transaction. (Oct7.6 - Issue #SIMP-1007-16).
*   **1.137 Native Anomaly Propagation (R-ID 612)**: Anomaly flags generated in the native layer MUST be propagated through the `ForensicSnapshot` container. (Oct7.7 - Issue #SIMP-1007-16).
*   **1.138 Memory-Agnostic Polling Stability (R-ID 592)**: The engine MUST force a staggered performance tier when native heap usage exceeds critical thresholds. (Oct7.7 - Issue #SIMP-1007-17).
*   **1.139 Adaptive Acoustic Gating (R-ID 655)**: Environmental acoustic monitoring MUST dynamically adjust its sensitivity based on physical vibration intensity. (Oct7.8 - Issue #SIMP-1010-1).
*   **1.140 Muzzle Hysteresis Native Offloading (R-ID 660)**: Stationary muzzle logic and hysteresis MUST be evaluated in the JNI layer. (Oct7.9 - Issue #SIMP-1010-2).
*   **1.141 SNR Decay Jammer Discrimination (R-ID 670)**: The engine MUST distinguish between mechanical interference and electronic jamming by correlating SNR degradation with vibration in the native layer. (Oct7.10 - Issue #SIMP-1010-3).
*   **1.142 Unified Health Evaluation (R-ID 680)**: Health status evaluation for both GNSS and behavioral anomalies MUST be centralized in the `SentinelValidator`. (Oct8.1 - Issue #SIMP-1007-17).
*   **1.143 Metadata Visibility Guardian (R-ID 1017)**: The `:core:engine` module MUST NOT leak `internal` types into `public` or `protected` signatures. The `:app` module MUST NOT unauthorizedly reference `internal` engine types. Enforced via build-time audit in `build.gradle`. (Oct10.2 - Issue #SIMP-1017-1).
*   **1.144 HUD Interface Property Alignment (R-ID 1010)**: Core state-access interfaces in `:core:engine` (`TimeProvider`, `BootLifecycleAuthority`, `PowerStateProvider`, `NetworkProvider`) and `:app` (`SignalingProvider`) MUST utilize strict `val` properties instead of method getters to ensure Hilt metadata reliability. (Oct10.3 - Issue #SIMP-1010-4).
*   **1.145 Communication & Test Stability (R-ID 1011)**: JSON iteration MUST use explicit casting to `String` for keys and non-null value checks. Test suites MUST strictly follow property-based interface access. (Oct10.4 - Issue #BUILD-FIX-OCT10.3).
*   **1.146 Native GNSS Batching (R-ID 615)**: GNSS health evaluation (satellites used, average SNR) MUST be centralized in the native batching layer (or its Kotlin fallback in `JdHardwareManager`) to decouple the JVM from hardware state logic. (Oct10.5 - Issue #SIMP-1011-1).
*   **1.147 Acoustic Decoupling (R-ID 616)**: Acoustic health evaluation (dB calculation, adaptive alpha, and spike detection) MUST be centralized in `JdHardwareManager` to complete the decoupling of sensor logic from the engine's application layer. (Oct10.6 - Issue #SIMP-1011-2).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 663**: Acoustic Decoupling - Centralized Acoustic health evaluation fallback in `JdHardwareManager.processAcousticBatchNative`. (Oct10.6 - Issue #SIMP-1011-2).
*   **SOT ID 662**: Native GNSS Batching - Centralized GNSS health evaluation in `JdHardwareManager`, eliminating manual fallback logic in `HardwareSuite`. (Oct10.5 - Issue #SIMP-1011-1).
*   **SOT ID 661**: Communication & Test Stability - Resolved JSONObject type ambiguity and remediated property invocation errors in test suites. (Oct10.4 - Issue #BUILD-FIX-OCT10.3).
*   **SOT ID 660**: HUD Interface Alignment - Migrated all core and secondary interfaces to property-based access, ensuring structural symmetry for Compose HUDs. (Oct10.3 - Issue #SIMP-1010-4).
*   **SOT ID 659**: Build-Time Metadata Guardian - Implemented automated cross-module visibility audit in `build.gradle` to prevent KAPT stub generation failures. (Oct10.2 - Issue #SIMP-1017-1).
*   **SOT ID 658**: Unified Health Evaluation - Consolidated redundant GNSS and behavioral health evaluation into `SentinelValidator`. (Oct8.1 - Issue #SIMP-1007-17).
...

---

## 🏁 Verification Chapters
*   **Chapter 31.278 (Acoustic Decoupling Audit)**: PASSED - Verified that `HardwareSuite` no longer contains manual dB/alpha calculation logic and correctly delegates to `JdHardwareManager`. (Oct10.6 - Issue #SIMP-1011-2).
*   **Chapter 31.277 (Native GNSS Batching Audit)**: PASSED - Verified that `HardwareSuite` no longer contains manual SNR/Satellite counting logic and correctly delegates to `JdHardwareManager`. (Oct10.5 - Issue #SIMP-1011-1).
*   **Chapter 31.276 (Communication & Test Audit)**: PASSED - Verified compilation of `CommunicationManager.kt` and all major audit test suites after fixing property invocation and JSON iteration errors. (Oct10.4 - Issue #BUILD-FIX-OCT10.3).
*   **Chapter 31.275 (HUD Interface Alignment Audit)**: PASSED - Verified that `TimeProvider`, `BootLifecycleAuthority`, `PowerStateProvider`, `NetworkProvider`, and `SignalingProvider` use `val` properties. Refactored all calling components to align. (Oct10.3 - Issue #SIMP-1010-4).
*   **Chapter 31.274 (Metadata Visibility Audit)**: PASSED - Verified that the recursive Groovy script in `build.gradle` correctly catalogs `internal` types and prevents leaks. (Oct10.2 - Issue #SIMP-1017-1).
*   **Chapter 31.273 (Unified Health Audit)**: PASSED - Verified delegation to `SentinelValidator.evaluateLocationPendingReason`. (Oct8.1 - Issue #SIMP-1007-17).
