# Project Issues & Hardening Tracking (Rigorous Audit) - Oct13.5

## 🎯 Current Resumption Focus: Forensic Integrity (Isolation Leak).

## ⚠️ Open Gaps & Unfinished Integration Points

### 🔴 High Priority (1)
*   **ISSUE-OCT13.5-3: Forensic Test Isolation Leak (High).** `ForensicStressAuditTest` reports 1,099 logs when 1,000 are expected. Likely background recovery tasks (`LogRepository.init`) overlapping with test setup.

### 🟡 Medium Priority (0)

### 🔵 Low Priority (0)

---

## 💡 Strategic Simplification Ideas (Ideas: 17)
*   **SIMP-OCT13.5-S1: Test-Only Persistence Gating (Medium).** Implement a `testMode` flag in `LogRepository` to inhibit automatic background recovery during instrumentation tests to ensure clean buffer states.
*   **SIMP-OCT12.17-S1: Periodic GPS Re-Zeroing (High).** Implement a silent background GPS provider restart every 6 hours if accuracy remains > 30m to clear internal chip thermal drift.
*   **SIMP-OCT12.18-S1: Pruning Scheduler (Medium).** Move background pruning to a dedicated `WorkManager` task to further isolate I/O pressure from the main service process.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT13.5-2: Forensic Bit-Overflow Fix.** Resolved bit-packing collision in `LogRepository.kt` signatures by switching from `Long` packing to `Pair<Long, Int>` composite keys. Resolved Oct13.5.
*   **ISSUE-OCT13.5-1: OfflineRepository Extreme Load Test.** Verified sequential actor stability under 1,000-count concurrent bursts. Zero data loss confirmed via `OfflineRepositorySmokeTest`. Resolved Oct13.5.
*   **ISSUE-OCT13.3-1: OfflineRepository Sequential Actor.** Refactored persistence logic to use a Channel-based actor, resolving high-frequency data loss. Resolved Oct13.3.
*   **ISSUE-OCT13.3-2: ForensicSpillBuffer Thread Isolation.** Hardened `ForensicSpillBuffer.kt` by moving duplication inside locks and using local buffers to prevent corruption. Resolved Oct13.5.

---

## 📈 Hardening Progress Dashboard
- **Oct13.5: [SOT Count: 396 (Rules: 243), Open: H:1, M:0, L:0, Ideas: H:1, M:9, L:7, Testing: 8 (Sub-items: 40), QA: 298]**
- **Oct12.21: [SOT Count: 384 (Rules: 231), Open: H:0, M:0, L:0, Ideas: H:2, M:7, L:7, Testing: 4 (Sub-items: 20), QA: 284]**
