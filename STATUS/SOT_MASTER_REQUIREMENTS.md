# SOT Master Requirements & Hardening Status (Oct17.11)

## 🏗️ Architectural Master Rules (286 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.211 Alignment-Safe JNI Ingestion (R-ID 729)**: Shared memory ingestion in JNI (e.g., `ByteBuffer` mapped pointers) MUST use `std::memcpy` for multi-byte primitives to ensure stability on MediaTek/ARMv8 SoCs sensitive to alignment violations. (Oct14.1 - ISSUE-OCT17.4-F1).
*   **1.212 Release Integrity Verification (R-ID 730)**: Every deployment MUST undergo a manual version string verification to ensure build/engine/UI synchronization. (Oct14.1 - ISSUE-OCT15.6-T1).
*   **1.213 Automated Version Synchronization (R-ID 731)**: Version naming MUST be centralized in `gradle.properties` and injected via `BuildConfig` to all modules and UI layers to prevent manual synchronization drift. (Oct14.1 - SIMP-OCT15.6-S1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 759**: Instrumentation Test Isolation - PASSED. Inhibited background drainer during wrap-around stability tests to prevent race conditions. (Oct17.11 - ISSUE-OCT17.11-T2).
*   **SOT ID 758**: Build Dependency Hardening - PASSED. Corrected Compose BOM platform alignment for AndroidTest. (Oct17.11 - FIX-OCT17.11-D1).
*   **SOT ID 757**: Automated Version Synchronization - PASSED. Centralized versioning in `gradle.properties` and refactored UI/Engine to use `BuildConfig.VERSION_NAME`. (Oct14.1 - TEST-OCT17.10-1).
*   **SOT ID 756**: Version Name Verification - PASSED. Implemented formal version verification step in Chapter 1.3 of the Test Procedure. (Oct14.1 - TEST-OCT17.10-1).
*   **SOT ID 734**: JNI Alignment Hardening - PASSED. Replaced direct pointer casting with `memcpy` for all 8-byte primitives in `jdhardware-jni.cpp`, resolving `NaNG` telemetry on Samsung A15. (Oct14.1 - ISSUE-OCT17.4-F1).

...

---

## ✅ Verification Chapters
*   **Chapter 1.3 (Version Verification)**: PASSED - Verified "Oct17.11_PLATINUM" on Landing Screen of A15. (Oct17.11).
*   **Chapter 1.4 (Permission Onboarding)**: PASSED - Verified all critical permission gates on A15 hardware. (Oct17.11).
*   **Chapter 7.3 (Engine Thread Safety)**: PASSED - Verified zero ConcurrentModificationException during 250Hz sensor audit. (Oct17.11).
*   **Chapter 31.809 (JNI Alignment Hardening)**: PASSED - Verified numeric telemetry pulse on A15 Dashboard. (Oct14.1).
