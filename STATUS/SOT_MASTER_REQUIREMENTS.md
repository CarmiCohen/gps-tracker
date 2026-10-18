# SOT Master Requirements & Hardening Status (Oct18.0)

## 🏗️ Architectural Master Rules (286 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.211 Alignment-Safe JNI Ingestion (R-ID 729)**: Shared memory ingestion in JNI (e.g., `ByteBuffer` mapped pointers) MUST use `std::memcpy` for multi-byte primitives to ensure stability on MediaTek/ARMv8 SoCs sensitive to alignment violations. (Oct14.1 - ISSUE-OCT17.4-F1).
*   **1.212 Release Integrity Verification (R-ID 730)**: Every deployment MUST undergo a manual version string verification to ensure build/engine/UI synchronization. (Oct14.1 - ISSUE-OCT15.6-T1).
*   **1.213 Automated Version Synchronization (R-ID 731)**: Version naming MUST be centralized in `gradle.properties` and injected via `BuildConfig` to all modules and UI layers to prevent manual synchronization drift. (Oct14.1 - SIMP-OCT15.6-S1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 761**: JNI GNSS Alignment Hardening - PASSED. Re-aligned `GnssHealthBatch` to 8-byte boundaries and migrated to `memcpy` in `jdhardware-jni.cpp`. Resolved `NaNG` regression. (Oct18.0 - ISSUE-OCT16.9-F2).
*   **SOT ID 760**: System Audit & Procedure Synchronization - PASSED. Synchronized TEST_PROCEDURE.md to 100 chapters and verified all logic for Oct14.1. (Oct14.1 - TEST-OCT17.11-T2).
*   **SOT ID 759**: Instrumentation Test Isolation - PASSED. Inhibited background drainer during wrap-around stability tests to prevent race conditions. (Oct14.1 - ISSUE-OCT17.11-T2).
*   **SOT ID 758**: Build Dependency Hardening - PASSED. Corrected Compose BOM platform alignment for AndroidTest. (Oct14.1 - FIX-OCT17.11-D1).
*   **SOT ID 757**: Automated Version Synchronization - PASSED. Centralized versioning in `gradle.properties` and refactored UI/Engine to use `BuildConfig.VERSION_NAME`. (Oct14.1 - TEST-OCT17.10-1).
*   **SOT ID 756**: Version Name Verification - PASSED. Implemented formal version verification step in Chapter 1.3 of the Test Procedure. (Oct14.1 - TEST-OCT17.10-1).
*   **SOT ID 734**: JNI Alignment Hardening - PASSED. Replaced direct pointer casting with `memcpy` for all 8-byte primitives in `jdhardware-jni.cpp`, resolving `NaNG` telemetry on Samsung A15. (Oct14.1 - ISSUE-OCT17.4-F1).

...

---

## ✅ Verification Chapters (vOct18.0)
*   **Chapter 1 (Launch)**: PASSED - Version "Oct18.0" verified; permissions validated. (Oct18.0).
*   **Chapter 5 (GNSS)**: PASSED - Re-aligned batch processing verified under I/O pressure. (Oct18.0 - ISSUE-OCT16.9-F2).
*   **Chapter 6 (Stress)**: PASSED - Forensic markers latched correctly in Log Overlay. (Oct14.1).
*   **Chapter 7 (Integrity)**: PASSED - Automated 250Hz audit confirms zero CME. (Oct14.1).
*   **Chapter 12 (Storage)**: PASSED - Pruning engine manages 100Hz pressure cycle. (Oct14.1).
*   **Chapter 17 (Export)**: PASSED - ForensicSpillBuffer successfully prepared share payload. (Oct14.1).
*   **Chapters 31-100 (Hardening)**: PASSED - 24h soak simulation counter stability verified. (Oct14.1).
