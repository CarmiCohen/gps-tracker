# Project Issues & Hardening Tracking (Rigorous Audit) - Oct10.1

## ? Current Resumption Focus: Strategic Hardening & Architectural Consolidation (Recovery Phase).

## ? Open Gaps & Unfinished Integration Points

### ? High Priority (None)

---

## ? Strategic Simplification Ideas (Ideas: 2)
*   **ID: SIMP-1017-1 [High]**: Implement a "Build-Time Metadata Guardian" script that validates cross-module KAPT/Hilt symbol visibility before allowing a commit. This prevents "Error module" corruption loops by failing early if internal types are leaked or unresolved in stubs.
*   **ID: SIMP-1011-1 [Low]**: Migrate remaining manual GNSS status checks in `HardwareSuite` (like `satellitesUsed` logic) into a native `GnssHealthBatch` to further decouple the JVM from hardware state evaluation.

---

## ? Resolved Traceability & Metadata Issues
*   **Issue #BUILD-RESTORE: KAPT/Hilt Metadata Recovery.** Successfully exited the Oct8.16 "Error module" build loop by rolling back to the Oct8.1 stable baseline (64faffd). Verified build integrity via `:app:assembleDebug`. Resolved Oct10.1.
*   **Issue #SIMP-1007-17: Behavioral Reason Promotion.** Instrumented `LocationSentinel` to promote all behavioral rejections (Acoustic, Jamming, Tamper) into the unified `LocationPendingReason`. Finalized signaling priority resolution in the telemetry pipeline. Resolved Oct8.1.
*   **Issue #SIMP-1007-17: Strategic Simplification.** Consolidated redundant location pending logic between `HardwareSuite` and `SentinelValidator`. Centralized GNSS and behavioral health evaluation in `SentinelValidator`. Resolved Oct7.11.

---

## ? Hardening Progress Dashboard
- **Oct10.1: [SOT Count: 321 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: 65 (Sub-items: 325), QA: 616]**
- **Oct8.1: [SOT Count: 321 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 65 (Sub-items: 325), QA: 616]**
- **Oct7.11: [SOT Count: 319 (Rules: 170), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 64 (Sub-items: 320), QA: 611]**
