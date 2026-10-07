# SOT Master Requirements & Hardening Status (Oct7.6)

## 🏗️ Architectural Master Rules (164 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.134 Conflation Starvation Protection (R-ID 511-V)**: Signaling dispatchers MUST implement a starvation cap for dynamic conflation windows. The transmission deadline MUST be calculated relative to the arrival of the FIRST message in a burst to ensure a deterministic maximum latency (e.g., 2000ms) regardless of subsequent burst density. (Oct7.3 - Issue #QA-1007-1).
*   **1.135 Unified Diagnostic Snapshots (R-ID 651)**: Diagnostic sensor probes (SNR, Vibration, Thermal, Heap) MUST be grouped into a unified immutable-friendly container (e.g., `ForensicSnapshot`) across all domain and UI models. This ensures atomic updates, simplifies state duplication (`duplicate()`), and reduces delegation boilerplate in the telemetry monolith. (Oct7.5 - Issue #SIMP-1007-15).
*   **1.136 Multi-Sensor Native Correlation (R-ID 610)**: High-frequency sensor hot-paths (100Hz+) MUST consolidate diverse diagnostic probes (Vibration, SNR, Thermal) into a single JNI batch transaction. Native logic SHOULD leverage these correlated signals for advanced anomaly detection while avoiding redundant JVM-to-OS system calls via metric caching. (Oct7.6 - Issue #SIMP-1007-16).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 652**: Multi-Sensor Native Correlation - Expanded JNI FastPath batching to include unified forensic snapshots (SNR, Thermal, Heap) for native-layer state evaluation. (Oct7.6 - Issue #SIMP-1007-16).
*   **SOT ID 651**: Unified Snapshot Container - Migrated all engine and app-level diagnostic probes into a grouped `ForensicSnapshot` container for architectural parity. (Oct7.5 - Issue #SIMP-1007-15).
*   **SOT ID 650**: Forensic Telemetry Expansion - Promoted internal engine flags (muzzled, siren, hardware health, environmental lockouts) to Protobuf for remote diagnostics. (Oct7.3 - Issue #QA-1007-1).

---

## 🏁 Verification Chapters
*   **Chapter 31.268 (Native Correlation Audit)**: PASSED - Verified that `VibrationBatch` correctly carries correlated SNR and Thermal snapshots into the native layer. Verified that thermal and heap probes use a 2-second caching interval to protect the 100Hz path from system call overhead. (Oct7.6 - Issue #SIMP-1007-16).
*   **Chapter 31.267 (Forensic Container Parity Audit)**: PASSED - Verified that `EngineConnectionPoint`, `ConnectionPoint`, and `LogEntry` all delegate correctly to the unified `ForensicSnapshot`. (Oct7.5 - Issue #SIMP-1007-15).
