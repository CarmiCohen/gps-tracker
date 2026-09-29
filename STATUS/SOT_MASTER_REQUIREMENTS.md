# SOT Master Requirements & Hardening Status (Sep.29.3)

## 🏗️ Architectural Master Rules (66 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.51 Traceability Rule Enforcement (R716/S537)**: All structural modifications and resolutions MUST be explicitly linked to an Issue # in both Git commit messages and relevant documentation files to ensure absolute forensic auditability (Issue #1370).
*   **1.52 Dependency Catalog Integrity (R717/S538)**: Build scripts must utilize type-safe dot-notation accessors for version catalog libraries to prevent operator ambiguity (e.g., dashes interpreted as subtraction) and ensure deterministic dependency resolution (Issue #1372).
*   **1.53 Test Environment Governance (R718/S541)**: Instrumented tests MUST utilize a custom Hilt test application that implements Configuration.Provider to ensure consistent WorkManager initialization and prevent runtime exceptions in lifecycle-dependent components (Issue #1375).
*   **1.54 Forensic Stress Validation (R719/S543)**: The application MUST provide a manual stress test trigger in the Diagnostics screen that saturates CPU/IO and injects forensic markers (Jammer/Stall) to validate persistence reliability and alert threshold (R715) behavior (Issue #S071).
*   **1.55 Test Probe Infallibility (R720/S546)**: Forensic logging interfaces MUST provide a 'force' bypass mechanism for instrumented tests to ensure probe recording is not suppressed by high-frequency background network activity or hardware-specific callback noise (Issue #1378).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 544**: Stress Test UI Consolidation - Relocated Forensic Stress Test trigger to Diagnostics Screen to stabilize onboarding UX. (Resolved Sep.29.3).
*   **SOT ID 545**: Production Codebase Stabilization - Advanced versioning and forensic tracking baselines to Sep.29.3 to maintain process integrity. (Resolved Sep.29.3).
*   **SOT ID 546**: S21 Verification & Probe Hardening - Advanced versioning to Sep.29.3 and implemented force-bypass for forensic probes to stabilize tests on S21 hardware. (Resolved Sep.29.3).
*   **SOT ID 547**: S21 Compilation Cache Probe Fix - Recompiled `ForensicSpillBuffer.kt` to force the inclusion of the newly sized `FORENSIC_SPILL_ENTRY_SIZE` (128 bytes), resolving string truncation issues on the S21 device. All 21 instrumented tests passing. (Resolved Sep.29.3).
*   **SOT ID 548**: Cross-Device Dual Target Verification - Hardened `verifySignalingLifecycleProbes` to eliminate background task interference and buffer peek visibility windows across dual connected hardware targets (A15 & S21). (Resolved Sep.29.3).

---

## 🏁 Verification Chapters
*   **Chapter 31.177 (Stress Test UI Consolidation)**: PASSED - Verified relocation of Forensic Stress Test trigger to Diagnostics Screen. (Sep.29.3)
*   **Chapter 31.178 (Production Codebase Stabilization)**: PASSED - Verified build stability and version alignment at Sep.29.3. (Sep.29.3)
*   **Chapter 31.179 (S21 Hardware Verification)**: PASSED - All 21 instrumented tests pass successfully. Resolved `verifySignalingLifecycleProbes` probe truncation by clearing compilation cache for `ForensicSpillBuffer.kt` after schema sizing adjustments. (Sep.29.3)
*   **Chapter 31.180 (A15 & S21 Dual Target Stabilization)**: PASSED - Hardened memory-mapped buffer lookahead search boundaries and introduced recovery startup delays to shield probes from asynchronous backfill trace sweeps. 21/21 tests passed completely on both targets. (Sep.29.3)
...