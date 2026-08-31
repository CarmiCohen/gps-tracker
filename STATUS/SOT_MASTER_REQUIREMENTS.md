# SOT Master Requirements (Aug.31.10)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (34 Rules)

### 1. Lifecycle & Resource Management
...
*   **1.9 IPC Optimization (R759/R876)**: **MANDATORY**. High-frequency lookups of system identifiers (e.g., Package Name, UID) must utilize `GpsApplication` shadow-caches. **Race Condition Hardening**: The `getPackageName()` override MUST query the cache directly rather than via a `lazy` delegate to ensure the shadow-cache is active immediately upon `onCreate()` population, preventing framework-level initialization race conditions on Samsung hardware. (Updated Aug.31.10).
...

### 2. UI & Performance Authority
*   **2.1 Staggered Hydration Manager (R318/R323/R739/R758/R776/R777/R874/R875)**: ... Map hydration must be further segmented (Levels 6 vs 7) and use fine-grained yielding (batch size ≤ 5) to ensure no single step exceeds the 700ms Davey threshold. (Updated Aug.31.09).
...

*(Total: 34 Architectural Rules + 196 Functional R-IDs = 230 Items)*
