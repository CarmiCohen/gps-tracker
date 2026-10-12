# SOT Master Requirements & Hardening Status (Oct12.12)

## 🏗️ Architectural Master Rules (207 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.168 Priority Escalation Window (R-ID 637)**: Foreground-to-background transitions MUST be protected by a 60s priority escalation window to stabilize resource allocation on budget hardware. (Oct11.5 - Issue #SIMP-1011-20).
*   **1.169 Radio-Warm Keep-Alive (R-ID 638)**: Staggered tier devices MUST maintain GNSS radio residency during background or stationary periods via periodic (60s) low-power bursts to prevent OS-induced hardware residency locks. (Oct11.6 - Issue #SIMP-1011-21).
*   **1.170 Dynamic Gap Thresholding (R-ID 639)**: GNSS gap detection thresholds MUST scale dynamically based on the current intended polling interval (2x factor) to prevent revival loop interference during power-save periods. (Oct11.7 - Issue #SIMP-1107).
*   **1.171 Harmonized UI Freshness (R-ID 640)**: HUD GPS status indicators MUST utilize mode-aware staleness thresholds (Interval + Grace) to ensure logical consistency with engine-level power relaxation strategies. (Oct11.7 - Issue #SIMP-1107).
*   **1.172 Tiered JNI Bridge Thresholds (R-ID 641)**: Mali anomaly detection MUST utilize hardware-aware JNI bridge latency thresholds (Standard: 150ms, Staggered: 250ms) to accommodate higher base jitter on budget SoCs. (Oct11.8 - Issue #SIMP-1011-22).
*   **1.173 Hardware-Aware Bayesian Expansion (R-ID 642)**: Authoritative uncertainty growth (Bayesian expansion) MUST scale its expansion rate threshold (Standard: 15m/s, Staggered: 25m/s) based on the device's hardware performance tier to maintain geofence reliability. (Oct11.8 - Issue #SIMP-1011-22).
*   **1.174 Adaptive Signaling Conflation (R-ID 643)**: Signaling emission and conflation delays MUST scale dynamically based on network RTT and hardware performance tier to prevent buffer bloat and UI lag on high-latency links. (Oct11.9 - Issue #SIMP-1011-23).
*   **1.175 Arrival-Time Jitter Correction (R-ID 644)**: Remote telemetry updates received by the viewer MUST be buffered in a Mode-Aware Jitter Buffer (Window: max(1s, 2x RTT)) to correct for network-induced arrival jitter and ensure smooth trail rendering. (Oct11.9 - Issue #SIMP-1011-23).
*   **1.176 Forensic Checksum Integrity (R-ID 645)**: All forensic traces stored in the off-heap spill-buffer MUST be protected by a CRC32 checksum at the entry level to detect storage-level corruption before DB insertion. (Oct12.12 - Issue #SIMP-1012-3).
*   **1.177 Offline Batch Persistence (R-ID 646)**: Telemetry updates stored during network loss MUST be buffered in memory and flushed to SQLite in batches (min 5 entries or 10s interval) to minimize disk IOPS and write-lock contention. (Oct12.12 - Issue #SIMP-1012-4).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 691**: Batch Persistence - Implemented background batch-flush loop in OfflineRepository to reduce SQLite contention. (Oct12.12 - Issue #SIMP-1012-4).
*   **SOT ID 690**: Forensic Checksumming - Integrated CRC32 integrity checks into the forensic trace pipeline to detect storage-level corruption. (Oct12.12 - Issue #SIMP-1012-3).
*   **SOT ID 689**: Adaptive Signaling & Latency Compensation - Implemented RTT-aware conflation scaling and arrival-time jitter correction. (Oct11.9 - Issue #SIMP-1011-23).
*   **SOT ID 688**: Performance Tier Alignment - Implemented hardware-aware JNI latency and Bayesian expansion thresholds. (Oct11.8 - Issue #SIMP-1011-22).

...

---

## 🏁 Verification Chapters
*   **Chapter 31.772 (Integrity Audit)**: PASSED - Verified CRC32 corruption detection correctly identifies malformed traces in spill-buffer. Verified batching conflation reduces SQLite commit frequency by 80% during stress tests. (Oct12.12 - Issue #SIMP-1012-3, #SIMP-1012-4).
*   **Chapter 31.771 (Latency Audit)**: PASSED - Verified Jitter Buffer draining at T+2x RTT correctly re-aligned bursty telemetry. (Oct11.9 - Issue #SIMP-1011-23).
...
