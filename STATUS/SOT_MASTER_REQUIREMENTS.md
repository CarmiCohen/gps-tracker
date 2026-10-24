# SOT Master Requirements & Hardening Status (Oct24.34)

## 🏗️ Architectural Master Rules (340 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0).
*   **(Rules 1.5 - 1.262 archived or listed in previous versions).**
*   **1.263 Connectivity Callback Idempotency (R-ID 632)**: All system network callback registrations MUST be guarded by process-level idempotency checks and executed on the Main thread to prevent diagnostic `StackLog` traces and redundant system overhead. (Oct24.18 - ISSUE-OCT24.14-F2).
*   **1.264 Startup Frame Optimization (R-ID 633)**: The application MUST defer non-critical Main-thread observations and utilize granular UI state collection to ensure startup frame deadlines stay below 500ms (preventing Davey violations). (Oct24.20 - ISSUE-OCT24.14-F3).
*   **1.265 Debug Artifact Suppression (R-ID 610)**: All diagnostic status labels (e.g., `UI_READY`) MUST be explicitly suppressed in production (PLATINUM) builds to prevent forensic leaks. (Oct24.22 - ISSUE-OCT24.14-F4).
*   **1.266 Dynamic Version Alignment (R-ID 611)**: All test suites verifying version-dependent fields MUST dynamically reference `BuildConfig.VERSION_NAME` to prevent regression and alignment failures during version increments. (OctX - ISSUE-OCT24.22-F1).
*   **1.267 Diagnostic UI Hardening (R-ID 612)**: The diagnostic version and build variant indicators MUST be aligned to the bottom-right (BottomEnd) of the screen and utilize conditional suffix logic to prevent overlap and ensure clarity in both debug and production environments. (Oct24.26 - ISSUE-OCT24.26-F1).
*   **1.268 JNI System Pressure Parity (R-ID 640)**: The native layer MUST maintain full functional parity with JVM fallback logic for memory and storage pressure evaluation, including hysteresis and NaN sanitization, to ensure consistent forensic log pruning (R104). (Oct24.32 - ISSUE-OCT24.32-F1).

## 🛡️ Core Hardening Baseline
*   **SOT ID 837**: JNI System Pressure Parity - PASSED. Implemented storage pressure logic in `n24` native function and synchronized shared buffer offsets (32-76). Added NaN sanitization for heap and storage inputs. (Oct24.32 - ISSUE-OCT24.32-F1).
*   **SOT ID 836**: Forensic Integrity Sync - PASSED. Synchronized `versionName` to `Oct24.34` and updated `TEST_PROCEDURE.md`. (Oct24.34 - ISSUE-OCT24.26-F2).
*   **SOT ID 835**: Diagnostic UI Hardening - PASSED. Refined `UI_READY` gate and version suffix logic in `MainAppContent.kt`. Aligned diagnostic indicators to `BottomEnd` to match UX specification and R-ID 612. (Oct24.26 - ISSUE-OCT24.26-F1).
*   **SOT ID 834**: Dynamic Version Alignment - PASSED. Implemented dynamic version referencing in `MainViewModelMappingTest` using `BuildConfig.VERSION_NAME` to eliminate hardcoded version dependencies. (OctX - ISSUE-OCT24.22-F1).
*   **SOT ID 833**: Debug Artifact Suppression - PASSED. Hardened diagnostic label suppression by introducing `IS_PLATINUM` build config flag and wrapping `UI_READY` in a conditional check. (Oct24.22 - ISSUE-OCT24.14-F4).
*   **SOT ID 832**: Startup Performance Hardening - PASSED. Optimized startup frame deadlines by decoupling monolithic UI state collection and deferring heavy Main-thread observations. (Oct24.20 - ISSUE-OCT24.14-F3).
*   **SOT ID 831**: StackLog Leak Remediation - PASSED. Hardened `AndroidNetworkProvider` with process-level idempotency guards and Main-thread registration to prevent platform diagnostic `StackLog` traces on Samsung A15 devices. (Oct24.18 - ISSUE-OCT24.14-F2).
*   **SOT ID 830**: Settings Validation Centralization - PASSED. Relocated identity uniqueness and regex validation logic to `ConfigValidator` in `:core:engine`. Standardized error messages and improved testability. (Oct24.16 - SIMP-OCT22.0-S1).
*   **(Archived SOT IDs 801-826 listed in historical baselines).**

## ✅ Verification Chapters
*   **Chapter 4.12 (System Pressure Forensic Audit)**: PASSED - Verified JNI parity for memory and storage pressure monitoring and confirmed NaN sanitization (R-ID 640). (Oct24.32 - ISSUE-OCT24.32-F1).
*   **Chapter 4.11 (Diagnostic UI Alignment Audit)**: PASSED - Verified alignment of diagnostic indicators to BottomEnd and validated conditional suffix logic (R-ID 612). (Oct24.26 - ISSUE-OCT24.26-F1).
*   **Chapter 4.10 (Version Alignment Resilience Audit)**: PASSED - Verified that unit tests dynamically track version changes without manual code updates (R-ID 611). (OctX - ISSUE-OCT24.22-F1).
*   **Chapter 4.9 (Diagnostic Suppression Audit)**: PASSED - Verified suppression of `UI_READY` label in PLATINUM build variants (R-ID 610). (Oct24.22 - ISSUE-OCT24.14-F4).
*   **Chapter 4.8 (Startup Performance Audit)**: PASSED - Verified startup frame deadlines and confirmed absence of Davey violations through granular state collection and deferred observations. (Oct24.20 - ISSUE-OCT24.14-F3).
*   **Chapter 4.7 (Connectivity Forensic Audit)**: PASSED - Verified zero `StackLog` traces during network transitions and confirmed idempotent registration behavior in `AndroidNetworkProvider`. (Oct24.18 - ISSUE-OCT24.14-F2).
*   **Chapter 4.6 (Configuration Validation Audit)**: PASSED - Verified centralized validation rules and error message consistency in `ConfigValidator`. (Oct24.16 - SIMP-OCT22.0-S1).
*   **Chapter 4.5 (Version Lifecycle Audit)**: PASSED - Verified version alignment across `app/build.gradle`, `BuildConfig`, and unit tests. (Oct24.14 - ISSUE-OCT24.14-F1).
