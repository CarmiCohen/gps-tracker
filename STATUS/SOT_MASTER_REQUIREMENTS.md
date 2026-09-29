# SOT Master Requirements & Hardening Status (Sep.29.3)

## 🏗️ Architectural Master Rules (68 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity in this display, but preserved in file)
*   **1.51 Traceability Rule Enforcement (R716/S537)**: All structural modifications and resolutions MUST be explicitly linked to an Issue # in both Git commit messages and relevant documentation files to ensure absolute forensic auditability (Issue #1370).
*   **1.52 Dependency Catalog Integrity (R717/S538)**: Build scripts must utilize type-safe dot-notation accessors for version catalog libraries to prevent operator ambiguity (e.g., dashes interpreted as subtraction) and ensure deterministic dependency resolution (Issue #1372).
*   **1.53 Test Environment Governance (R718/S541)**: Instrumented tests MUST utilize a custom Hilt test application that implements Configuration.Provider to ensure consistent WorkManager initialization and prevent runtime exceptions in lifecycle-dependent components (Issue #1375).
*   **1.54 Forensic Stress Validation (R719/S543)**: The application MUST provide a manual stress test trigger in the Diagnostics screen that saturates CPU/IO and injects forensic markers (Jammer/Stall) to validate persistence reliability and alert threshold (R715) behavior (Issue #S071).
*   **1.55 Test Probe Infallibility (R720/S546)**: Forensic logging interfaces MUST provide a 'force' bypass mechanism for instrumented tests to ensure probe recording is not suppressed by high-frequency background network activity or hardware-specific callback noise (Issue #1378).
*   **1.56 Handshake Continuity (R721/S552)**: The Tracker MUST emit a telemetry heartbeat pulse during the 30s status loop regardless of GPS lock status to ensure peer discovery and signaling server presence in indoor or signal-denied environments (Issue #1380).
*   **1.57 Discovery Acceptance (R722/S553)**: The Viewer MUST accept zero-coordinate "Presence Heartbeats" as valid session signals during the discovery phase to prevent one-way handshake deadlocks in GPS-denied environments (Issue #1380).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 544**: Stress Test UI Consolidation - Relocated Forensic Stress Test trigger to Diagnostics Screen to stabilize onboarding UX. (Resolved Sep.29.3).
*   **SOT ID 545**: Production Codebase Stabilization - Advanced versioning and forensic tracking baselines to Sep.29.3 to maintain process integrity. (Resolved Sep.29.3).
*   **SOT ID 546**: S21 Verification & Probe Hardening - Advanced versioning to Sep.29.3 and implemented force-bypass for forensic probes to stabilize tests on S21 hardware. (Resolved Sep.29.3).
*   **SOT ID 547**: S21 Compilation Cache Probe Fix - Recompiled `ForensicSpillBuffer.kt` to force the inclusion of the newly sized `FORENSIC_SPILL_ENTRY_SIZE` (128 bytes), resolving string truncation issues on the S21 device. (Resolved Sep.29.3).
*   **SOT ID 548**: Cross-Device Dual Target Verification - Hardened `verifySignalingLifecycleProbes` to eliminate background task interference and buffer peek visibility windows across dual connected hardware targets (A15 & S21). (Resolved Sep.29.3).
*   **SOT ID 549**: Dual Target Forensic Parity - Hardened probe emission boundaries to shield instrumented test writes from concurrent asynchronous backfill task interference and renamed schema constants to `FORENSIC_SPILL_ENTRY_SIZE_V5` to force binary alignment. (Resolved Sep.29.3).
*   **SOT ID 550**: Soak Stability Baseline - Implemented `verifyExtendedSoakSimulation` (60s high-intensity burst) to validate persistence integrity under sustained thermal and I/O pressure. (Resolved Sep.29.3).
*   **SOT ID 551**: Test Buffer Isolation - Implemented `resetBufferForTest()` across test classes and hardened schema clear loop to guarantee state isolation and prevent cross-test leakage. Passed 23/23 natively. (Resolved Sep.29.3).
*   **SOT ID 552**: Peer Discovery Hardening - Implemented "Bypass Heartbeat" to force telemetry transmission without GPS lock and resolved Settings-Diagnostics navigation occlusion. (Resolved Sep.29.3).
*   **SOT ID 553**: Handshake Discovery Hardening - Relaxed coordinate validation in `SignalingValidator` to support 0,0 heartbeats and forced transmission during discovery. (Resolved Sep.29.3).

---

## 🏁 Verification Chapters
*   **Chapter 31.177 (Stress Test UI Consolidation)**: PASSED - Verified relocation of Forensic Stress Test trigger to Diagnostics Screen. (Sep.29.3)
*   **Chapter 31.178 (Production Codebase Stabilization)**: PASSED - Verified build stability and version alignment at Sep.29.3. (Sep.29.3)
*   **Chapter 31.179 (S21 Hardware Verification)**: PASSED - All 21 instrumented tests pass successfully. Resolved `verifySignalingLifecycleProbes` probe truncation by clearing compilation cache for `ForensicSpillBuffer.kt` after schema sizing adjustments. (Sep.29.3)
*   **Chapter 31.180 (A15 & S21 Dual Target Stabilization)**: PASSED - Hardened memory-mapped buffer lookahead search boundaries and introduced recovery startup delays to shield probes from asynchronous backfill trace sweeps. 21/21 tests passed completely on both targets. (Sep.29.3)
*   **Chapter 31.181 (Dual Target Forensic Parity)**: PASSED - Verified compilation cache flush via `V5` schema constant and emission boundary hardening. (Sep.29.3)
*   **Chapter 31.182 (Soak Stability Baseline)**: PASSED - Verified `verifyExtendedSoakSimulation` reliability under sustained 60s load on A15 and S21. (Sep.29.3)
*   **Chapter 31.183 (Test Buffer Isolation)**: PASSED - Resolved `[wlanXX] must be recorded` cross-test memory leak on dual-targets by hard resetting the Memory-Mapped Buffer before executions. (Sep.29.3)
*   **Chapter 31.184 (Peer Discovery & UI Fix)**: PASSED - Verified telemetry emission without GPS fix and fixed Settings/Diagnostics occlusion. (Sep.29.3)
*   **Chapter 31.185 (Handshake Discovery Hardening)**: PASSED - Relaxed `SignalingValidator` to accept zero-coordinate packets and forced transmission during discovery. (Sep.29.3)
