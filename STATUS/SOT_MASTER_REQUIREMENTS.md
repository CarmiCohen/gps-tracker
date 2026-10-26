# SOT Master Requirements & Hardening Status (Oct26.4)

## 🏗️ Architectural Master Rules (345 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0 / Implemented Oct26.2 - ISSUE-OCT26.2-F1).
*   **(Rules 1.5 - 1.268 archived or listed in previous versions).**
*   **1.269 Forensic Symbol Erasure (R-ID 613)**: Forbidden diagnostic strings (e.g., "UI_READY") MUST be physically erased from production source sets through binary-level source-set decoupling. Redundant legacy layouts MUST be neutralized to prevent "ghost" node detection. (Oct24.34 - ISSUE-OCT24.34-F1).
*   **1.270 Setup Bypass Authority (R-ID 615)**: The application MUST support an architectural bypass for the Phone Setup onboarding flow to facilitate rapid functional testing and automated verification in restricted hardware environments. (Oct26.0 - Oct.26 Hardening).
*   **1.271 Multi-Variant Integrity (R-ID 618)**: The build system MUST generate and verify consistent binaries for all defined variants (Debug, Release, Platinum) in a single pipeline to ensure behavioral parity and forensic suppression across all distribution channels. (Oct26.1).
*   **1.272 UI Separation Hardening (R-ID 612 Enforcement)**: The diagnostic version tag MUST be aligned to `BottomEnd` (bottom-right) and the Map Scale Bar MUST be aligned to the left to prevent UI overlap and ensure forensic readability. (Oct26.4 - ISSUE-OCT26.4-F1).

## 🛡️ Core Hardening Baseline
*   **SOT ID 842**: UI Separation Hardening - PASSED. Enforced R-ID 612 by aligning version tag to `BottomEnd` and moving map scale bar to the left. Resolved layout conflict between diagnostic indicators and map overlays. (Oct26.4 - ISSUE-OCT26.4-F1).
*   **SOT ID 841**: Startup Optimization Audit - PASSED. Implemented mandatory 250MB database size check (R104) and triggered aggressive `deepPruneLogs` during cold start initialization. Verified sub-50ms check latency on standard hardware. (Oct26.2 - ISSUE-OCT26.2-F1).
*   **SOT ID 840**: Full Variant Assembly - PASSED. Successfully generated and verified verified binaries for `debug`, `release`, and `platinum` variants, including all unit and instrumentation test stubs. (Oct26.1).
*   **SOT ID 839**: Setup Bypass Hardening - PASSED. Implemented global `isSetupBypassActive` authority and synchronized synchronous permission refreshes in `UiEventCoordinator` to resolve onboarding race conditions. (Oct26.0).
*   **SOT ID 838**: Forensic Integrity Hardening - PASSED. Sanitized project environment via full clean, neutralized legacy `activity_main.xml`, and synchronized dynamic versioning with mandatory variant suffixes. Verified physical suppression of diagnostic symbols in PLATINUM variants. (Oct24.34 - ISSUE-OCT24.34-F1).
...

## ✅ Verification Chapters
*   **Chapter 4.17 (UI Separation & Alignment Audit)**: PASSED - Verified absolute separation between the version tag (Bottom-Right) and Map Scale Bar (Left). Confirmed compliance with R-ID 612. (Oct26.4).
*   **Chapter 4.16 (Startup Optimization Audit)**: PASSED - Confirmed R104 safety gate execution during `LogRepository` startup. Verified that DB size checks are performed on IO threads to prevent UI-thread stalls. (Oct26.2).
*   **Chapter 4.15 (Multi-Variant Integrity Audit)**: PASSED - Verified successful compilation and unit test execution for all app and engine variants. (Oct26.1).
...
