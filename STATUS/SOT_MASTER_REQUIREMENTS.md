# System Source of Truth (SoT) - Aug.21.06 (Forensic Hardening Release)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Forensic Range-Deduplication Authority (R197)**: (Added Aug.21.06) The system MUST utilize range-based signature queries (`getExistingForensicSignaturesInRange`) during forensic drains. This optimization restricts deduplication lookups to the specific time window of the current batch, eliminating full-history table scans and reducing CPU/Memory pressure on budget hardware (SM-A155F) during 100Hz bursts. (Issue #196). **Status: Implemented.**
*   **Forensic Overflow Hysteresis (R196)**: (Added Aug.21.06) The `LogManager` MUST implement hysteresis for buffer overflow alerts. The overflow state MUST only be reset when the off-heap `ForensicSpillBuffer` fill level drops below 50%. This prevents alert oscillation and notification spam during periods of sustained high-frequency sampling. (Issue #196). **Status: Implemented.**
*   **UI Thread Authority (R246)**: (Updated Aug.21.04) The system MUST consolidate UI hydration sequences into no more than 3 broad phases on budget hardware (SM-A155F) to minimize recomposition overhead and prevent Davey stalls (>700ms). Heavy system state observation (e.g., permission refreshes) MUST be throttled to no more than 1Hz during setup and 0.2Hz during background execution. (Issue #246). **Status: Implemented.**
*   **Stationary Anchor Hardening (R238)**: (Added Aug.20.10) The system MUST prevent stationary anchors from "chasing" GPS drift. Coordinate-averaging convergence MUST be restricted to points within the 50% "dead zone" of the current breakout threshold. (Issue #238). **Status: Implemented.**
*   **HUD State Centralization (R226)**: (Updated Aug.20.10) The system MUST utilize a unified `HudState` data class for all UI status indicators. (Issue #226/240). **Status: Implemented.**
*   **Telemetry Mapping Consolidation (R225)**: (Added Aug.20.07) The system MUST utilize a unified `ForensicMapper` for all 1:1 parity transformations. (Issue #225). **Status: Implemented.**
*   **Coordinate Stabilization Authority (R224)**: (Updated Aug.20.06) Following signal loss, the EMA filter MUST utilize a weighted transition window. (Issue #224). **Status: Implemented.**
*   **Production Release Hardening (R223)**: (Added Aug.20.03) System MUST be stripped of all debug instrumentation/thermal simulation prior to release. (Issue #223). **Status: Implemented.**
*   **Analytical Index Performance (R219)**: (Added Aug.20.00) GpsIndex calculation MUST be offloaded to `Dispatchers.Default` with 500ms sampling. (Issue #219). **Status: Implemented.**
*   **Shadow-Cache Hardening (R217)**: (Updated Aug.20.00) System MUST utilize LRU eviction for all shadow-caches to prevent unbounded memory growth. (Issue #217). **Status: Implemented.**
*   **JNI Vendor Collision Remediation (R212)**: (Added Aug.19.01) System MUST NOT utilize proprietary vendor keywords in JNI identifiers. Hardware bridge MUST use `JdHardware` namespace. (Issue #212). **Status: Implemented.**
*   **Samsung Battery Authority (R405)**: (Added Aug.19.01) On SM-A155F, system MUST detect/enforce battery optimization exemption. (Issue #214). **Status: Implemented.**

### 2. UI & Map Authority
*   **Sensitivity Slider Authority (R246-S)**: (Added Aug.21.04) Sensor sensitivity calibration MUST utilize a unified `SensitivitySlider` component to ensure consistent UX and reduced layout churn in Alert Management. (Issue #246). **Status: Implemented.**
*   **Sensor Sensitivity Authority (R247)**: (Added Aug.21.01) The system MUST provide manual calibration controls (0-100%) for Vibration and Tilt sensors. (Issue #247). **Status: Implemented.**
*   **UI Hardening: Button Clipping (R232)**: (Added Aug.20.08) System MUST use `heightIn(min=...)` and `13.sp` fonts for setup action buttons. (Issue #232). **Status: Implemented.**

### 3. Forensic Persistence Authority
*   **Zero-Loss Spill Buffer (R200)**: Memory-mapped circular buffer for forensic telemetry. (Issue #196).
*   **CRC32 Integrity Authority (R201)**: Every forensic entry MUST include a CRC32 checksum. (Issue #203).
