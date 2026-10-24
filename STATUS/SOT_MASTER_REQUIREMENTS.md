# SOT Master Requirements & Hardening Status (Oct24.20)

## 🏗️ Architectural Master Rules (337 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0).
*   **(Rules 1.5 - 1.262 archived or listed in previous versions).**
*   **1.263 Connectivity Callback Idempotency (R-ID 632)**: All system network callback registrations MUST be guarded by process-level idempotency checks and executed on the Main thread to prevent diagnostic `StackLog` traces and redundant system overhead. (Oct24.18 - ISSUE-OCT24.14-F2).
*   **1.264 Startup Frame Optimization (R-ID 633)**: The application MUST defer non-critical Main-thread observations and utilize granular UI state collection to ensure startup frame deadlines stay below 500ms (preventing Davey violations). (Oct24.20 - ISSUE-OCT24.14-F3).

## 🛡️ Core Hardening Baseline
*   **SOT ID 832**: Startup Performance Hardening - PASSED. Optimized startup frame deadlines by decoupling monolithic UI state collection and deferring heavy Main-thread observations. (Oct24.20 - ISSUE-OCT24.14-F3).
*   **SOT ID 831**: StackLog Leak Remediation - PASSED. Hardened `AndroidNetworkProvider` with process-level idempotency guards and Main-thread registration to prevent platform diagnostic `StackLog` traces on Samsung A15 devices. (Oct24.18 - ISSUE-OCT24.14-F2).
*   **SOT ID 830**: Settings Validation Centralization - PASSED. Relocated identity uniqueness and regex validation logic to `ConfigValidator` in `:core:engine`. Standardized error messages and improved testability. (Oct24.16 - SIMP-OCT22.0-S1).
*   **(Archived SOT IDs 801-826 listed in historical baselines).**

## ✅ Verification Chapters
*   **Chapter 4.8 (Startup Performance Audit)**: PASSED - Verified startup frame deadlines and confirmed absence of Davey violations through granular state collection and deferred observations. (Oct24.20 - ISSUE-OCT24.14-F3).
*   **Chapter 4.7 (Connectivity Forensic Audit)**: PASSED - Verified zero `StackLog` traces during network transitions and confirmed idempotent registration behavior in `AndroidNetworkProvider`. (Oct24.18 - ISSUE-OCT24.14-F2).
*   **Chapter 4.6 (Configuration Validation Audit)**: PASSED - Verified centralized validation rules and error message consistency in `ConfigValidator`. (Oct24.16 - SIMP-OCT22.0-S1).
*   **Chapter 4.5 (Version Lifecycle Audit)**: PASSED - Verified version alignment across `app/build.gradle`, `BuildConfig`, and unit tests. (Oct24.14 - ISSUE-OCT24.14-F1).
*   **Chapter 4.4 (Native Throttling Stress Audit)**: PASSED - Verified propagation of native load-shedding signals under memory pressure after fixing native build configuration. (Oct24.12 - ISSUE-OCT24.10-F1).
*   **Chapter 4.3 (Offline Synchronization Audit)**: PASSED - Verified persistent buffering and delivery of violation events after network recovery. (Oct24.2 - ISSUE-OCT23.101-F6).
