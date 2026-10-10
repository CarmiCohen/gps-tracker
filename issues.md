# Project Issues & Hardening Tracking (Rigorous Audit) - Oct10.9

## 🎯 Current Resumption Focus: Signal Decay Audit.

## 🔴 Open Gaps & Unfinished Integration Points

### 🟡 Medium Priority (0)
*   *No open medium priority issues.*

---

## 💡 Strategic Simplification Ideas (Ideas: 0)
*   *No active simplification ideas.*

---

## 🟢 Resolved Traceability & Metadata Issues
*   **Issue #SIMP-1014-3: Connectivity Jitter.** Remediated state jitter in the Compose HUD and IO layer caused by high-frequency native JNI telemetry bursts. Implemented `saveLocationUpdateDebounced` in `MainRepository` to cap persistence IO at 1Hz. Applied 200ms temporal sampling (`HUD_STATE_SAMPLE_MS`) to telemetry and signaling flows in `MainViewModel`. Integrated flyweight duplication to maintain state integrity during asynchronous debouncing. Resolved Oct10.9.
*   **Issue #SIMP-1014-2: JNI Consolidation.** Finalized native system pressure evaluation (Memory/Storage) with hysteresis in `n24`. Consolidated GNSS (`n21`), Acoustic (`n22`), and Proximity (`n23`) JNI paths to replace Kotlin fallbacks. Increased shared state buffer to 2048 bytes for multi-sensor safety. Optimized forensic capture for zero-allocation throughput. Resolved Oct10.8.
*   **Issue #SIMP-1011-3: Proximity Decoupling.** Centralized proximity debouncing and index calculation fallback in `JdHardwareManager.processProximityBatchNative`. Finalized decoupling of environmental heuristics from `HardwareSuite.kt`. Resolved Oct10.7.
*   **Issue #SIMP-1011-2: Acoustic Decoupling.** Centralized Acoustic health evaluation fallback (dB calculation, adaptive alpha, and spike detection) in `JdHardwareManager.processAcousticBatchNative`. Removed manual sensor math from `HardwareSuite.kt`, delegating all environment heuristics to the native/JNI path with a robust Kotlin fallback. Resolved Oct10.6.
*   **Issue #SIMP-1011-1: Native GNSS Batching.** Centralized GNSS health evaluation (satellite counts and average SNR) in `JdHardwareManager.processGnssBatchNative`. Removed manual fallback logic from `HardwareSuite.kt`. Resolved Oct10.5.
*   **Issue #BUILD-FIX-OCT10.3: Communication & Test Remediation.** Fixed `JSONObject` iteration type mismatches in `CommunicationManager.kt` and remediated property invocation errors in `ProductionReadinessAuditTest`, `GeofenceBatteryAuditTest`, and `ForensicStressAuditTest`. Resolved Oct10.4.
*   **Issue #SIMP-1010-4: HUD Interface Alignment.** Migrated all core and secondary service interfaces in `:core:engine` to strict `val` properties for state access. Resolved Oct10.3.
*   **Issue #SIMP-1017-1: Build-Time Metadata Guardian.** Implemented a module-wide audit script in `build.gradle` that catalogs `internal` types in `:core:engine`. Resolved Oct10.2.
*   **Issue #BUILD-RESTORE: KAPT/Hilt Metadata Recovery.** Successfully exited the Oct8.16 "Error module" build loop. Resolved Oct10.1.

---

## 📊 Hardening Progress Dashboard
- **Oct10.9: [SOT Count: 328 (Rules: 178), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 68 (Sub-items: 340), QA: 700]**
- **Oct10.8: [SOT Count: 327 (Rules: 177), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 68 (Sub-items: 340), QA: 686]**
- **Oct10.7: [SOT Count: 326 (Rules: 176), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:0, Testing: 68 (Sub-items: 340), QA: 672]**
- **Oct10.6: [SOT Count: 325 (Rules: 175), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 68 (Sub-items: 340), QA: 658]**
- **Oct10.5: [SOT Count: 324 (Rules: 174), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 68 (Sub-items: 340), QA: 644]**
- **Oct10.4: [SOT Count: 323 (Rules: 173), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 68 (Sub-items: 340), QA: 630]**
- **Oct10.3: [SOT Count: 322 (Rules: 172), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:1, Testing: 65 (Sub-items: 325), QA: 616]**
- **Oct10.2: [SOT Count: 322 (Rules: 172), Open: H:0, M:1, L:0, Ideas: H:0, M:0, L:1, Testing: 65 (Sub-items: 325), QA: 616]**
- **Oct10.1: [SOT Count: 321 (Rules: 171), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: 65 (Sub-items: 325), QA: 616]**
