# SOT Master Requirements & Hardening Status (Sep.15.101)

## 🛡️ Core Hardening Baseline
*   **SOT ID 362**: GNSS Revival Resource Leak Remediation - Modified `setSafeMode(active)` in `HardwareSuite.kt` to explicitly unregister `rawRevivalListener` and `revivalCallback`. This ensures that raw GPS and high-accuracy updates are immediately terminated when Safe Mode is engaged, preventing resource leaks and unintended battery drain (R-ID 362). (Resolved Sep.19.02)
*   **SOT ID 361**: Redundant Battery Baseline Capture Remediation - Fully resolved the redundant battery baseline capture logic by initializing `lastFixRt` to `sessionStartRt` inside both `start()` and `resetBaseline()`. This guarantees a proper grace period before any GNSS gap or stall state can be declared, preventing immediate redundant baseline captures upon app activation or reset (R-ID 361). (Resolved Sep.19.03)
*   **SOT ID 360**: GNSS Stall Timing Leakage Remediation - Implemented explicit reset of revival state variables and guarded the background audit loop in `HardwareSuite.kt` with `isStarted.get()`. This prevents `pendingEnterRt` from accumulating stall duration while the suite is inactive, ensuring no immediate hardware locks occur upon activation (R-ID 360). (Resolved Sep.19.02)
*   **SOT ID 359**: Battery Baseline Persistence Hardening - Ensured that `revivalBaselineCaptured` flag in `HardwareSuite.kt` is reset to false within `stop()` and `resetBaseline()`. This prevents lifecycle persistence of the capture state, ensuring new battery baselines are correctly captured after suite restarts or manual resets (R-ID 359). (Resolved Sep.19.01)
*   **SOT ID 358**: Battery Baseline Capture Hardening - Implemented `revivalBaselineCaptured` flag in `HardwareSuite.kt` to ensure a single battery baseline capture per GNSS pending cycle. Prevents premature recapture when intermediate audit events consume the baseline during a sustained stall (R-ID 358). (Resolved Sep.19.00)
*   **SOT ID 357**: Structured Concurrency Burst Hardening - Resolved a coroutine leak in `HardwareSuite.kt` by tracking the 10-second raw GPS revival timeout via `revivalBurstJob`. Ensured immediate cancellation during suite teardown and safe mode transitions (R-ID 357). (Resolved Sep.18.00)

## 📈 Metric Summary
- **Rules Verified**: 72
- **Total SOT IDs**: 362
- **Resolved Issues**: 1108
- **Open Issues**: 1
- **Testing Coverage**: 2 (Sub-items: 10)
- **Simplification Ideas**: 18
- **QA Validation Tasks**: 281

## 🏁 Verification Chapters
*   **Chapter 31.26 (Safe Mode Leakage)**: PASSED - Verified that revival listeners are unregistered when Safe Mode is toggled (Sep.15.101)
*   **Chapter 31.25 (Baseline Capture Gating)**: PASSED - Verified that battery baseline capture is strictly gated by suite active state (Sep.15.101)
*   **Chapter 31.24 (Stall Timing Leakage)**: PASSED - Verified that revival state resets cleanly on stop and audits are gated by suite lifecycle (Sep.15.101)

---
*Next Audit: Sep.20.00. (Sep.15.101)*
