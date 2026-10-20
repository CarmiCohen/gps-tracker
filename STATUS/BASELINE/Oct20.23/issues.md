# Project Issues & Hardening Tracking (Rigorous Audit) - Oct20.23

## 🎯 Current Resumption Focus: Forensic Remediation of Connectivity & Storage Leaks.

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT20.23-F1: Redundant Connectivity Callback Leaks (High).** Forensic failure. `AndroidNetworkProvider` emits `StackLog` entries during high-frequency hydration or test cycles, indicating redundant `ConnectivityManager.registerNetworkCallback` calls at line 88. Violates R-ID 582 (Resource Lifecycle).
*   **ISSUE-OCT20.23-F2: Storage Pressure Audit Flakiness (High).** Stability failure. `StoragePressureAuditTest` exhibits non-deterministic behavior; failures in `verifyCriticalStoragePrioritization` and `verifyLowStoragePrioritization` were observed in initial runs but cleared upon retry. Indicates unresolved race conditions in the manual batch processor.

---

## 💡 Strategic Simplification Ideas (Ideas: 56)
*   **SIMP-OCT20.23-S1: Declarative Callback Lifecycle (Medium).** Consider refactoring `AndroidNetworkProvider` to use a single `StateFlow` backed by a `ManagedNetworkCallback` that is strictly bound to the `externalScope`, ensuring zero redundant registrations during concurrent access.
*   **SIMP-OCT20.22-S1: Automated Version Injector for Tests (Low).** Consider using `BuildConfig` values directly in unit tests to avoid manual version string updates during baseline synchronization.
... [Content truncated for brevity in baseline record] ...
