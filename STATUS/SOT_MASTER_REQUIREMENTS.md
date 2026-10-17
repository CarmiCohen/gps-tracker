# SOT Master Requirements & Hardening Status (Oct14.1)

## 🏗️ Architectural Master Rules (265 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.211 Alignment-Safe JNI Ingestion (R-ID 729)**: Shared memory ingestion in JNI (e.g., `ByteBuffer` mapped pointers) MUST use `std::memcpy` for multi-byte primitives to ensure stability on MediaTek/ARMv8 SoCs sensitive to alignment violations. (Oct14.1 - ISSUE-OCT17.4-F1).
*   **1.212 Release Integrity Verification (R-ID 730)**: Every deployment MUST undergo a manual version string verification to ensure build/engine/UI synchronization. (Oct14.1 - ISSUE-OCT15.6-T1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 756**: Version Name Verification - PASSED. Implemented formal version verification step in Chapter 1.3 of the Test Procedure. (Oct14.1 - ISSUE-OCT15.6-T1).
*   **SOT ID 734**: JNI Alignment Hardening - PASSED. Replaced direct pointer casting with `memcpy` for all 8-byte primitives in `jdhardware-jni.cpp`, resolving `NaNG` telemetry on Samsung A15. (Oct14.1 - ISSUE-OCT17.4-F1).
*   **SOT ID 733**: History Buffer Optimization - PASSED. Refactored forensic history flows to use `HistoryBuffer`. (Oct14.1 - SIMP-OCT15.1-S1).

...

---

## ✅ Verification Chapters
*   **Chapter 1.3 (Version Verification)**: PASSED - Verified "Oct14.1_PLATINUM" on Landing Screen of A15. (Oct14.1).
*   **Chapter 31.809 (JNI Alignment Hardening)**: PASSED - Verified numeric telemetry pulse on A15 Dashboard after JNI refactor. (Oct14.1).
