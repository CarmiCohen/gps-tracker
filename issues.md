# Project Issues & Hardening Tracking (Aug.20.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 HEALTHY | 0 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 663 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #212-C2 (Final Forensic Conclusion)**: Samsung CFMS Trigger is a **Resilient Static Heuristic**.
    *   **Evidence**: `libmbrainSDK` load attempts persist despite JNI suppression, metadata rephrasing, and identity swaps.
    *   **Implication**: The trigger is likely embedded in APK resource signatures or internal class structure profile matching on the SM-A155F. Accepted as a benign vendor side-effect.

---

## 🔴 Open Issues
*   *(No active critical issues)*

---

## 🟢 Recently Resolved Issues (Aug.20.00)
*   **Issue #219: Analytical Index Performance Verification**: Offloaded the `GpsIndex` calculation in `GpsStatusManager.kt` to `Dispatchers.Default` and implemented a 500ms `sample` throttle. This ensures that weighted averaging of GPS Age, Accuracy, and Satellite count does not induce UI thread jitter during 100Hz forensic bursts (R219).
*   **Issue #217: Shadow-Cache Hardening**: Finalized the LRU-based `ShadowCache` in `core:engine`. Hardened thread-safety for atomic `getOrPut` operations to ensure stability during high-frequency forensic bursts. Integrated into `GpsApplication` and `MainRepository` (R217).
*   **Issue #218: Systematic JNI Audit**: Audited the native C++ layer in `src/main/cpp`. Verified that all internal identifiers, logic, and logs are fully decoupled from neutralized vendor keywords. Exported JNI functions now strictly utilize abstract identifiers (`n1`-`n5`). (R218)
*   **Issue #216: Atomic Counter Consolidation**: Simplified `MainRepository.kt` by grouping disparate `AtomicInteger` counters into a single `RepositoryMetrics` data structure (R216).
*   **Issue #215: Integrity Monitor Flow Audit**: Synchronized `IntegrityMonitor` vital-flow thresholds with the new R213 limits. Increased `locationStalled` check to 180s (R215).
*   **Issue #213: Signal Loss False-Positive**: Remediated logical lock in `GpsManager.kt` and performed threshold hardening in `EngineConstants.kt` (R213).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.20.00)
