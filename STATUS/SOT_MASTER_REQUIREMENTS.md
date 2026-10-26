# SOT Master Requirements & Hardening Status (Oct26.0)

## 🏗️ Architectural Master Rules (342 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0).
*   **(Rules 1.5 - 1.268 archived or listed in previous versions).**
*   **1.269 Forensic Symbol Erasure (R-ID 613)**: Forbidden diagnostic strings (e.g., "UI_READY") MUST be physically erased from production source sets through binary-level source-set decoupling. Redundant legacy layouts MUST be neutralized to prevent "ghost" node detection. (Oct24.34 - ISSUE-OCT24.34-F1).
*   **1.270 Setup Bypass Authority (R-ID 615)**: The application MUST support an architectural bypass for the Phone Setup onboarding flow to facilitate rapid functional testing and automated verification in restricted hardware environments. (Oct26.0 - Oct.26 Hardening).

## 🛡️ Core Hardening Baseline
*   **SOT ID 839**: Setup Bypass Hardening - PASSED. Implemented global `isSetupBypassActive` authority and synchronized synchronous permission refreshes in `UiEventCoordinator` to resolve onboarding race conditions. (Oct26.0).
*   **SOT ID 838**: Forensic Integrity Hardening - PASSED. Sanitized project environment via full clean, neutralized legacy `activity_main.xml`, and synchronized dynamic versioning with mandatory variant suffixes. Verified physical suppression of diagnostic symbols in PLATINUM variants. (Oct24.34 - ISSUE-OCT24.34-F1).
*   **SOT ID 837**: JNI System Pressure Parity - PASSED. Implemented storage pressure logic in `n24` native function and synchronized shared buffer offsets (32-76). Added NaN sanitization for heap and storage inputs. (Oct24.32 - ISSUE-OCT24.32-F1).
...

## ✅ Verification Chapters
*   **Chapter 4.14 (Functional Testing Bypass Audit)**: PASSED - Verified immediate access to Tracker Dashboard via `isSetupBypassActive` and confirmed resolution of Background Disclosure loops (R-ID 615). (Oct26.0).
*   **Chapter 4.13 (Forensic Symbol Erasure Audit)**: PASSED - Verified absence of `UI_READY` and legacy layouts in PLATINUM binaries. Confirmed variant integrity via mandatory `_PLATINUM` suffix (R-ID 613). (Oct24.34 - ISSUE-OCT24.34-F1).
...
