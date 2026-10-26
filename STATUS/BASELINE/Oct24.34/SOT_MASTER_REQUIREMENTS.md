# SOT Master Requirements & Hardening Status (Oct24.34)

## 🏗️ Architectural Master Rules (341 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0).
*   **(Rules 1.5 - 1.268 archived or listed in previous versions).**
*   **1.269 Forensic Symbol Erasure (R-ID 613)**: Forbidden diagnostic strings (e.g., "UI_READY") MUST be physically erased from production source sets through binary-level source-set decoupling. Redundant legacy layouts MUST be neutralized to prevent "ghost" node detection. (Oct24.34 - ISSUE-OCT24.34-F1).

## 🛡️ Core Hardening Baseline
*   **SOT ID 838**: Forensic Integrity Hardening - PASSED. Sanitized project environment via full clean, neutralized legacy `activity_main.xml`, and synchronized dynamic versioning with mandatory variant suffixes. Verified physical suppression of diagnostic symbols in PLATINUM variants. (Oct24.34 - ISSUE-OCT24.34-F1).
*   **SOT ID 837**: JNI System Pressure Parity - PASSED. Implemented storage pressure logic in `n24` native function and synchronized shared buffer offsets (32-76). Added NaN sanitization for heap and storage inputs. (Oct24.32 - ISSUE-OCT24.32-F1).
*   **SOT ID 836**: Forensic Integrity Sync - PASSED. Synchronized `versionName` to `Oct24.34` and updated `TEST_PROCEDURE.md`. (Oct24.34 - ISSUE-OCT24.26-F2).
*   **SOT ID 835**: Diagnostic UI Hardening - PASSED. Refined `UI_READY` gate and version suffix logic in `MainAppContent.kt`. Aligned diagnostic indicators to `BottomEnd` to match UX specification and R-ID 612. (Oct24.26 - ISSUE-OCT24.26-F1).
... (archived entries preserved)

## ✅ Verification Chapters
*   **Chapter 4.13 (Forensic Symbol Erasure Audit)**: PASSED - Verified absence of `UI_READY` and legacy layouts in PLATINUM binaries. Confirmed variant integrity via mandatory `_PLATINUM` suffix (R-ID 613). (Oct24.34 - ISSUE-OCT24.34-F1).
*   **Chapter 4.12 (System Pressure Forensic Audit)**: PASSED - Verified JNI parity for memory and storage pressure monitoring and confirmed NaN sanitization (R-ID 640). (Oct24.32 - ISSUE-OCT24.32-F1).
...
