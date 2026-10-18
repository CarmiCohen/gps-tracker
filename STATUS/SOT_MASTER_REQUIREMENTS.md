# SOT Master Requirements & Hardening Status (Oct14.1)

## 🏗️ Architectural Master Rules (289 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.215 Explicit Component Lifecycle (R-ID 733)**: Core repositories and managers MUST NOT initiate background processing or side-effect heavy operations (e.g., coroutine loops) within their constructors. Lifecycle control MUST be delegated to the Application or Service layer via explicit `start()` methods to ensure predictable initialization order and environmental readiness. (Oct14.1 - SIMP-OCT18.2-S1).
*   **1.214 Test Isolation via Dependency Injection (R-ID 732)**: Test-only environmental states (e.g., "Test Mode") MUST be managed via injected providers (`TestModeProvider`) rather than static fields. (Oct14.1 - SIMP-OCT17.11-S1).
*   **1.211 Alignment-Safe JNI Ingestion (R-ID 729)**: Shared memory ingestion in JNI MUST use `std::memcpy`. (Oct14.1 - ISSUE-OCT17.4-F1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 763**: Explicit Repository Lifecycle - PASSED. Refactored `LogRepository` and `LogManager` to use explicit `start()` triggers. Integrated into `GpsApplication.onCreate`. (Oct14.1 - SIMP-OCT18.2-S1).
*   **SOT ID 762**: Test Mode DI Refactoring - PASSED. Migrated `LogRepository` to `TestModeProvider` injection. (Oct14.1 - SIMP-OCT17.11-S1).
*   **SOT ID 761**: JNI GNSS Alignment Hardening - PASSED. Re-aligned `GnssHealthBatch` to 8-byte boundaries. (Oct14.1 - ISSUE-OCT16.9-F2).

... [Remaining entries preserved] ...
